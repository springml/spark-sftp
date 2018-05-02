/*
 * Copyright 2015 springml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.springml.spark.sftp

import java.io.File
import java.util.UUID

import com.springml.sftp.client.SFTPClient
import com.springml.spark.sftp.util.Utils.ImplicitDataFrameWriter

import org.apache.commons.io.FilenameUtils
import org.apache.hadoop.fs.Path
import org.apache.log4j.Logger
import org.apache.spark.sql.sources.{BaseRelation, CreatableRelationProvider, RelationProvider, SchemaRelationProvider}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

/**
 * Datasource to construct dataframe from a sftp url
 */
class DefaultSource extends RelationProvider with SchemaRelationProvider with CreatableRelationProvider  {
  @transient val logger = Logger.getLogger(classOf[DefaultSource])

  /**
   * Copy the file from SFTP to local location and then create dataframe using local file
   */
  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String]) = {
    createRelation(sqlContext, parameters, null)
  }

  /**
   * Copy the file from SFTP to local location and then create dataframe using local file
   */
  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String], schema: StructType) = {
    val username = parameters.get("username")
    val password = parameters.get("password")
    val pemFileLocation = parameters.get("pem")
    val pemPassphrase = parameters.get("pemPassphrase")
    val host = parameters.getOrElse("host", sys.error("SFTP Host has to be provided using 'host' option"))
    val port = parameters.get("port")
    val path = parameters.getOrElse("path", sys.error("'path' must be specified"))
    val fileType = parameters.getOrElse("fileType", sys.error("File type has to be provided using 'fileType' option"))
    val inferSchema = parameters.get("inferSchema")
    val header = parameters.getOrElse("header", "true")
    val delimiter = parameters.getOrElse("delimiter", ",")
    val createDF = parameters.getOrElse("createDF", "true")
    val copyLatest = parameters.getOrElse("copyLatest", "false")
    val tempFolder = parameters.getOrElse("tempLocation", System.getProperty("java.io.tmpdir"))
    val hdfsTemp = parameters.getOrElse("hdfsTempLocation", tempFolder)
    val cryptoKey = parameters.getOrElse("cryptoKey", null)
    val cryptoAlgorithm = parameters.getOrElse("cryptoAlgorithm", "AES")

    val supportedFileTypes = List("csv", "json", "avro", "parquet")
    if (!supportedFileTypes.contains(fileType)) {
      sys.error("fileType " + fileType + " not supported. Supported file types are " + supportedFileTypes)
    }

    val inferSchemaFlag = if (inferSchema != null && inferSchema.isDefined) {
      inferSchema.get
    } else {
      "false"
    }

    val sftpClient = getSFTPClient(username, password, pemFileLocation, pemPassphrase, host, port,
      cryptoKey, cryptoAlgorithm)
    val copiedFileLocation = copy(sftpClient, path, tempFolder, copyLatest.toBoolean)
    val fileLocation = copyToHdfs(sqlContext, copiedFileLocation, hdfsTemp)

    if (!createDF.toBoolean) {
      logger.info("Returning an empty dataframe after copying files...")
      createReturnRelation(sqlContext, schema)
    } else {
      DatasetRelation(fileLocation, fileType, inferSchemaFlag, header, delimiter, schema,
        sqlContext)
    }
  }

  override def createRelation(
      sqlContext: SQLContext,
      mode: SaveMode,
      parameters: Map[String, String],
      data: DataFrame): BaseRelation = {

    val username = parameters.get("username")
    val password = parameters.get("password")
    val pemFileLocation = parameters.get("pem")
    val pemPassphrase = parameters.get("pemPassphrase")
    val host = parameters.getOrElse("host", sys.error("SFTP Host has to be provided using 'host' option"))
    val port = parameters.get("port")
    val path = parameters.getOrElse("path", sys.error("'path' must be specified"))
    val fileType = parameters.getOrElse("fileType", sys.error("File type has to be provided using 'fileType' option"))
    val header = parameters.getOrElse("header", "true")
    val copyLatest = parameters.getOrElse("copyLatest", "false")
    val tmpFolder = parameters.getOrElse("tempLocation", System.getProperty("java.io.tmpdir"))
    val hdfsTemp = parameters.getOrElse("hdfsTempLocation", tmpFolder)
    val cryptoKey = parameters.getOrElse("cryptoKey", null)
    val cryptoAlgorithm = parameters.getOrElse("cryptoAlgorithm", "AES")
    val delimiter = parameters.getOrElse("delimiter", ",")
    val codec = parameters.getOrElse("codec", null)

    val supportedFileTypes = List("csv", "json", "avro", "parquet")
    if (!supportedFileTypes.contains(fileType)) {
      sys.error("fileType " + fileType + " not supported. Supported file types are " + supportedFileTypes)
    }

    val sftpClient = getSFTPClient(username, password, pemFileLocation, pemPassphrase, host, port,
      cryptoKey, cryptoAlgorithm)
    val tempFile = writeToTemp(sqlContext, data, hdfsTemp, tmpFolder, fileType, header, delimiter, codec)

    upload(tempFile, path, sftpClient)
    return createReturnRelation(data)
  }
  private def copyToHdfs(sqlContext: SQLContext, fileLocation : String,
                         hdfsTemp : String): String  = {
    val hadoopConf = sqlContext.sparkContext.hadoopConfiguration
    val hdfsPath = new Path(fileLocation)
    val fs = hdfsPath.getFileSystem(hadoopConf)
    if ("hdfs".equalsIgnoreCase(fs.getScheme)) {
      fs.copyFromLocalFile(new Path(fileLocation), new Path(hdfsTemp))
      val filePath = hdfsTemp + "/" + hdfsPath.getName
      fs.deleteOnExit(new Path(filePath))
      return filePath
    } else {
      return fileLocation
    }
  }

  private def copyFromHdfs(sqlContext: SQLContext, hdfsTemp : String,
                           fileLocation : String): String  = {
    val hadoopConf = sqlContext.sparkContext.hadoopConfiguration
    val hdfsPath = new Path(hdfsTemp)
    val fs = hdfsPath.getFileSystem(hadoopConf)
    if ("hdfs".equalsIgnoreCase(fs.getScheme)) {
      fs.copyToLocalFile(new Path(hdfsTemp), new Path(fileLocation))
      fs.deleteOnExit(new Path(hdfsTemp))
      return fileLocation
    } else {
      return hdfsTemp
    }
  }

  private def upload(source: String, target: String, sftpClient: SFTPClient) {
    logger.info("Copying " + source + " to " + target)
    sftpClient.copyToFTP(source, target)
  }

  private def getSFTPClient(
      username: Option[String],
      password: Option[String],
      pemFileLocation: Option[String],
      pemPassphrase: Option[String],
      host: String,
      port: Option[String],
      cryptoKey : String,
      cryptoAlgorithm : String) : SFTPClient = {

    val sftpPort = if (port != null && port.isDefined) {
      port.get.toInt
    } else {
      22
    }

    val cryptoEnabled = cryptoKey != null

    if (cryptoEnabled) {
      new SFTPClient(getValue(pemFileLocation), getValue(pemPassphrase), getValue(username),
        getValue(password),
          host, sftpPort, cryptoEnabled, cryptoKey, cryptoAlgorithm)
    } else {
      new SFTPClient(getValue(pemFileLocation), getValue(pemPassphrase), getValue(username),
        getValue(password), host, sftpPort)
    }
  }

  private def createReturnRelation(data: DataFrame): BaseRelation = {
    createReturnRelation(data.sqlContext, data.schema)
  }

  private def createReturnRelation(sqlContextVar: SQLContext, schemaVar: StructType): BaseRelation = {
    new BaseRelation {
      override def sqlContext: SQLContext = sqlContextVar
      override def schema: StructType = schemaVar
    }
  }

  private def copy(sftpClient: SFTPClient, source: String,
      tempFolder: String, latest: Boolean): String = {
    var copiedFilePath: String = null
    try {
      val target = tempFolder + File.separator + FilenameUtils.getName(source)
      copiedFilePath = target
      if (latest) {
        copiedFilePath = sftpClient.copyLatest(source, tempFolder)
      } else {
        logger.info("Copying " + source + " to " + target)
        copiedFilePath = sftpClient.copy(source, target)
      }

      copiedFilePath
    } finally {
      addShutdownHook(copiedFilePath)
    }
  }

  private def getValue(param: Option[String]): String = {
    if (param != null && param.isDefined) {
      param.get
    } else {
      null
    }
  }

  private def writeToTemp(sqlContext: SQLContext, df: DataFrame,
                          hdfsTemp: String, tempFolder: String, fileType: String, header: String,
                          delimiter: String, codec: String) : String = {
    val randomSuffix = "spark_sftp_connection_temp_" + UUID.randomUUID
    val hdfsTempLocation = hdfsTemp + File.separator + randomSuffix
    val localTempLocation = tempFolder + File.separator + randomSuffix

    addShutdownHook(localTempLocation)

    if (fileType.equals("json")) {
      df.coalesce(1).write.json(hdfsTempLocation)
    } else if (fileType.equals("parquet")) {
      df.coalesce(1).write.parquet(hdfsTempLocation)
      return copiedParquetFile(hdfsTempLocation)
    } else if (fileType.equals("csv")) {
      df.coalesce(1).
          write.
          option("header", header).
          option("delimiter", delimiter).
          optionNoNull("codec", Option(codec)).
          csv(hdfsTempLocation)
    } else if (fileType.equals("avro")) {
      df.coalesce(1).write.format("com.databricks.spark.avro").save(hdfsTempLocation)
    }

    copyFromHdfs(sqlContext, hdfsTempLocation, localTempLocation)
    copiedFile(localTempLocation)
  }

  private def addShutdownHook(tempLocation: String) {
    logger.debug("Adding hook for file " + tempLocation)
    val hook = new DeleteTempFileShutdownHook(tempLocation)
    Runtime.getRuntime.addShutdownHook(hook)
  }

  private def copiedParquetFile(tempFileLocation: String) : String = {
    val baseTemp = new File(tempFileLocation)
    val files = baseTemp.listFiles().filter { x =>
      (!x.isDirectory()
          && x.getName.endsWith("parquet")
          && !x.isHidden())}
    files(0).getAbsolutePath
  }

  private def copiedFile(tempFileLocation: String) : String = {
    val baseTemp = new File(tempFileLocation)
    val files = baseTemp.listFiles().filter { x =>
      (!x.isDirectory()
        && !x.getName.contains("SUCCESS")
        && !x.isHidden()
        && !x.getName.contains(".crc"))}
    files(0).getAbsolutePath
  }
}
