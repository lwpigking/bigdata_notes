# Hudi IDEA编程开发

Apache Hudi最初是由Uber开发的，旨在以高效率实现低延迟的数据库访问。Hudi 提供了Hudi 表的概念，这些表支持CRUD操作。接下来，`基于Spark框架使用Hudi API 进行读写操作`。

```scala
package com.lwPigKing.hudi.spark

import org.apache.hudi.QuickstartUtils._
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, Dataset, Row, SaveMode, SparkSession}

import java.util


/**
 * Project:  BigDataProject
 * Create date:  2023/8/7
 * Created by lwPigKing
 */
object HudiSparkDemo {
  def main(args: Array[String]): Unit = {

    val sparkSession: SparkSession = SparkSession
      .builder()
      .appName(this.getClass.getSimpleName.stripSuffix("$"))
      .master("local[*]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()


    val tableName: String = "tbl_trips_cow"
    val tablePath: String = "/hudi-warehouse/tbl_trips_cow"

    // build data generators that simulate inserting and updating data
    import org.apache.hudi.QuickstartUtils._


    // Task1:Simulate data,insert hudi table and use COW mode
    insertData(sparkSession, tableName, tablePath)

    // Task2:Snapshot Query data in DSL mode
    queryData(sparkSession, tablePath)
    queryDataTime(sparkSession, tablePath)

//    // Task3:Update the data
    val generator: DataGenerator = new DataGenerator()
    insertData(sparkSession, tableName, tablePath, generator)
    updateData(sparkSession, tableName, tablePath, generator)
//
//    // Task4:Incremental Query data in SQL
    incrementalQueryData(sparkSession, tablePath)
//
//    // Task5:Delete the data
    deleteData(sparkSession, tableName, tablePath)


    sparkSession.close()


  }
}


def insertData(sparkSession: SparkSession, table: String, path: String): Unit = {
  import sparkSession.implicits._
  import org.apache.hudi.QuickstartUtils._

  val generator: DataGenerator = new DataGenerator
  val inserts: util.List[String] = convertToStringList(generator.generateInserts(100))

  import scala.collection.JavaConverters._
  val insertDF: DataFrame = sparkSession
    .read
    .json(sparkSession.sparkContext.parallelize(inserts.asScala, 2).toDS())

  import org.apache.hudi.DataSourceWriteOptions._
  import org.apache.hudi.config.HoodieWriteConfig._
  insertDF.write
    .mode(SaveMode.Append)
    .format("hudi")
    .option("hoodie.insert.shuffle.parallelism", "2")
    .option("hoodie.upsert.shuffle.parallelism", "2")
    .option(PRECOMBINE_FIELD.key(), "ts")
    .option(RECORDKEY_FIELD.key(), "uuid")
    .option(PARTITIONPATH_FIELD.key(), "partitionpath")
    .option(TBL_NAME.key(), table)
    .save(path)

}


def queryData(sparkSession: SparkSession, path: String): Unit = {
  import sparkSession.implicits._
  val tripsDF: DataFrame = sparkSession.read.format("hudi").load(path)
  tripsDF.filter(col("fare") >= 20 && col("fare") <= 50)
    .select($"driver", $"rider", $"fare", $"begin_lat", $"begin_log", $"partitionpath", $"_hoodie_commit_time")
    .orderBy($"fare".desc, $"_hoodie_commit_time".desc)
    .show(20, truncate = false)
}


def queryDataTime(sparkSession: SparkSession, path: String): Unit = {
  import org.apache.spark.sql.functions._

  // method 1:specify a string in the format yyyyMMddHHmmss
  val df1: Dataset[Row] = sparkSession.read
    .format("hudi")
    .option("as.of.instant", "20211119095057")
    .load(path)
    .sort(col("_hoodie_commit_time").desc)
  df1.show(numRows = 5, truncate = false)

  // method 2:specify a string in the format yyyy-MM-dd HH:mm:ss
  val df2: Dataset[Row] = sparkSession.read
    .format("hudi")
    .option("as.of.instant", "20211119095057")
    .load(path)
    .sort(col("_hoodie_commit_time").desc)
  df2.show(numRows = 5, truncate = false)
}

def insertData(sparkSession: SparkSession, table: String, path: String, dataGen: DataGenerator): Unit = {
  import sparkSession.implicits._

  // TODO: a. 模拟乘车数据
  import org.apache.hudi.QuickstartUtils._
  val inserts = convertToStringList(dataGen.generateInserts(100))

  import scala.collection.JavaConverters._
  val insertDF: DataFrame = sparkSession.read
    .json(sparkSession.sparkContext.parallelize(inserts.asScala, 2).toDS())
  //insertDF.printSchema()
  //insertDF.show(10, truncate = false)

  // TODO: b. 插入数据至Hudi表
  import org.apache.hudi.DataSourceWriteOptions._
  import org.apache.hudi.config.HoodieWriteConfig._
  insertDF.write
    .mode(SaveMode.Overwrite)
    .format("hudi") // 指定数据源为Hudi
    .option("hoodie.insert.shuffle.parallelism", "2")
    .option("hoodie.upsert.shuffle.parallelism", "2")
    // Hudi 表的属性设置
    .option(PRECOMBINE_FIELD.key(), "ts")
    .option(RECORDKEY_FIELD.key(), "uuid")
    .option(PARTITIONPATH_FIELD.key(), "partitionpath")
    .option(TBL_NAME.key(), table)
    .save(path)
}

def updateData(sparkSession: SparkSession, table: String, path: String, dataGen: DataGenerator): Unit = {
  import sparkSession.implicits._
  import org.apache.hudi.QuickstartUtils._
  import scala.collection.JavaConverters._
  val updates: util.List[String] = convertToStringList(dataGen.generateUpdates(100))
  val updateDF: DataFrame = sparkSession.read
    .json(sparkSession.sparkContext.parallelize(updates.asScala, 2).toDS())

  import org.apache.hudi.DataSourceWriteOptions._
  import org.apache.hudi.config.HoodieWriteConfig._
  updateDF.write
    .mode(SaveMode.Append)
    .format("hudi")
    .option("hoodie.insert.shuffle.parallelism", "2")
    .option("hoodie.upsert.shuffle.parallelism", "2")
    .option(PRECOMBINE_FIELD.key(), "ts")
    .option(RECORDKEY_FIELD.key(), "uuid")
    .option(PARTITIONPATH_FIELD.key(), "partitionpath")
    .option(TBL_NAME.key(), table)
    .save(path)
}


def incrementalQueryData(sparkSession: SparkSession, path: String): Unit = {
  import sparkSession.implicits._
  import org.apache.hudi.DataSourceReadOptions._
  sparkSession.read
    .format("hudi")
    .load(path)
    .createOrReplaceTempView("view_temp_hudi_trips")

  val commits: Array[String] = sparkSession.sql(
    s"""
       |select
       |  distinct(_hoodie_commit_time) as commitTime
       |from
       |  view_temp_hudi_trips
       |order by
       |  commitTime DESC
       |""".stripMargin)
    .map(row => {
      row.getString(0)
    }).take(50)

  val beginTime: String = commits(commits.length - 1)
  println(s"beginTime = ${beginTime}")

  val tripsIncrementalDF: DataFrame = sparkSession.read
    .format("hudi")
    .option(QUERY_TYPE.key(), QUERY_TYPE_INCREMENTAL_OPT_VAL)
    .option(BEGIN_INSTANTTIME.key(), beginTime)
    .load(path)

  tripsIncrementalDF.createOrReplaceTempView("hudi_trips_incremental")
  sparkSession.sql(
    s"""
       |select
       |  _hoodie_commit_time, fare, begin_lon, begin_lat, ts
       |from
       |  hudi_trips_incremental
       |where
       |  fare > 20.0
       |""".stripMargin)
    .show(10, truncate = false)
}


def deleteData(sparkSession: SparkSession, table: String, path: String): Unit = {
  import sparkSession.implicits._
  val tripsDF: DataFrame = sparkSession.read.format("hudi").load(path)
  println(s"Count = ${tripsDF.count()}")

  val value: Dataset[Row] = tripsDF.select($"uuid", $"partitionpath").limit(2)
  import org.apache.hudi.QuickstartUtils._

  val generator: DataGenerator = new DataGenerator()
  val deletes: util.List[String] = generator.generateDeletes(value.collectAsList())

  import scala.collection.JavaConverters._
  val deleteDF: DataFrame = sparkSession.read.json(sparkSession.sparkContext.parallelize(deletes.asScala, 2))

  import org.apache.hudi.DataSourceWriteOptions._
  import org.apache.hudi.config.HoodieWriteConfig._
  deleteDF.write
    .mode(SaveMode.Append)
    .format("hudi")
    .option("hoodie.insert.shuffle.parallelism", "2")
    .option("hoodie.upsert.shuffle.parallelism", "2")
    .option(OPERATION.key(), "delete")
    .option(PRECOMBINE_FIELD.key(), "ts")
    .option(RECORDKEY_FIELD.key(), "uuid")
    .option(PARTITIONPATH_FIELD.key(), "partitionpath")
    .option(TBL_NAME.key(), table)
    .save(path)

  val hudiDF: DataFrame = sparkSession.read.format("hudi").load(path)
  println(s"Delete after count = ${hudiDF.count()}")
}

```



