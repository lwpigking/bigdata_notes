package com.lwpigking.CAL

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/10/1
 * Created by lwPigKing
 */

/**
 * 请根据dwd层表计算出1998每个国家的平均消费额和所有国家平均消费额相比较结果
 *（“高/低/相同”）,存入MySQL数据库shtd_store的nationavgcmp表（表结构如下）中，然后在
 * Linux的MySQL命令行中根据比较结果、所有国家内客单价、该国家内客单价三列均逆序排序的
 * 方式，查询出前5条，将SQL语句与执行结果截图粘贴至对应报告中;
 */

/**
 * 字段         类型
 * nationkey    int
 * nationname   text
 * nationavgconsumption double
 * compartmentalization double
 * comparison string
 */
object CalTask2 {
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

    /**
     * 1.先统计每个人的总交易额（筛选1988年）
     * 2.按照每个人的国籍进行聚合累加，计算每个国家的平均消费额
     * 3.计算所有国家的平均消费额
     * 4.两者消费额进行比较
     */

    // 1.每个人总交易额
    val customerTotalPriceDataFrame: DataFrame = sparkSession.sql(
      s"""
         |select
         |  cast(shtd_store_dwd.dim_nation.nationkey as INT) as nationkey,
         |  shtd_store_dwd.dim_nation.name as nationname,
         |  shtd_store_dwd.fact_orders.custkey as custkey,
         |  sum(cast(shtd_store_dwd.fact_orders.totalprice as DOUBLE)) as customer_totalprice
         |from
         |  shtd_store_dwd.dim_natiom,
         |  shtd_store_dwd.dim_customer,
         |  shtd_store_dwd.fact_orders
         |where
         |  shtd_store_dwd.dim_nation.nationkey = shtd_store_dwd.dim_customer.nationkey
         |  and
         |  shtd_store_dwd.dim_customer.custkey = shtd_store_dwd.fact_orders.custkey
         |  and
         |  year(cast(shtd_store_dwd.fact_orders.orderdate as date)) = 1988
         |group by
         |  shtd_store_dwd.dim_nation.nationkey,
         |  shtd_store_dwd.dim_nation.nationname,
         |  shtd_store_dwd.fact_orders.custkey
         |""".stripMargin)

    // 2.按照国家聚合，计算国家的平均消费额
    customerTotalPriceDataFrame
      .groupBy("nationkey", "nationname")
      .avg("customer_totalprice").as("nationavgconsumption")
      .select("nationkey", "nationname", "nationavgconsumption")
      .createTempView("avgnation")

    // 3.计算所有国家消费额
    // 计算各个国家的所有消费额和消费人数
    customerTotalPriceDataFrame.createTempView("customerTotalPrice")
    sparkSession.sql(
      s"""
         |select
         |  nationkey,
         |  nationname,
         |  sum(customer_totalprice) as alltotalprice,
         |  count(*) as totalpopulation
         |from
         |  customerTotalPrice
         |group by
         |  nationkey,
         |  nationname
         |""".stripMargin).createTempView("allnation")

    // 各个国家和所有国家两张表进行连接
    sparkSession.sql(
      s"""
         |select
         |  nationkey,
         |  nationname,
         |  alltotalprice,
         |  totalpopulation,
         |  nationavgconsumption
         |from
         |  allnation,
         |  avgnation
         |where allnation.nationname = avgnation.nationname
         |""".stripMargin).createTempView("allAndAvgNation")

    // 所有国家平均消费额 = 所有国家总消费额 / 所有国家消费人数
    // 在最后比较的时候，需要用到所有国家平均消费额和各个国家平均消费额这两列
    // 所以在这使用开窗函数，保留各个国家平均消费额这列，增加所有国家平均消费额这列
    sparkSession.sql(
      s"""
         |select
         |  nationkey,
         |  nationname,
         |  nationavgconsumption,
         |  sum(alltotalprice) over() / sum(totalpopulation) over() as compartmentalization
         |from
         |  allAndAvgNation
         |""".stripMargin).createTempView("consumptionResult")

    // 4.比较消费额
    val resultDataFrame: DataFrame = sparkSession.sql(
      s"""
         |select *
         |  case
         |    when nationavgconsumption > compartmentalization then '高'
         |    when nationavgconsumption < compartmentalization then '低'
         |    else '相同'
         |    end as comparison
         |""".stripMargin)

    // 写入mysql表
    resultDataFrame
      .write.mode(SaveMode.Overwrite)
      .jdbc(mysql, "nationavgcmp", properties)

    // 查看结果
    sparkSession.read.jdbc(mysql, "nationavgcmp", properties).createTempView("nationavgcmp")
    sparkSession.sql(
      s"""
         |select *
         |from
         |  nationavgcmp
         |order by
         |  comparison desc,
         |  compartmentalization desc,
         |  nationavgconsumption desc limit 5
         |""".stripMargin).show()
  }
}