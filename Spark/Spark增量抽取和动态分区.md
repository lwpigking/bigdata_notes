```scala
package ETL

import java.util.Properties

import org.apache.spark.sql.SparkSession

/**
 * Project:  Bigdata
 * Create date:  2023/4/26
 * Created by fujiahao
 */
object SparkIncDynamicETL {
  def main(args: Array[String]): Unit = {
    val warehouse: String = "hdfs://master:9000/hive/warehouse"
    val metastore: String = "thrift://master:9083"
    val mysql: String = "jdbc:mysql://master:3306/ds_pub?useUnicode=true&characterEncoding=utf-8"
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
     * 抽取shtd_store库中ORDERS的增量数据进入Hive的ods库中表orders，要求只取1997
     * 年12月1日及之后的数据（包括1997年12月1日），根据ORDERS表中ORDERKEY作为增量字段
     * （提示：对比MySQL和Hive中的表的ORDERKEY大小），只将新增的数据抽入，字段类型不变，
     * 同时添加动态分区，分区字段类型为String，且值为ORDERDATE字段的内容（ORDERDATE的格
     * 式为yyyy-MM-dd，分区字段格式为yyyyMMdd），。并在hive cli执行select
     * count(distinct(etldate)) from ods.orders命令，将结果截图复制粘贴至对应报告中；
     */

    val MaxOrderKey: Int = sparkSession.sql(
      s"""
         |select max(cast(orderkey as INT))
         |from ods.orders
         |""".stripMargin).collect()(0).get(0).toString.toInt

    sparkSession.read.jdbc(mysql, "ORDERS", properties).createTempView("mysql_orders")
    sparkSession.sql(
      s"""
         |from mysql_orders
         |select *,
         |date_format(orderdate, 'yyyyMMdd') as etldate
         |where cast(orderkey as INT) > ${MaxOrderKey}
         |and cast(orderdate as date) > '1997-12-01'
         |""".stripMargin).createTempView("hive_orders")
    sparkSession.sql(
      s"""
         |insert into table ods.orders
         |partition(etldate)
         |select * from hive_orders
         |""".stripMargin)
  }
}

```

