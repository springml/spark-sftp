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

import org.apache.spark.sql.sources.RelationProvider
import org.apache.spark.sql.sources.SchemaRelationProvider
import org.apache.spark.sql.sources.CreatableRelationProvider
import org.apache.spark.sql.SQLContext
import org.apache.log4j.Logger
import org.apache.spark.sql.types.StructType
import com.springml.sftp.client.SFTPClient
import org.apache.commons.io.FilenameUtils
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.sources.BaseRelation
import com.databricks.spark.avro._
import java.io.File
import org.apache.commons.io.FileUtils

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

    val supportedFileTypes = List("csv", "json", "avro", "parquet")
    if (!supportedFileTypes.contains(fileType)) {
      sys.error("fileType " + fileType + " not supported. Supported file types are " + supportedFileTypes)
    }

    val inferSchemaFlag = if (inferSchema != null && inferSchema.isDefined) {
      inferSchema.get
    } else {
      "false"
    }

    val sftpClient = getSFTPClient(username, password, pemFileLocation, host, port)
    val fileLocation = copy(sftpClient, path, tempFolder, copyLatest.toBoolean)

    if (!createDF.toBoolean) {
      logger.info("Returning an empty dataframe after copying files...")
      createReturnRelation(sqlContext, schema)
    } else {
      DatasetRelation(fileLocation, fileType, inferSchemaFlag, header, delimiter, sqlContext)
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
    val host = parameters.getOrElse("host", sys.error("SFTP Host has to be provided using 'host' option"))
    val port = parameters.get("port")
    val path = parameters.getOrElse("path", sys.error("'path' must be specified"))
    val fileType = parameters.getOrElse("fileType", sys.error("File type has to be provided using 'fileType' option"))
    val header = parameters.getOrElse("header", "true")
    val copyLatest = parameters.getOrElse("copyLatest", "false")
    val tmpFolder = parameters.getOrElse("tempLocation", System.getProperty("java.io.tmpdir"))

    val supportedFileTypes = List("csv", "json", "avro", "parquet")
    if (!supportedFileTypes.contains(fileType)) {
      sys.error("fileType " + fileType + " not supported. Supported file types are " + supportedFileTypes)
    }

    val sftpClient = getSFTPClient(username, password, pemFileLocation, host, port)
    val tempFile = writeToTemp(sqlContext, data, tmpFolder, fileType, header)
    upload(tempFile, path, sftpClient)
    return createReturnRelation(data)
  }

  private def upload(source: String, target: String, sftpClient: SFTPClient) {
    logger.info("Copying " + source + " to " + target)
    sftpClient.copyToFTP(source, target)
  }

  private def getSFTPClient(
      username: Option[String],
      password: Option[String],
      pemFileLocation: Option[String],
      host: String,
      port: Option[String]) : SFTPClient = {

    val sftpPort = if (port != null && port.isDefined) {
      port.get.toInt
    } else {
      22
    }

    new SFTPClient(getValue(pemFileLocation), getValue(username), getValue(password), host, sftpPort)
  }

  private def createReturnRelation(data: DataFrame): BaseRelation = {
    createReturnRelation(data.sqlContext, data.schema)
  }

  private def createReturnRelation(sqlContext: SQLContext, schema: StructType): BaseRelation = {
    new BaseRelation {
      override def sqlContext: SQLContext = sqlContext
      override def schema: StructType = schema
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
      addShutdownHook(copiedFilePath);
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
      tempFolder: String, fileType: String, header: String) : String = {
    val r = scala.util.Random
    val tempLocation = tempFolder + File.separator + "spark_sftp_connection_temp" + r.nextInt(1000)
    addShutdownHook(tempLocation);

    if (fileType.equals("json")) {
      df.coalesce(1).write.json(tempLocation)
    } else if (fileType.equals("parquet")) {
      df.coalesce(1).write.parquet(tempLocation)
      return copiedParquetFile(tempLocation)
    } else if (fileType.equals("csv")) {
      df.coalesce(1).
          write.
          format("com.databricks.spark.csv").
          option("header", header).
          save(tempLocation)
    } else if (fileType.equals("avro")) {
      df.coalesce(1).write.avro(tempLocation)
    }

    copiedFile(tempLocation)
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
        && !x.isHidden())}
    files(0).getAbsolutePath
  }
}
