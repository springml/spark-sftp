package com.springml.spark.sftp

import org.scalatest.{ FunSuite, BeforeAndAfterEach}
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import scala.io.Source

/**
 * Simple unit test for basic testing on different formats of file
 */
class TestDatasetRelation extends FunSuite with BeforeAndAfterEach {
  var sparkConf: SparkConf = _
  var sc: SparkContext = _

  override def beforeEach() {
    sparkConf = new SparkConf().setMaster("local").setAppName("Test Dataset Relation")
    sc = new SparkContext(sparkConf)
  }

  override def afterEach() {
    sc.stop()
  }

  test ("Read CSV") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/sample.csv").getPath
    val dsr = DatasetRelation(fileLocation, "csv", "false", "true", sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read JSON") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/people.json").getPath
    val dsr = DatasetRelation(fileLocation, "json", "false", "true", sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read AVRO") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/users.avro").getPath
    val dsr = DatasetRelation(fileLocation, "avro", "false", "true", sqlContext)
    val rdd = dsr.buildScan()
    assert(2 == rdd.count())
  }

  test ("Read parquet") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/users.parquet").getPath
    val dsr = DatasetRelation(fileLocation, "parquet", "false", "true", sqlContext)
    val rdd = dsr.buildScan()
    assert(2 == rdd.count())
  }
}