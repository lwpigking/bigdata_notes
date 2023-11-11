package com.lwpigking.CLEAN

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.functions.col

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/29
 * Created by lwPigKing
 */

/**
 * 删除ods.orders中的分区，仅保留最近的三个分区。并在hive cli
 * 执行show partitions ods.orders命令，将结果截图粘贴至对应报告中；
 */
object CleanTask6 {
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

    val etl_dateRow: Array[Row] = sparkSession.sql(
      s"""
         |select
         |  etl_date
         |from
         |  shtd_store_ods.orders
         |""".stripMargin)
      .dropDuplicates("etl_date")
      .sort(col("etl_date").desc)
      .collect()

    for (x <- 3 until(etl_dateRow.length)) {
      val eachPartition: Any = etl_dateRow(x).get(0)
      // 最终删除
      sparkSession.sql(
        s"""
           |alter table
           |  shtd_store_ods.orders
           |if exists
           |  partition(etl_date=${eachPartition})
           |""".stripMargin)
    }

    sparkSession.sql(
      s"""
         |show partitions shtd_store_ods.orders
         |""".stripMargin).show()
  }
}
