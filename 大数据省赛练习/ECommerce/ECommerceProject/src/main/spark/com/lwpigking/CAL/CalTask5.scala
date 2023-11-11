package com.lwpigking.CAL

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.expressions.{Window, WindowSpec}
import org.apache.spark.sql.functions.{col, row_number}

import java.util.Properties

/**
 * Project:  ECommerceProject
 * Create date:  2023/10/1
 * Created by lwPigKing
 */

/**
 * 根据dws层表customer_consumption_day_aggr表，再联合
 * dwd.dim_region,dwd.dim_nation统计每人每个月下单的数量和下单的总金额，并按照
 * cust_key，totalconsumption，totalorder，month进行分组逆序排序（以cust_key为分组条
 * 件），将计算结果存入MySQL数据库shtd_store的customer_consumption_month_aggr表（表
 * 结构如下）中，然后在Linux的MySQL命令行中根据订单总数、消费总额、国家表主键三列均逆
 * 序排序的方式，查询出前5条，将SQL语句与执行结果截图粘贴至对应报告中;
 */

/**
 * 字段 类型
 * cust_key int
 * cust_name string
 * nationkey int
 * nationname text
 * regionkey int
 * regionname text
 * totalconsumption double
 * totalorder int
 * year int
 * month int
 * sequence int
 */
object CalTask5 {
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

    val tmpDataFrame: DataFrame = sparkSession.sql(
      s"""
         |select
         |  cust_key,
         |  cust_name,
         |  cast(shtd_store_dwd.dim_nation.nationkey as INT) as nationkey,
         |  shtd_store_dwd.dim_nation.name as nationname,
         |  cast(shtd_store_dwd.dim_region.regionkey as INT) as regionkey,
         |  shtd_store_dwd.dim_region.name as regionname,
         |  sum(totalconsumption) as totalconsumption,
         |  sum(totalorder) as totalorder,
         |  `year`,
         |  `month`
         |from
         |  shtd_store_dws.customer_consumption_aggr,
         |  shtd_store_dwd.dim_region,
         |  shtd_store_dwd.dim_nation
         |where
         |  shtd_store_dwd.dim_region.regionkey = shtd_store_dwd.dim_nation.regionkey
         |  and
         |  shtd_store_dwd.dim_nation.nationkey = shtd_store_dwd.dim_customer.nationkey
         |  and
         |  shtd_store_dwd.dim_customer.custkey = shtd_store_dws.customer_consumption_aggr.cust_key
         |group by
         |  cust_key,
         |  cust_name,
         |  shtd_store_dwd.dim_nation.nationkey,
         |  shtd_store_dwd.dim_nation.name,
         |  shtd_store_dwd.dim_region.regionkey,
         |  shtd_store_dwd.dim_region.name,
         |  `year`,
         |  `month`
         |""".stripMargin)

    val windowSpec: WindowSpec = Window.partitionBy("cust_key")
      .orderBy(
        col("totalconsumption").desc,
        col("totalorder").desc,
        col("month").desc
      )

    val resultDataFrame: DataFrame = tmpDataFrame.withColumn("sequence", row_number().over(windowSpec))

    resultDataFrame.write.mode(SaveMode.Overwrite)
      .jdbc(mysql, "customer_consumption_month_aggr", properties)

    sparkSession.read.jdbc(mysql, "customer_consumption_month_aggr", properties)
      .createTempView("customer_consumption_month_aggr")

    sparkSession.sql(
      s"""
         |select *
         |from
         |  customer_consumption_month_aggr
         |order by
         |  totalorder desc,
         |  totalconsumption desc,
         |  nationkey desc limit 5
         |""".stripMargin).show()

  }
}
