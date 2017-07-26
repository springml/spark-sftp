package com.springml.spark.sftp

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types._
import org.scalatest.{BeforeAndAfterEach, FunSuite}

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

  val csvTypesMap = Map("ProposalId" -> IntegerType,
    "OpportunityId" -> StringType,
    "Clicks" -> LongType,
    "Impressions" -> LongType
  )

  val jsonTypesMap = Map("name" -> StringType,
    "age" -> IntegerType
  )

  private def validateTypes(field : StructField, typeMap : Map[String, DataType]) = {
    val expectedType = typeMap(field.name)
    assert(expectedType == field.dataType)
  }

  private def columnArray(typeMap : Map[String, DataType]) : Array[StructField] = {
    val columns = typeMap.map(x => new StructField(x._1, x._2, true))

    val columnStruct = Array[StructField] ()
    columns.copyToArray(columnStruct)

    columnStruct
  }

  test ("Read CSV") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/sample.csv").getPath
    val dsr = DatasetRelation(fileLocation, "csv", "false", "true", ",", null, sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read JSON") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/people.json").getPath
    val dsr = DatasetRelation(fileLocation, "json", "false", "true", ",", null, sqlContext)
    val rdd = dsr.buildScan()
    assert(3 == rdd.count())
  }

  test ("Read AVRO") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/users.avro").getPath
    val dsr = DatasetRelation(fileLocation, "avro", "false", "true", ",", null, sqlContext)
    val rdd = dsr.buildScan()
    assert(2 == rdd.count())
  }

  test ("Read parquet") {
    val sqlContext = new SQLContext(sc)
    val fileLocation = getClass.getResource("/users.parquet").getPath
    val dsr = DatasetRelation(fileLocation, "parquet", "false", "true", ",", null, sqlContext)
    val rdd = dsr.buildScan()
    assert(2 == rdd.count())
  }

  test ("Read CSV with custom schema") {
    val sqlContext = new SQLContext(sc)
    val columnStruct = columnArray(csvTypesMap)
    val expectedSchema = StructType(columnStruct)

    val fileLocation = getClass.getResource("/sample.csv").getPath
    val dsr = DatasetRelation(fileLocation, "csv", "false", "true", ",", expectedSchema, sqlContext)
    val rdd = dsr.buildScan()

    assert(dsr.schema.fields.length == columnStruct.length)
    dsr.schema.fields.foreach(s => validateTypes(s, csvTypesMap))
  }

  test ("Read Json with custom schema") {
    val sqlContext = new SQLContext(sc)
    val columnStruct = columnArray(jsonTypesMap)
    val expectedSchema = StructType(columnStruct)

    val fileLocation = getClass.getResource("/people.json").getPath
    val dsr = DatasetRelation(fileLocation, "json", "false", "true", ",", expectedSchema, sqlContext)
    val rdd = dsr.buildScan()

    assert(dsr.schema.fields.length == columnStruct.length)
    dsr.schema.fields.foreach(s => validateTypes(s, jsonTypesMap))
  }
}