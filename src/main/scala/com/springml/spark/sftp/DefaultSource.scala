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
import java.io.File

/**
 * Datasource to construct dataframe from a sftp url
 */
class DefaultSource extends RelationProvider with SchemaRelationProvider with CreatableRelationProvider  {
  @transient val logger = Logger.getLogger(classOf[DefaultSource])

  def createRelation(sqlContext: SQLContext, mode: SaveMode, parameters: Map[String, String], data: DataFrame): BaseRelation = {
    sys.error("Write not yet supported");
    new BaseRelation {
      override def sqlContext: SQLContext = data.sqlContext
      override def schema: StructType = data.schema
    }
  }

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

    val supportedFileTypes = List("csv", "json", "avro", "parquet")
    if (!supportedFileTypes.contains(fileType)) {
      sys.error("fileType " + fileType + " not supported. Supported file types are " + supportedFileTypes)
    }

    val inferSchemaFlag = if (inferSchema != null && inferSchema.isDefined) {
      inferSchema.get
    } else {
      "false"
    }

    val fileLocation = copy(username, password, pemFileLocation, host, port, path)

    DatasetRelation(fileLocation, fileType, inferSchemaFlag, sqlContext)
  }

  private def copy(username: Option[String], password: Option[String], pemFileLocation: Option[String],
      host: String, port: Option[String], source: String): String = {
    val sftpPort = if (port != null && port.isDefined) {
      port.get.toInt
    } else {
      22
    }

    val sftpClient = new SFTPClient(getValue(pemFileLocation), getValue(username), getValue(password), host, sftpPort)
    val tempDir = System.getProperty("java.io.tmpdir")
    val target = tempDir + File.separator + FilenameUtils.getName(source)
    logger.info("Copying " + source + " to " + target)
    sftpClient.copy(source, target)

    target
  }

  private def getValue(param: Option[String]): String = {
    if (param != null && param.isDefined) {
      param.get
    } else {
      null
    }
  }

}
