package com.lwpigking.ETL

import org.apache.spark.sql.SparkSession

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/27
 * Created by lwPigKing
 */

/**
 * 4、 抽取shtd_store库中PARTSUPP的全量数据进入Hive的ods库中表partsupp。字段排
 * 序、类型不变，同时添加静态分区，分区字段类型为String，且值为当前比赛日的前一天日期
 *（分区字段格式为yyyyMMdd）。并在hive cli执行show partitions ods.partsupp命令，将结果
 * 截图复制粘贴至对应报告中；
 */

object FullStaticExtractionPartsupp {
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

    sparkSession.read.jdbc(mysql, "partsupp", properties).createTempView("mysql_partsupp")
    sparkSession.sql(
      s"""
         |insert overwrite table shtd_store.ods
         |partitions(etl_date=20230925)
         |select * from mysql_partsupp
         |""".stripMargin)
    sparkSession.sql(
      s"""
         |show partitions shtd_store_ods.partsupp
         |""".stripMargin).show()
  }
}
