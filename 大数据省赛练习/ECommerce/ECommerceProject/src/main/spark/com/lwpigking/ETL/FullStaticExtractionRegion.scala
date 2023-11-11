package com.lwpigking.ETL

import org.apache.spark.sql.SparkSession

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/27
 * Created by lwPigKing
 */

/**
 * 5、 抽取shtd_store库中REGION的全量数据进入Hive的ods库中表region，字段排序、类型
 * 不变，同时添加静态分区，分区字段类型为String，且值为当前比赛日的前一天日期（分区字段
 * 格式为yyyyMMdd）。并在hive cli执行show partitions ods.region命令，将结果截图复制粘贴
 * 至对应报告中；
 */
object FullStaticExtractionRegion {
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

    sparkSession.read.jdbc(mysql, "region", properties).createTempView("mysql_region")
    sparkSession.sql(
      s"""
         |insert overwrite table shtd_store_ods.region
         |partitions(etl_date=20230925)
         |select * from mysql_region
         |""".stripMargin)
    sparkSession.sql(
      s"""
         |show partitions shtd_store_ods.region
         |""".stripMargin).show()

  }
}
