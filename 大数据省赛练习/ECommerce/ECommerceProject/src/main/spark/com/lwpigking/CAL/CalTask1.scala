package com.lwpigking.CAL

import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/9/30
 * Created by lwPigKing
 */

/**
 * 编写Scala工程代码，根据dwd层表统计每个地区、每个国家、每个月下单的数量和下单
 * 的总金额，存入MySQL数据库shtd_store的nationeverymonth表（表结构如下）中，然后在
 * Linux的MySQL命令行中根据订单总数、消费总额、国家表主键三列均逆序排序的方式，查询出
 * 前5条，将SQL语句与执行结果截图粘贴至对应报告中;
 */

/**
 * 字段 类型 中文含义 备注
 * nationkey int 国家表主键
 * nationname text 国家名称
 * regionkey int 地区表主键
 * regionname text 地区名称
 * totalconsumption double 消费总额 当月消费订单总额
 * totalorder int 订单总数 当月订单总额
 * year int 年 订单产生的年
 * month int 月 订单产生的月
 */

object CalTask1 {
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

    // 计算过程
    val resultDataFrame: DataFrame = sparkSession.sql(
      s"""
         |select
         |  cast(shtd_store_dwd.dim_nation.nationkey as INT) as nationkey,
         |  shtd_store_dwd.dim_nation.name as nationname,
         |  cast(shtd_store_dwd.dim_region.regionkey as INT) as regionkey,
         |  shtd_store_dwd.dim_nation.name as regionname,
         |  cast(sum(shtd_store_dwd.fact_orders.totalprice) as DOUBLE) as totalconsumption,
         |  count(*) as totalorder,
         |  year(cast(shtd_store_dwd.fact_orders.orderdate as date)) as year,
         |  month(cast(shtd_store_dwd.fact_orders.orderdate as date)) as month
         |from
         |  shtd_store_dwd.dim_nation,
         |  shtd_store_dwd.dim_region,
         |  shtd_store_dwd.dim_customer,
         |  shtd_store_dwd.fact_orders
         |where
         |  shtd_store_dwd.dim_region.regionkey = shtd_store_dwd.dim_nation.regionkey
         |  and
         |  shtd_store_dwd.dim_nation.nationkey = shtd_store_dwd.dim_customer.nationkey
         |  and
         |  shtd_store_dwd.dim_customer.custkey = shtd_store_dwd.fact_orders.custkey
         |""".stripMargin)

    // 写入mysql
    resultDataFrame.write.mode(SaveMode.Overwrite)
      .jdbc(mysql, "nationeverymonth", properties)

    // 查询计算结果：用sparksql api
    sparkSession.read.jdbc(mysql, "nationeverymonth", properties).createTempView("mysql_nationeverymonth")
    sparkSession.sql(
      s"""
         |select * from mysql_nationeverymonth
         |""".stripMargin)
      .orderBy(col("totalorder").desc)
      .orderBy(col("totalconsumption").desc)
      .orderBy(col("nationkey").desc)
      .limit(5)
      .show()

  }
}
