package com.lwpigking.ETL

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, date_format}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/27
 * Created by lwPigKing
 */

/**
 * 抽取shtd_store库中ORDERS的增量数据进入Hive的ods库中表orders，要求只取1997
 * 年12月1日及之后的数据（包括1997年12月1日），根据ORDERS表中ORDERKEY作为增量字段
 * （提示：对比MySQL和Hive中的表的ORDERKEY大小），只将新增的数据抽入，字段类型不变，
 * 同时添加动态分区，分区字段类型为String，且值为ORDERDATE字段的内容（ORDERDATE的格
 * 式为yyyy-MM-dd，分区字段格式为yyyyMMdd），。并在hive cli执行select
 * count(distinct(etldate)) from ods.orders命令，将结果截图复制粘贴至对应报告中；
 */

object IncrementalDynamicExtraction {
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

    sparkSession.read.jdbc(mysql, "orders", properties).createTempView("mysql_orders")
    // 取hive中最大的orderkey，与mysql中的orderkey进行比较
    val MaxOrderKey: Int = sparkSession.sql(
      s"""
         |select max(cast(orderkey as INT)) from shtd_store_ods.orders
         |""".stripMargin).collect()(0).get(0).toString.toInt
    // 选取mysql中的增量数据
    // 1.orderkey进行比较
    // 2.选取orderdate大于19971201
    sparkSession.sql(
      s"""
         |select * from mysql_orders
         |where cast(orderkey as INT) > ${MaxOrderKey}
         |and cast(orderdate as date) > '1997-12-01'
         |""".stripMargin)
      // 3.新增一类etl_date作为动态分区
      .withColumn("etl_date", date_format(col("orderdate"), "yyyyMMdd"))
      .createTempView("hive_orders")
    // 选取mysql中的增量数据，抽取到hive表中
    sparkSession.sql(
      s"""
         |insert into table shtd_store_ods.orders
         |partitions(etl_date)
         |select * from hive_orders
         |""".stripMargin)
    sparkSession.sql(
      s"""
         |select count(distinct(etl_date)) from shtd_store_ods.ordrs
         |""".stripMargin).show()
  }
}
