# FlinkSQL&TableAPI

这部分相对于SparkSQL理解起来可能会比较难（主要是FlinkSQL的时间语义并不好写），并且如果要构成一个完整的FlinkSQL和TableAPI的知识体系，难度还是比较大的。只有多练习了。

## 获取表环境

```scala
val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
env.setParallelism(1)
// 获取表环境
val tableEnv: StreamTableEnvironment = StreamTableEnvironment.create(env)
```



## 连接外部系统

```scala
// 创建输入表，连接外部系统读取数据
tableEnv.executeSql("create temporary table inputTable ... with ('connector' = ...)")

// 注册一个表，连接到外部系统，用于输出
tableEnv.executeSql("create temporary table outputTable ... with ('connect' = ...)")
```



## 表的查询

```scala
// 用SQL的方式提取数据
val visitTable: Table = tableEnv.sqlQuery(s"select url, user from ${eventTable}")

// 用Table API方式提取数据
val visitTable: Table = eventTable.select($("url"), $("user"))

// 执行SQL对表进行查询转换，得到一个新的表
val table1: Table = tableEnv.sqlQuery("select ... from inputTable...")

// 使用TableAPI对表进行查询转换，得到一个新的表
val table2: Table = tableEnv.from("inputTable").select("...")

// 将得到的结果写入输出表
val tableResult: TableResult = table1.executeInsert("outputTable")

// 虚拟表(临时视图)
// 得到的newTable是一个中间转换结果，如果之后又希望直接使用这个表执行SQL，就可以创建虚拟表
val newTable: Table = tableEnv.sqlQuery("select ... from MyTable")
// 第一个参数是注册的表名，第二个参数是Table对象
tableEnv.createTemporaryView("newTable", newTable)

```

## 表和流相互转换

```scala
// 表 -> 流：写完SQL后转换成流然后print测试
// toDataStream并不适合聚合操作，是因为无法进行更新
tableEnv.toDataStream(aliceVisitTable).print()
// toChangelogStream更新日志
val urlCountTable2: Table = tableEnv.sqlQuery("select user, count(url) from EventTable group by user")
tableEnv.toChangelogStream(urlCountTable2).print()

// 流 -> 表
val eventTable3: Table = tableEnv.fromDataStream(eventStream)
val eventTable4: Table = tableEnv.fromDataStream(eventStream, $("timestamp").as("ts"), $("url"))
tableEnv.createTemporaryView("EventTable", eventStream, $("timestamp").as("ts"), $("url"))
```



## TableAPI过程

```scala
package Tutorial.TableAPIAndSQL

import Tutorial.DataStreamAPI.SensorReading
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.api.Expressions.$
import org.apache.flink.table.api.{AnyWithOperations, FieldExpression, LiteralIntExpression, Table, Tumble, UnresolvedFieldExpression}
import org.apache.flink.table.api.bridge.scala.{StreamTableEnvironment, dataStreamConversions, tableConversions}

/**
 * Project:  BigDataCode
 * Create date:  2023/7/7
 * Created by lwPigKing
 */
object TableAPI {
  def main(args: Array[String]): Unit = {
    /**
     * 简单了解一下TableAPI的过程
     */
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val inputStream: DataStream[String] = env.readTextFile("D:\\BigDataCode\\Code\\Flink\\Tutorial\\TableAPIAndSQL\\sensor.txt")
    val dataStream: DataStream[SensorReading] = inputStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
    })

    val tableEnvironment: StreamTableEnvironment = StreamTableEnvironment.create(env)
    val dataTable: Table = tableEnvironment.fromDataStream(dataStream)

    val selectedTable: Table = dataTable.select($("id"), $("temperature")).filter("id = 'sensor_1'")

    tableEnvironment.toDataStream(selectedTable).print()

    env.execute()


    /**
     * TableAPI的窗口聚合操作
     */
    // 统计每10秒中每个传感器温度值的个数
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val inputStream: DataStream[String] = env.readTextFile("D:\\BigDataCode\\Code\\Flink\\Tutorial\\TableAPIAndSQL\\sensor.txt")
    val dataStream: DataStream[SensorReading] = inputStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).toDouble)
    }).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
      override def extractTimestamp(element: SensorReading): Long = element.timestamp * 1000L
    })

    val tableEnvironment: StreamTableEnvironment = StreamTableEnvironment.create(env)
    // 处理时间：.proctime  事件时间：rowtime
    val dataTable: Table = tableEnvironment.fromDataStream(dataStream, $("id"), $("temperature"), $("timestamp").rowtime())

    // 按照时间开窗聚合统计
    val resultTable: Table = dataTable
      .window(Tumble over 10.seconds on $"timestamp" as "tw")
      .groupBy($"id", $"tw")
      .select($"id", $"id".count)
    // 如果使用了groupBy，table转换成流的时候只能用toRetractStream
    // Boolean:true表示最新的数据（Insert），false表示过期老数据（Delete）
    val selectedStream: DataStream[(Boolean, (String, Long))] = resultTable.toRetractStream[(String, Long)]
    selectedStream.print()

    env.execute()


  }
}

```



## SQL过程

```scala
package Tutorial.TableAPIAndSQL

import Tutorial.DataStreamAPI.SensorReading
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.api.Expressions.$
import org.apache.flink.table.api.Table
import org.apache.flink.table.api.bridge.scala.{StreamTableEnvironment, tableConversions}

/**
 * Project:  BigDataCode
 * Create date:  2023/7/7
 * Created by lwPigKing
 */
object SQLExample {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val inputStream: DataStream[String] = env.readTextFile("D:\\BigDataCode\\Code\\Flink\\Tutorial\\TableAPIAndSQL\\sensor.txt")
    val dataStream: DataStream[SensorReading] = inputStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).toDouble)
    }).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
      override def extractTimestamp(element: SensorReading): Long = element.timestamp * 1000L
    })

    val tableEnvironment: StreamTableEnvironment = StreamTableEnvironment.create(env)
    // 处理时间：.proctime  事件时间：rowtime
    val dataTable: Table = tableEnvironment.fromDataStream(dataStream, $("id"), $("temperature"), $("timestamp").rowtime().as("ts"))
    tableEnvironment.createTemporaryView("dataTable", dataTable)

    val resultSQLTable: Table = tableEnvironment.sqlQuery("select id, count(id) from table(tumble (table dataTable, descriptor(ts), interval '15' second)) group by id, window_start, window_end")

    tableEnvironment.toDataStream(resultSQLTable).print()

    env.execute()

  }
}
```

