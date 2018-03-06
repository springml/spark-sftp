# Spark SFTP Connector Library

A library for constructing dataframes by downloading files from SFTP and writing dataframe to a SFTP server

## Requirements

This library requires Spark 2.x.

For Spark 1.x support, please check [spark1.x](https://github.com/springml/spark-sftp/tree/spark1.x) branch.

## Linking
You can link against this library in your program at the following ways:

### Maven Dependency
```
<dependency>
	<groupId>com.springml</groupId>
	<artifactId>spark-sftp_2.11</artifactId>
	<version>1.1.1</version>
</dependency>

```

### SBT Dependency
```
libraryDependencies += "com.springml" % "spark-sftp_2.11" % "1.1.1"
```


## Using with Spark shell
This package can be added to Spark using the `--packages` command line option.  For example, to include it when starting the spark shell:

```
$ bin/spark-shell --packages com.springml:spark-sftp_2.11:1.1.1
```

## Features
This package can be used to construct spark dataframe by downloading the files from SFTP server.

This package can also be used to write spark dataframe as a csv|json|acro tp SFTP server

This library requires following options:
* `path`: FTP URL of the file to be used for dataframe construction
* `username`: SFTP Server Username. 
* `password`: (Optional) SFTP Server Password. 
* `pem`: (Optional) Location of PEM file. Either pem or password has to be specified
* `pemPassphrase`: (Optional) Passphrase for PEM file.
* `host`: SFTP Host.
* `port`: (Optional) Port in which SFTP server is running. Default value 22.
* `fileType`: Type of the file. Supported types are csv, json, avro and parquet
* `inferSchema`: (Optional) InferSchema from the file content. Currently applicable only for csv fileType
* `header`: (Optional) Applicable only for csv fileType. Is the first row in CSV file is header. 
* `delimiter`: (Optional) Set the field delimiter. Applicable only for csv fileType. Default is comma.
* `codec`: (Optional) Applicable only for csv fileType. Compression codec to use when saving to file. Should be the fully qualified name of a class implementing org.apache.hadoop.io.compress.CompressionCodec or one of case-insensitive shorten names (bzip2, gzip, lz4, and snappy). Defaults to no compression when a codec is not specified.

### Scala API
```scala

// Construct Spark dataframe using file in FTP server
val df = spark.read.
            format("com.springml.spark.sftp").
            option("host", "SFTP_HOST").
            option("username", "SFTP_USER").
            option("password", "****").
            option("fileType", "csv").
            option("delimiter", ";").
            option("inferSchema", "true").
            load("/ftp/files/sample.csv")

// Write dataframe as CSV file to FTP server
df.write.
      format("com.springml.spark.sftp").
      option("host", "SFTP_HOST").
      option("username", "SFTP_USER").
      option("password", "****").
      option("fileType", "csv").
      option("delimiter", ";").
      option("codec", "bzip2").
      save("/ftp/files/sample.csv")

```


### Java API
```java
// Construct Spark dataframe using file in FTP server
DataFrame df = spark.read().
					format("com.springml.spark.sftp").
				    option("host", "SFTP_HOST").
				    option("username", "SFTP_USER").
				    option("password", "****").
				    option("fileType", "json").
				    load("/ftp/files/sample.json")

// Write dataframe as CSV file to FTP server
df.write().
      format("com.springml.spark.sftp").
      option("host", "SFTP_HOST").
      option("username", "SFTP_USER").
      option("password", "****").
      option("fileType", "json").
      save("/ftp/files/sample.json");
```

### R API
Spark 1.5+:
```r

if (nchar(Sys.getenv("SPARK_HOME")) < 1) {
  Sys.setenv(SPARK_HOME = "/home/spark")
}
library(SparkR, lib.loc = c(file.path(Sys.getenv("SPARK_HOME"), "R", "lib")))
sparkR.session(master = "local[*]", sparkConfig = list(spark.driver.memory = "2g"))

# Construct Spark dataframe using avro file in FTP server
df <- read.df(path="/ftp/files/sample.avro",
            source="com.springml.spark.sftp",
            host="SFTP_HOST",
            username="SFTP_USER",
            pem="/home/user/mypem.pem",
            fileType="avro")

# Write dataframe as avro file to FTP server
write.df(df,
        path="/ftp/files/sample.avro",
        source="com.springml.spark.sftp",
        host="SFTP_HOST",
        username="SFTP_USER",
        pem="/home/user/mypem.pem",
        fileType="avro")
```

### Note
1. SFTP files are fetched and written using [jsch](http://www.jcraft.com/jsch/). It will be executed as a single process
2. Files from SFTP server will be downloaded to temp location and it will be deleted only during spark shutdown


## Building From Source
This library is built with [SBT](http://www.scala-sbt.org/0.13/docs/Command-Line-Reference.html), which is automatically downloaded by the included shell script. To build a JAR file simply run `sbt/sbt package` from the project root.
