package com.lwpigking.CAL

import org.apache.spark.sql.expressions.{Window, WindowSpec}
import org.apache.spark.sql.functions.{col, concat, concat_ws, lead}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/10/1
 * Created by lwPigKing
 */

/**
 * 编写Scala工程代码，根据dwd层表统计连续两个月下单并且下单金额保持增长的用户，
 * 订单发生时间限制为大于等于1997年，存入MySQL数据库shtd_store的usercontinueorder表
 * (表结构如下)中。然后在Linux的MySQL命令行中根据订单总数、消费总额、客户主键三列均逆
 * 序排序的方式，查询出前5条，将SQL语句与执行结果截图粘贴至对应报告中。
 */

/**
 * 字段   类型
 * custkey int
 * custname text
 * month text
 * totalconsumption double
 * totalorder int
 */

object CalTask3 {
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

    sparkSession.sql(
      s"""
         |select
         |  cast(shtd_store_dwd.dim_customer.custkey as INT) as custkey,
         |  shtd_store_dwd.dim_customer.name as custname,
         |  year(cast(shtd_store_dwd.fact_orders.orderdate as date)) as year,
         |  month(cast(shtd_store_dwd.fact_orders.orderdate as date)) as month
         |  sum(cast(shtd_store_dwd.fact_orders.orderdate as DOUBLE)) as totalPriceSum
         |from
         |  shtd_store_dwd.dim_customer,
         |  shtd_store_dwd.fact_orders
         |where
         |  year(cast(shtd_store_dwd.fact_orders.orderdate as date)) >= 1977
         |  and
         |  shtd_store_dwd.dim_customer.custkey = shtd_store_dwd.fact_orders.custkey
         |group by
         |  shtd_store_dwd.dim_customer.custkey,
         |  shtd_store_dwd.dim_customer.name,
         |  year(cast(shtd_store_dwd.fact_orders.orderdate as date)),
         |  month(cast(shtd_store_dwd.fact_orders.orderdate as date))
         |""".stripMargin).createTempView("selectData")

    sparkSession.sql(
      s"""
         |select *,
         |  lead(`year`) over (partition by custkey, custname order by `year`, `month`) as next_year,
         |  lead(`month`) over (partition by custkey, custname order by `year`, `month`) as next_month,
         |  lead(`totalPriceSum`) over (partition by custkey, custname order by `year`, `month`) as next_totalPriceSum
         |from
         |  selectData
         |""".stripMargin)
      // 去除空值
      .filter(col("next_year").isNotNull
        && col("next_month").isNotNull
        && col("next_totalPriceSum").isNotNull)
      // 留下连续两个月下单的
      // next_year - year == 0 && next_month - month == 1
      .filter(
        (col("next_year").cast("int") - col("year").cast("int")) === 0
        &&
          (col("next_month").cast("int") - col("month").cast("int")) === 1
      )
      // 留下保持下单金额持续增长的
      .filter(
        col("next_totalPriceSum") > col("totalPriceSum")
      )
      // 修改month列
      // 202304_202305
      .withColumn("month", concat_ws(
        "_",
        concat(col("year"), col("month")),
        concat(col("next_year"), col("next_month"))
        )
      )
      // 计算totalconsumption
      .withColumn("totalconsumption", col("totalPriceSum") + col("next_totalPriceSum"))
      .createTempView("last")

    // 计算totalorder
    // 按照custkey，custname分区进行开窗计算
    val resultDataFrame: DataFrame = sparkSession.sql(
      s"""
         |select
         |  custkey,
         |  custname,
         |  month,
         |  totalconsumption,
         |  count(*) over (partition by custkey, custname) as totalorder
         |from
         |  last
         |""".stripMargin)

    resultDataFrame.write.mode(SaveMode.Overwrite)
      .jdbc(mysql, "usercontinueorder", properties)

    // 写入结果
    sparkSession.read.jdbc(mysql, "usercontinueorder", properties).createTempView("usercontinueorder")
    // 查询结果
    sparkSession.sql(
      s"""
         |select *
         |from
         |  usercontinueorder
         |order by
         |  totalorder desc,
         |  totalconsumption desc,
         |  custkey desc limit 5
         |""".stripMargin).show()
  }
}