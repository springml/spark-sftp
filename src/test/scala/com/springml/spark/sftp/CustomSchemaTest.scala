package com.springml.spark.sftp

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, LongType, StringType, StructField, _}
import org.scalatest.{BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

/**
  * Tests for creating dataframe using custom schema
  */
class CustomSchemaTest extends AnyFunSuite with BeforeAndAfterEach {
  var ss: SparkSession = _

  val csvTypesMap = Map("ProposalId" -> IntegerType,
    "OpportunityId" -> StringType,
    "Clicks" -> LongType,
    "Impressions" -> LongType
  )

  val jsonTypesMap = Map("name" -> StringType,
    "age" -> IntegerType
  )

  override def beforeEach() {
    ss = SparkSession.builder().master("local").appName("Custom Schema Test").getOrCreate()
  }

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

  test ("Read CSV with custom schema") {
    val columnStruct = columnArray(csvTypesMap)
    val expectedSchema = StructType(columnStruct)

    val fileLocation = getClass.getResource("/sample.csv").getPath
    val dsr = DatasetRelation(fileLocation, "csv", "false", "true", ",", "\"", "\\", "false", null, expectedSchema, ss.sqlContext)
    val rdd = dsr.buildScan()

    assert(dsr.schema.fields.length == columnStruct.length)
    dsr.schema.fields.foreach(s => validateTypes(s, csvTypesMap))
  }

  test ("Read Json with custom schema") {
    val columnStruct = columnArray(jsonTypesMap)
    val expectedSchema = StructType(columnStruct)

    val fileLocation = getClass.getResource("/people.json").getPath
    val dsr = DatasetRelation(fileLocation, "json", "false", "true", ",", "\"", "\\", "false", null, expectedSchema, ss.sqlContext)
    val rdd = dsr.buildScan()

    assert(dsr.schema.fields.length == columnStruct.length)
    dsr.schema.fields.foreach(s => validateTypes(s, jsonTypesMap))
  }

}
