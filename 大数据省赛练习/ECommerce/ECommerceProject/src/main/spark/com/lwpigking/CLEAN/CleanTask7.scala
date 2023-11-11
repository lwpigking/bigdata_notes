package com.lwpigking.CLEAN

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.functions.{current_timestamp, date_format, lit}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/29
 * Created by lwPigKing
 */

/**
 * 将ods库中lineitem表数据抽取到dwd库中fact_lineitem的分区表，分区字段为etldate
 * 且值与ods库的相对应表该值相等，抽取的条件为根据orderkey和partkey进行去重，并添加
 * dwd_insert_user、dwd_insert_time、dwd_modify_user、dwd_modify_time四列，其中
 * dwd_insert_user、dwd_modify_user均填写“user1”，dwd_insert_time、dwd_modify_time均
 * 填写操作时间，并进行数据类型转换。在hive cli执行show partitions dwd.fact_lineitem命令，
 * 将结果截图粘贴至对应报告中。
 */

object CleanTask7 {
  def main(args: Array[String]): Unit = {
    val warehouse: String = "hdfs://master:9000/hive/warehouse"
    val metastore: String = "thrift://master:9083"
    val mysql: String = "jdbc:mysql://master:3306/shtd_store?useUnicode=true&characterEncoding=utf-8"
    val properties: Properties = new Properties()
    properties.put("user", "root")
    properties.put("password", "123456")
    properties.put("driver", "com.mysql.jdbc.Driver")
    val sparkSession: SparkSession = SparkSession
      .builder()
      .config("spark.sql.warehouse.dir", warehouse)
      .config("hive.metastore.uris", metastore)
      .config("spark.sql.shuffle.partitions", 1000)
      .config("hive.exec.dynamic.partition", true)
      .config("hive.exec.dynamic.partition.mode", "nonstrict")
      .config("hive.exec.max.dynamic.partitions", 10000)
      .config("spark.sql.writeLegacyFormat", true)
      .appName("spark")
      .enableHiveSupport()
      .getOrCreate()

    val dataFrameLineitem: DataFrame = sparkSession.sql(
      s"""
         |select *
         |from
         |  shtd_store_ods.lineitem
         |""".stripMargin)
      .dropDuplicates("orderkey", "partkey")
      .withColumn("dwd_insert_user", lit("user1"))
      .withColumn("dwd_modify_user", lit("user1"))
      .withColumn("dwd_insert_time", lit(date_format(current_timestamp(), "yyyy-MM-dd HH:mm:ss")).cast("timestamp"))
      .withColumn("dwd_modify_time", lit(date_format(current_timestamp(), "yyyy-MM-dd HH:mm:ss")).cast("timestamp"))

    dataFrameLineitem.write.mode(SaveMode.Overwrite)
      .partitionBy("etl_date")
      .saveAsTable("shtd_store_dwd.fact_lineitem")

    sparkSession.sql(
      s"""
         |show partitions shtd_store_dwd.fact_lineitem
         |""".stripMargin).show()
  }
}
