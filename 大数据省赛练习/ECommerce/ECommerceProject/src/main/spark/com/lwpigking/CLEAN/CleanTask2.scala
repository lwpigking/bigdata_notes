package com.lwpigking.CLEAN

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.functions.{current_timestamp, date_format, lit}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/28
 * Created by lwPigKing
 */

/**
 * 将ods库中part表数据抽取到dwd库中dim_part的分区表，分区字段为etldate且值与
 * ods库的相对应表该值相等，并添加dwd_insert_user、dwd_insert_time、dwd_modify_user、
 * dwd_modify_time四列,其中dwd_insert_user、dwd_modify_user均填写“user1”，
 * dwd_insert_time、dwd_modify_time均填写操作时间，并进行数据类型转换。在hive cli中按照
 * partkey顺序排序，查询dim_part前1条数据，将结果内容复制粘贴至对应报告中；
 */

object CleanTask2 {
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

    // 选取ods库中数据
    val dataFramePart: DataFrame = sparkSession.sql(
      s"""
         |select *
         |from
         |  shtd_store_ods.part
         |""".stripMargin)
      // 增加dwd_insert_user列
      .withColumn("dwd_insert_user", lit("user1"))
      // 增加dwd_modify_user列
      .withColumn("dwd_modify_user", lit("user1"))
      // 增加dwd_insert_time列
      .withColumn("dwd_insert_time", lit(date_format(current_timestamp(), "yyyy-MM-dd HH:mm:ss")).cast("timestamp"))
      // 增加dwd_modify_time列
      .withColumn("dwd_modify_time", lit(date_format(current_timestamp(), "yyyy-MM-dd HH:mm:ss")).cast("timestamp"))

    dataFramePart.write.mode(SaveMode.Overwrite)
      // 分区
      .partitionBy("etl_date")
      .saveAsTable("shtd_store_dwd.dim_part")

    // 查询结果
    sparkSession.sql(
      s"""
         |select *
         |from
         |  shtd_store_dwd.dim_part
         |order by
         |  partkey limit 1
         |""".stripMargin).show()
  }
}
