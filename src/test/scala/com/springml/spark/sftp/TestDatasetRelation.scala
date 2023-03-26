package com.springml.spark.sftp

import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite

/**
 * Simple unit test for basic testing on different formats of file
 */
class TestDatasetRelation extends AnyFunSuite with BeforeAndAfterEach {
  var ss: SparkSession = _

  override def beforeEach() {
    ss = SparkSession.builder().master("local").enableHiveSupport().appName("Test Dataset Relation").getOrCreate()
  }

  test ("Read CSV") {
    val fileLocation = getClass.getResource("/sample.csv").getPath
    val dsr = DatasetRelation(fileLocation, "csv", "false", "true", ",", "\"", "\\", "false", null, null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read CSV using custom delimiter") {
    val fileLocation = getClass.getResource("/sample.csv").getPath
    val dsr = DatasetRelation(fileLocation, "csv", "false", "true", ";", "\"", "\\", "false", null, null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read multiline CSV using custom quote and escape") {
    val fileLocation = getClass.getResource("/sample_quoted_multiline.csv").getPath
    val dsr = DatasetRelation(fileLocation, "csv", "false", "true", ",", "\"", "\\", "true", null, null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }


  test ("Read JSON") {
    val fileLocation = getClass.getResource("/people.json").getPath
    val dsr = DatasetRelation(fileLocation, "json", "false", "true", ",", "\"", "\\", "false", null, null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read AVRO") {
    val fileLocation = getClass.getResource("/users.avro").getPath
    val dsr = DatasetRelation(fileLocation, "avro", "false", "true", ",", "\"", "\\", "false", null, null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(2 == rdd.count())
  }

  test ("Read parquet") {
    val fileLocation = getClass.getResource("/users.parquet").getPath
    val dsr = DatasetRelation(fileLocation, "parquet", "false", "true", ",", "\"", "\\", "false", null, null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(2 == rdd.count())
  }

  test ("Read text file") {
    val fileLocation = getClass.getResource("/plaintext.txt").getPath
    val dsr = DatasetRelation(fileLocation, "txt", "false", "true", ",", "\"", "\\", "false", null, null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read xml file") {
    val fileLocation = getClass.getResource("/books.xml").getPath
    val dsr = DatasetRelation(fileLocation, "xml", "false", "true", ",", "\"", "\\", "false", "book", null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(12 == rdd.count())
  }
  test ("Read orc file") {
    val fileLocation = getClass.getResource("/books.orc").getPath
    val dsr = DatasetRelation(fileLocation, "orc", "false", "true", ",", "\"", "\\", "false", "book", null, ss.sqlContext)
    val rdd = dsr.buildScan()
    assert(12 == rdd.count())
  }
}
