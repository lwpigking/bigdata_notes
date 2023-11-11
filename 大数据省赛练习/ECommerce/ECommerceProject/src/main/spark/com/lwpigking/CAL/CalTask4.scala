package com.lwpigking.CAL

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/10/1
 * Created by lwPigKing
 */

/**
 * 根据dwd层表统计每人每天下单的数量和下单的总金额，存入dws层的
 * customer_consumption_day_aggr表（表结构如下）中，然后在hive cli中按照cust_key，
 * totalconsumption, totalorder三列均逆序排序的方式，查询出前5条，将SQL语句与执行结果截图粘贴至对应报告中;
 */

/**
 * 字段 类型
 * cust_key int
 * cust_name string
 * totalconsumption double
 * totalorder int
 * year int
 * month int
 * day int
 */

object CalTask4 {
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

    val resultDataFrame: DataFrame = sparkSession.sql(
      s"""
         |select
         |  cast(shtd_store_dwd.dim_customer.custkey as INT) as cust_key,
         |  shtd_store_dwd.dim_customer.name as cust_name,
         |  sum(cast(shtd_store_dwd.fact_orders.totalprice as DOUBLE)) as totalconsumption,
         |  count(*) as totalorder,
         |  cast(year(cast(shtd_store_dwd.fact_orders as date)) as INT) as year,
         |  cast(month(cast(shtd_store_dwd.fact_orders as date)) as INT) as month,
         |  cast(day(cast(shtd_store_dwd.fact_orders as date)) as INT) as day
         |where
         |  shtd_store_dwd.dim_customer.custkey = shtd_store_dwd.fact_orders.custkey
         |group by
         |  shtd_store_dwd.dim_customer.custkey,
         |  shtd_store_dwd.dim_customer.name,
         |  year(cast(shtd_store_dwd.fact_orders as date),
         |  month(cast(shtd_store_dwd.fact_orders as date),
         |  day(cast(shtd_store_dwd.fact_orders as date)
         |""".stripMargin)

    resultDataFrame.write.mode(SaveMode.Overwrite)
      .saveAsTable("shtd_store_dws.customer_consumption_day_aggr")

    sparkSession.sql(
      s"""
         |select *
         |from
         |  shtd_store_dws.customer_consumption_day_aggr
         |order by
         |  cust_key desc,
         |  totalconsumption desc,
         |  totalorder desc limit 5
         |""".stripMargin).show()
  }
}
