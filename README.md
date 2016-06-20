# Spark SFTP Connector Library

A library for constructing dataframes by downloading files from SFTP and writing dataframe to a SFTP server

## Requirements

This library requires Spark 1.5+

## Linking
You can link against this library in your program at the following ways:

### Maven Dependency
```
<dependency>
	<groupId>com.springml</groupId>
	<artifactId>spark-sftp_2.10</artifactId>
	<version>1.0.1</version>
</dependency>

```

### SBT Dependency
```
libraryDependencies += "com.springml" % "spark-sftp_2.10" % "1.0.1"
```


## Using with Spark shell
This package can be added to Spark using the `--packages` command line option.  For example, to include it when starting the spark shell:

```
$ bin/spark-shell --packages com.springml:spark-sftp_2.10:1.0.1
```

## Features
This package can be used to construct spark dataframe by downloading the files from SFTP server.

This package can also be used to write spark dataframe as a csv|json|acro tp SFTP server

This library requires following options:
* `path`: FTP URL of the file to be used for dataframe construction
* `username`: SFTP Server Username. 
* `password`: (Optional) SFTP Server Password. 
* `pem`: (Optional) Location of PEM file. Either pem or password has to be specified
* `host`: SFTP Host.
* `port`: (Optional) Port in which SFTP server is running. Default value 22.
* `fileType`: Type of the file. Supported types are csv, json, avro and parquet
* `inferSchema`: (Optional) InferSchema from the file content. Currently applicable only for csv fileType
* `header`: (Optional) Applicable only for csv fileType. Is the first row in CSV file is header. 


### Scala API
Spark 1.5+:
```scala
import org.apache.spark.sql.SQLContext

// Construct Spark dataframe using file in FTP server
val sqlContext = new SQLContext(sc)
val df = sqlContext.read.
				    format("com.springml.spark.sftp").
				    option("host", "SFTP_HOST").
				    option("username", "SFTP_USER").
				    option("password", "****").
				    option("fileType", "csv").
				    option("inferSchema", "true").
				    load("/ftp/files/sample.csv")

// Write dataframe as CSV file to FTP server
df.write().
      format("com.springml.spark.sftp").
      option("host", "SFTP_HOST").
      option("username", "SFTP_USER").
      option("password", "****").
      option("fileType", "csv").
      save("/ftp/files/sample.csv")

```


### Java API
Spark 1.5+:
```java
import org.apache.spark.sql.SQLContext

SQLContext sqlContext = new SQLContext(sc);

// Construct Spark dataframe using file in FTP server
DataFrame df = sqlContext.read().
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
library(SparkR)

Sys.setenv('SPARKR_SUBMIT_ARGS'='"--packages" "com.springml:spark-sftp_2.10:1.0.1" "sparkr-shell"')
sqlContext <- sparkRSQL.init(sc)

# Construct Spark dataframe using file in FTP server
df <- read.df(sqlContext,
		path="/ftp/files/sample.avro",
		source="com.springml.spark.sftp",
		host="SFTP_HOST",
		username="SFTP_USER",
		pem="/home/user/mypem.pem",
		fileType="avro")

# Write dataframe as CSV file to FTP server
write.df(df,
        path="/ftp/files/sample.avro",
        source="com.springml.spark.sftp",
        host="SFTP_HOST",
        username="SFTP_USER",
        pem="/home/user/mypem.pem",
        fileType="avro")
```

### Python API
Spark 1.5+:
```python
from pyspark.sql import SQLContext
sqlContext = SQLContext(sc)

df = sqlContext.read.
					format('com.springml.spark.sftp').
					options(password='*****').
					options(host='SFTP_HOST').
					options(username='SFTP_USER').
					options(fileType='parquet').
					options(inferSchema='true').
					load("/home/user/files/sample.parquet")

```

### Note
1. SFTP files are fetched and written using [jsch](http://www.jcraft.com/jsch/). It is not executed as spark job. It might have issues in cluster
2. Files from SFTP server will be downloaded to temp location and it will be deleted only during spark shutdown


## Building From Source
This library is built with [SBT](http://www.scala-sbt.org/0.13/docs/Command-Line-Reference.html), which is automatically downloaded by the included shell script. To build a JAR file simply run `sbt/sbt package` from the project root. The build configuration includes support for both Scala 2.10 and 2.11.