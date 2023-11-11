package com.lwpigking.ETL

import org.apache.spark.sql.SparkSession

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/28
 * Created by lwPigKing
 */

/**
 * 抽取shtd_store库中LINEITEM的增量数据进入Hive的ods库中表lineitem，根据
 * LINEITEM表中orderkey作为增量字段，只将新增的数据抽入，字段类型不变，同时添加静态分
 * 区，分区字段类型为String，且值为当前比赛日的前一天日期（分区字段格式为yyyyMMdd）。
 * 并在hive cli执行show partitions ods.lineitem命令，将结果截图复制粘贴至对应报告中。
 */

object IncrementalStaticExtraction {
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

    sparkSession.read.jdbc(mysql, "lineitem", properties).createTempView("mysql_lineitem")

    // 选取hive中最大的orderkey
    val MaxOrderKey: Int = sparkSession.sql(
      S"""
         |select max(cast(orderkey as INT))
         |from shtd_store_ods.orders
         |""".stripMargin).collect()(0).get(0).toString.toInt

    // 与mysql中的orderkey进行比较，选取增量数据
    sparkSession.sql(
      s"""
         |select * from mysql_lineitem
         |where cast(orderkey as INT) > ${MaxOrderKey}
         |""".stripMargin).createTempView("hive_orders")

    // 抽取增量数据
    sparkSession.sql(
      s"""
         |insert into table shtd_store_ods.orders
         |partitions(etl_date=20230926)
         |select * from hive_orders
         |""".stripMargin)

    // 查看结果
    sparkSession.sql(
      s"""
         |show partitions ods.lineitem
         |""".stripMargin).show()
  }
}
