package com.springml.spark.sftp.util

import org.apache.spark.sql.DataFrameWriter


object Utils {


  /**
    * [[DataFrameWriter]] implicits
    */
  implicit class ImplicitDataFrameWriter[T](dataFrameWriter: DataFrameWriter[T]) {

    /**
      * Adds an output option for the underlying data source if the option has a value.
      */
    def optionNoNull(key: String, optionValue: Option[String]): DataFrameWriter[T] = {
      optionValue match {
        case Some(_) => dataFrameWriter.option(key, optionValue.get)
        case None => dataFrameWriter
      }
    }
  }

}