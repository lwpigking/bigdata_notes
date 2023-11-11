# Hudi 滴滴数据分析

## SparkUtils

```scala
package com.lwPigKing.hudi.didi

import org.apache.spark.sql.SparkSession

/**
 * Project:  BigDataProject
 * Create date:  2023/8/7
 * Created by lwPigKing
 */

/**
 * SparkSQL utility class when manipulating data
 */

object SparkUtils {

  def createSparkSession(clazz: Class[_], master: String = "local[4]", partitions: Int = 4): SparkSession = {
    SparkSession.builder()
      .appName(clazz.getSimpleName.stripSuffix("$"))
      .master(master)
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.sql.shuffle.partitions", partitions)
      .getOrCreate()
  }

}

```



## 分析主要步骤

```scala
package com.lwPigKing.hudi.didi

import org.apache.spark.sql.functions.{col, concat_ws, unix_timestamp}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

/**
 * Project:  BigDataProject
 * Create date:  2023/8/7
 * Created by lwPigKing
 */

/**
 * Didi Haikou Mobility operation data analysis uses SparkSQL to manipulate the data,
 * first read the CSV file,and save it to the Hudi table
 */

/**
 * development major steps
 * 1.build SparkSession instance objects(integrating Hudi and HDFS)
 * 2.load the local CSV file Didi trip data
 * 3.ETL processing
 * 4.save the converted data to the Hudi table
 * 5.stop the SparkSession
 */

object DidiStorageSpark {
  def main(args: Array[String]): Unit = {
    // dataPath
    val dataPath: String = "dwv_order_make_haikou_1.txt"

    // Hudi table
    val hudiTableName: String = "tbl_didi_haikou"
    val hudiTablePath: String = "/hudi-warehouse/tbl_didi_haikou"

    // step1
    val sparkSession: SparkSession = SparkUtils.createSparkSession(this.getClass)
    import sparkSession.implicits._

    // step2
    val didiDF: DataFrame = sparkSession.read
      .option("sep", "\\t")
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(dataPath)
//    didiDF.printSchema()
//    didiDF.show(10, truncate = false)

    // step3
    val etlDF: DataFrame = didiDF
      .withColumn("partitionpath", concat_ws("/", col("year"), col("month"), col("day")))
      .drop("year", "month", "day")
      .withColumn("ts", unix_timestamp(col("departure_time"), "yyyy-MM-dd HH:mm:ss"))
//    etlDF.show(10, truncate = false)

    // step4
    import org.apache.hudi.DataSourceWriteOptions._
    import org.apache.hudi.config.HoodieWriteConfig._
    etlDF.write
      .mode(SaveMode.Overwrite)
      .format("hudi")
      .option("hoodie.insert.shuffle.parallelism", 2)
      .option("hoodie.upsert.shuffle.parallelism", 2)
      .option(RECORDKEY_FIELD_OPT_KEY, "order_id")
      .option(PRECOMBINE_FIELD_OPT_KEY, "ts")
      .option(PARTITIONPATH_FIELD_OPT_KEY, "partitionpath")
      .option(TABLE_NAME, hudiTableName)
      .save(hudiTablePath)

    // step5
    sparkSession.close()

  }
}
```



## Spark分析

