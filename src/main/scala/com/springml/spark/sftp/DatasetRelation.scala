package com.springml.spark.sftp

import org.apache.spark.sql.sources.BaseRelation
import org.apache.spark.sql.sources.TableScan
import org.apache.log4j.Logger
import org.apache.spark.sql.DataFrame
import com.databricks.spark.avro._
import com.databricks.spark.csv._
import org.apache.spark.sql.types.StructType
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * Abstract relation class for reading data from file
 */
case class DatasetRelation(
    fileLocation: String,
    fileType: String,
    inferSchema: String,
    header: String,
    delimiter: String,
    sqlContext: SQLContext) extends BaseRelation with TableScan {

    private val logger = Logger.getLogger(classOf[DatasetRelation])

    val df = read()

    private def read(): DataFrame = {
      var df: DataFrame = null
      if (fileType.equals("json")) {
        df = sqlContext.read.json(fileLocation)
      } else if (fileType.equals("parquet")) {
        df = sqlContext.read.parquet(fileLocation)
      } else if (fileType.equals("csv")) {
        df = sqlContext.
          read.
          format("com.databricks.spark.csv").
          option("header", header).
          option("delimiter", delimiter).
          option("inferSchema", inferSchema).
          load(fileLocation)
      } else if (fileType.equals("avro")) {
        df = sqlContext.read.avro(fileLocation)
      }

      df
    }

    override def schema: StructType = {
      df.schema
    }

    override def buildScan(): RDD[Row] = {
      df.rdd
    }

}