```scala
package com.lwPigKing.hudi.didi

import org.apache.commons.lang3.time.FastDateFormat
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{col, sum, udf, when}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.util.{Calendar, Date}

/**
 * Project:  BigDataProject
 * Create date:  2023/8/7
 * Created by lwPigKing
 */
object DidiAnalysisSpark {
  def main(args: Array[String]): Unit = {
    val sparkSession: SparkSession = SparkUtils.createSparkSession(this.getClass, partitions = 8)
    import sparkSession.implicits._

    val hudiTablePath: String = "/hudi-warehouse/tbl_didi_haikou"
    val didiDF: DataFrame = sparkSession.read.format("hudi").load(hudiTablePath)
    val hudiDF: DataFrame = didiDF.select("order_id", "product_id", "type", "traffic_type",
      "pre_total_fee", "start_dest_distance", "departure_time"
    )

    /**
     * Indicator calculation 1
     * For the data of Didi Travel in Haikou City,
     * according to the order type statistics,
     * the field used: product_id,
     * the median value [1 Didi car, 2 Didi enterprise car, 3 Didi express, 4 Didi enterprise express]
     */
    val reportDF: DataFrame = hudiDF.groupBy("product_id").count()
    val to_name: UserDefinedFunction = udf(
      (productID: Int) => {
        productID match {
          case 1 => "滴滴专车"
          case 2 => "滴滴企业专车"
          case 3 => "滴滴快车"
          case 4 => "滴滴企业快车"
        }
      }
    )
    val resultDF: DataFrame = reportDF.select(
      to_name(col("product_id")).as("order_type"),
      col("count").as("total")
    )
    resultDF.printSchema()
    resultDF.show(10, truncate = false)

    /**
     * Indicator calculation 2
     * Order timeliness statistics
     * the filed used: type
     */
    val reportDF2: DataFrame = hudiDF.groupBy("type").count()
    val to_name2: UserDefinedFunction = udf(
      (realtimeType: Int) => {
        realtimeType match {
          case 0 => "实时"
          case 1 => "预约"
        }
      }
    )
    val resultDF2: DataFrame = reportDF2.select(
      to_name2(col("type")).as("order_realtime"),
      col("count").as("total")
    )
    reportDF2.printSchema()
    reportDF2.show(10, truncate = false)

    /**
     * Indicator calculation 3
     * Traffic type statistics
     * the field used: traffic_type
     */
    val reportDF3: DataFrame = hudiDF.groupBy("traffic_type").count()
    val to_name3: UserDefinedFunction = udf(
      (trafficType: Int) => {
        trafficType match {
          case 0 => "普通散客"
          case 1 => "企业时租"
          case 2 => "企业接机套餐"
          case 3 => "企业送机套餐"
          case 4 => "拼车"
          case 5 => "接机"
          case 6 => "送机"
          case 302 => "跨城拼车"
          case _ => "未知"
        }
      }
    )
    val resultDF3: DataFrame = reportDF3.select(
      to_name3(col("traffic_type")).as("traffic_type"),
      col("count").as("total")
    )
    resultDF3.printSchema()
    resultDF3.show(10, truncate = false)

    /**
     * Order price statistics, which will be counted in stages
     * the field used: pre_total_fee
     */
    val resultDF4: DataFrame = hudiDF.agg(
      // price: 0-15
      sum(
        when(
          col("pre_total_fee").between(0, 15), 1
        ).otherwise(0)
      ).as("0~15"),
      // price 16-30
      sum(
        when(
          col("pre_total_fee").between(16, 30), 1
        ).otherwise(0)
      ).as("16~30"),
      // price：31-50
      sum(
        when(
          col("pre_total_fee").between(31, 50), 1
        ).otherwise(0)
      ).as("31~50"),
      // price：50-100
      sum(
        when(
          col("pre_total_fee").between(51, 100), 1
        ).otherwise(0)
      ).as("51~100"),
      // price：100+
      sum(
        when(
          col("pre_total_fee").gt(100), 1
        ).otherwise(0)
      ).as("100+")
    )
    resultDF4.printSchema()
    resultDF4.show(10, truncate = false)

    /**
     * Order week grouping statistics
     * the field used: departure_time
     */
    val to_week: UserDefinedFunction = udf(
      (dateStr: String) => {
        val format: FastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd")
        val calendar: Calendar = Calendar.getInstance()
        val date: Date = format.parse(dateStr)
        calendar.setTime(date)
        val dayWeek: String = calendar.get(Calendar.DAY_OF_WEEK) match {
          case 1 => "星期日"
          case 2 => "星期一"
          case 3 => "星期二"
          case 4 => "星期三"
          case 5 => "星期四"
          case 6 => "星期五"
          case 7 => "星期六"
        }
        dayWeek
      }
    )
    val resultDF5: DataFrame = hudiDF.select(
      to_week(col("departure_time")).as("week")
    )
      .groupBy(col("week")).count()
      .select(
        col("week"), col("count").as("total")
      )
    resultDF5.printSchema()
    resultDF5.show(10, truncate = false)


    sparkSession.stop()

  }
}

```

