# DataStream API

Flink流处理过程如下：

![流处理过程](img\流处理过程.png)

## Environment

### getExecutionEnvironment

创建一个执行环境，表示当前执行程序的上下文。如果程序是独立调用的，则此方法返回本地执行环境；如果从命令行客户端调用程序以提交到集群，则此方法返回此集群的执行环境，也就是说，getExecutionEnvironment会根`据查询运行的方式决定返回什么样的运行环境`，是`最常用`的一种创建执行环境的方式。

```scala
val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
env.setParallelism(1)
```

如果没有设置并行度，会以flink-conf.yaml中的配置为准，默认是1。

### createLocalEnvironment

`返回本地执行环境`，需要在调用时指定默认的并行度。

```scala
val env = StreamExecutionEnvironment.createLocalEnvironment(1)
```

### createRemoteEnvironment

`返回集群执行环境`，将Jar提交到远程服务器。需要在调用时指定JobManager的IP和端口号，并指定要在集群中运行的Jar包。

```scala
val env = ExecutionEnvironment.createRemoteEnvironment("jobmanage-hostname", 6123,"YOURPATH//wordcount.jar")
```



## Source

### 从集合读取数据

```scala
case class SensorReading(id: String, timestamp: Long, temperature: Double)

object Source {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    
    env.setParallelism(1)
    // 从集合中读取数据
    val stream1: DataStream[SensorReading] = env.fromCollection(List(
      SensorReading("sensor_1", 1547718199, 35.8),
      SensorReading("sensor_6", 1547718201, 15.4),
      SensorReading("sensor_7", 1547718202, 6.7),
      SensorReading("sensor_10", 1547718205, 38.1)
    ))
    stream1.print("stream1:")

    env.execute()
  }
}
```

### 从文件读取数据

```scala
val stream2 = env.readTextFile("YOUR_FILE_PATH")
```

### 以Kafka消息队列的数据作为来源

```scala
object Source {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    
    // 集成Kafka（从kafka中读取数据）
    val properties: Properties = new Properties()
    properties.put("bootstrap.servers", "master:9092")
      
    val kafkaDS: DataStream[String] = env.addSource(new FlinkKafkaConsumer[String](
      "lwPigKing",
      new SimpleStringSchema(),
      properties
    ))

    env.execute()
  }
}
```

### 自定义Source

继承SourceFunction

```scala
package Tutorial.DataStreamAPI

import org.apache.flink.streaming.api.functions.source.SourceFunction

import java.util.Calendar
import scala.util.Random

/**
 * Copyright (c) 2020-2030 尚硅谷 All Rights Reserved
 *
 * Project:  FlinkTutorial
 *
 * Created by  wushengran
 */
case class Event(user: String, url: String, timestamp: Long)

class ClickSource extends SourceFunction[Event]{
  // 标志位
  var running = true

  override def run(ctx: SourceFunction.SourceContext[Event]): Unit = {
    // 随机数生成器
    val random = new Random()
    // 定义数据随机选择的范围
    val users = Array("Mary", "Alice", "Bob", "Cary")
    val urls = Array("./home", "./cart", "./fav", "./prod?id=1", "./prod?id=2", "./prod?id=3")

    // 用标志位作为循环判断条件，不停地发出数据
    while (running){
      val event = Event(users(random.nextInt(users.length)), urls(random.nextInt(urls.length)), Calendar.getInstance.getTimeInMillis)
//      // 为要发送的数据分配时间戳
//      ctx.collectWithTimestamp(event, event.timestamp)
//
//      // 向下游直接发送水位线
//      ctx.emitWatermark(new Watermark(event.timestamp - 1L))

      // 调用ctx的方法向下游发送数据
      ctx.collect(event)

      // 每隔1s发送一条数据
      Thread.sleep(1000)
    }
  }

  override def cancel(): Unit = running = false
}

```

```scala
package Tutorial.DataStreamAPI

import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.collection.immutable
import scala.util.Random
import scala.util.Random.nextGaussian

/**
 * Project:  BigDataCode
 * Create date:  2023/6/24
 * Created by fujiahao
 */

/**
 * 自定义Source
 * 需要继承SourceFunction
 */

class MySensorSource extends SourceFunction[SensorReading] {

  // flag:表示数据源是否还在正常运行
  var running: Boolean = true

  override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit = {

    // 初始化一个随机数发生器
    val random: Random = new Random()

    var curTemp: immutable.IndexedSeq[(String, Double)] = 1.to(10).map(
      i => ("sensor_" + i, 65 + random.nextGaussian() * 20)
    )

    while (running) {
      // 更新温度值
      curTemp = curTemp.map(
        t => (t._1, t._2 + random.nextGaussian() )
      )

      // 获取时间戳
      val curTime: Long = System.currentTimeMillis()

      curTemp.foreach(
        t => sourceContext.collect(SensorReading(t._1, curTime, t._2))
      )

      Thread.sleep(100)
    }
  }

  override def cancel(): Unit = {
    running = false
  }
}
```



## Transform

### map

```scala
val stream: DataStream[Int] = env.fromElements(1, 2, 3, 4)
val streamMap: DataStream[Int] = stream.map(_ * 2)
streamMap.print()
```

### flatMap

```scala
val stream: DataStream[String] = env.fromCollection(
    List("Hello Hadoop", "Hello Flink", "Hello Spark")
)
val streamFlatMap: DataStream[String] = stream.flatMap(_.split(" "))
streamFlatMap.print()
```

### filter

```scala
val stream: DataStream[Int] = env.fromElements(1, 2, 3, 4)
val streamFilter: DataStream[Int] = stream.filter(_ == 1)
streamFilter.print()
```

### keyBy

DataStream -> KeyedStream(逻辑地讲一个流拆分成不相交的分区，每个分区`包含具有相同key元素`)

```scala
val stream: DataStream[String] = env.fromElements(
    "Hello Spark", 
    "Hello Hadoop", 
    "Hello Flink"
)
val streamFlatMap: DataStream[String] = stream.flatMap(_.split(" "))
val streamMap: DataStream[(String, Int)] = streamFlatMap.map(word => (word, 1))
val value: KeyedStream[(String, Int), String] = streamMap.keyBy(_._1)
value.print()
```

### Rolling Aggregation

滚动聚合算子，这些算子可以`针对KeyedStream的每一个支流做聚合`

sum()、min()、max()、minBy()、maxBy()

```scala
val stream: DataStream[String] = env.fromElements(
    "Hello Spark",
    "Hello Hadoop",
    "Hello Flink"
)
val streamFlatMap: DataStream[String] = stream.flatMap(_.split(" "))
val streamMap: DataStream[(String, Int)] = streamFlatMap.map(word => (word, 1))
val value: DataStream[(String, Int)] = streamMap.keyBy(_._1).sum(1)
value.print()
```

### Reduce

`KeyedStream → DataStream`：一个分组数据流的聚合操作，合并当前的元素和上次聚合的结果，产生一个新的值，返回的流中包含每一次聚合的结果，而不是只返回最后一次聚合的最终结果。

```scala
val stream2 = env.readTextFile("YOUR_PATH\\sensor.txt")
  .map( data => {
    val dataArray = data.split(",")
    SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
  })
  .keyBy("id")
  .reduce( (x, y) => SensorReading(x.id, x.timestamp + 1, y.temperature) )
```

### Connect和CoMap

![Connect算子](img\Connect算子.png)

`Connect`：`DataStream,DataStream → ConnectedStreams`：连接两个保持他们类型的数据流，两个数据流被Connect之后，只是被放在了一个同一个流中，内部依然保持各自的数据和形式不发生任何变化，两个流相互独立。



![CoMap](img\CoMap.png)

`CoMap`：`ConnectedStreams → DataStream`：作用于ConnectedStreams上，功能与map和flatMap一样，对ConnectedStreams中的每一个Stream分别进行map和flatMap处理。

`Connect如果和CoMap一起使用，得继承CoMapFunction。`

```scala
val intStream: DataStream[Int] = env.fromElements(1, 2, 3, 4, 5)
val stringStream: DataStream[String] = env.fromElements("A", "B", "C", "D", "E")
val connectedStream: ConnectedStreams[Int, String] = intStream.connect(stringStream)
val result: DataStream[String] = connectedStream.map(new CoMapFunction[Int, String, String] {
    override def map1(in1: Int): String = "Number: " + in1.toString
	override def map2(in2: String): String = "String: " + in2.toLowerCase
})
result.print()
```

### Union

![Union](img\Union.png)

合流操作DataStream -> DataStream：对两个或者两个以上的DataStream进行Union操作
产生一个包含所有DataStream元素的新DataStream。

```scala
val intStream: DataStream[Int] = env.fromElements(1, 2, 3, 4, 5)
val intStream2: DataStream[Int] = env.fromElements(6, 7, 8, 9, 10)
val value: DataStream[Int] = intStream.union(intStream2)
value.print()
```

### Connect和Union的区别

`Union之前两个流的类型必须一样`；`Connect可以不一样，在之后的CoMap中再去调整`
`Connect只能操作两个流；Union可以操作多个`



## 支持的数据类型

Flink流应用程序处理的是以数据对象表示的事件流。所以在Flink内部，我们需要能够处理这些对象。它们需要被序列化和反序列化，以便通过网络传送它们；或者从状态后端、检查点和保存点读取它们。为了有效地做到这一点，Flink需要明确知道应用程序所处理的数据类型。Flink使用类型信息的概念来表示数据类型，并为每个数据类型生成特定的序列化器、反序列化器和比较器。

Flink还具有一个类型提取系统，该系统分析函数的输入和返回类型，以自动获取类型信息，从而获得序列化器和反序列化器。但是，在某些情况下，例如lambda函数或泛型类型，需要显式地提供类型信息，才能使应用程序正常工作或提高其性能。

Flink支持Java和Scala中所有常见数据类型。使用最广泛的类型有以下几种。

### 基础数据类型

Flink支持所有的Java和Scala基础数据类型，Int, Double, Long, String..

```scala
val numbers: DataStream[Long] = env.fromElements(1L, 2L, 3L, 4L)
val value: DataStream[Long] = numbers.map(_ + 1)
value.print()
```

### Java和Scalau元组

```scala
val persons: DataStream[(String, Int)] = env.fromElements(
    ("Adam", 17),
    ("Sarah", 23)
)
val value: DataStream[(String, Int)] = persons.filter(_._2 > 18)
value.print()
```

### Scala样例类

```scala
case class Person(name: String, age: Int)
val persons: DataStream[Person] = env.fromElements(
    Person("Adam", 17),
    Person("Sarah", 23)
)
val value: DataStream[Person] = persons.filter(_.age > 18)
value.print()
```

### Java简单对象

```java
public class Person {
    public String name;
    public int age;
    public Person() {}
    public Person(String name, int age) { 
        this.name = name;      
        this.age = age;  
    }
}
```

```scala
val person: DataStream[Person] = env.fromElements(
    new Person("Alex", 42),
    new Person("Wendy", 23)
)
person.print()
```

### 	其他

Flink对Java和Scala中的一些特殊目的的类型也都是支持的，比如Java的ArrayList，HashMap，Enum等等。



## 实现UDF函数

### 函数类

Flink暴露了所有udf函数的接口(`实现方式为接口或者抽象类`)。例如MapFunction, FilterFunction, ProcessFunction等等。

```scala
val word: DataStream[String] = env.fromElements(
    "hello spark", 
    "hello hadoop", 
    "hello flink"
)
val wordFilter: DataStream[String] = word.filter(new myFilter)
wordFilter.print()


class myFilter extends FilterFunction[String] {
    override def filter(value: String): Boolean = value.contains("flink")
}
```

还可以通过实现匿名类的方法

```scala
val word: DataStream[String] = env.fromElements("hello spark", "hello hadoop", "hello flink")
val value1: DataStream[String] = word.filter(new FilterFunction[String] {
    override def filter(value: String): Boolean = value.contains("hadoop")
})
value1.print()
```

还可以直接当做参数传入

```scala
val word: DataStream[String] = env.fromElements(
    "hello spark", 
    "hello hadoop",
    "hello flink"
)
val value: DataStream[String] = word.filter(new keywordFilter("flink"))
value.print()


class keywordFilter(keyword: String) extends FilterFunction[String] {
    override def filter(value: String): Boolean = value.contains(keyword)
}
```

### 匿名函数

```scala
val word: DataStream[String] = env.fromElements(
    "hello spark", 
    "hello hadoop", 
    "hello flink"
)
word.filter(_.contains("flink")).print()
```

### 富函数

富函数是DataStream API提供的一个函数类的接口，所有Flink函数类都有其Rich版本。它与常规函数的不同在于，可以获取运行环境的上下文，并拥有一些生命周期方法，所以可以实现更复杂的功能。

Rich Function有一个生命周期的概念。典型的生命周期方法有：

open()方法是rich function的初始化方法，当一个算子例如map或者filter被调用之前open()会被调用。

close()方法是生命周期中的最后一个调用的方法，做一些清理工作。

getRuntimeContext()方法提供了函数的RuntimeContext的一些信息，例如函数执行的并行度，任务的名字，以及state状态。

```scala
class MyFlatMap extends RichFlatMapFunction[Int, (Int, Int)] {
    var subTaskIndex = 0
    
    override def open(parameters: Configuration): Unit = {
        subTaskIndex = getRuntimeContext.getIndexOfThisSubtask
    }

    override def flatMap(value: Int, out: Collector[(Int, Int)]): Unit = {
      if (value % 2 == subTaskIndex) {
        out.collect((subTaskIndex, value))
      }
    }

    override def close(): Unit = {

    }
    
}
```



## Sink

Flink没有类似于spark中foreach方法，让用户进行迭代的操作。虽有对外的输出操作都要利用Sink完成。最后通过类似如下方式完成整个任务最终输出操作。

```scala
stream.addSink(new MySink(xxxx)) 
```

### KafkaSink

```scala
val stream: DataStream[Int] = env.fromElements(1, 2, 3, 4)
val Res: DataStream[String] = stream.map(_.toString)
Res.addSink(new FlinkKafkaProducer[String](
    "master:9092",
    "lwPigKing",
    new SimpleStringSchema()
))
```

### RedisSink

```scala
val SensorDS: DataStream[SensorReading] = env.fromElements(
    SensorReading("1001", 32321423, 56.0),
    SensorReading("1002", 32321423, 56.0),
    SensorReading("1003", 32321423, 56.0)
)

val conf: FlinkJedisPoolConfig = new FlinkJedisPoolConfig.Builder().setHost("master").setPort(6379).build()

SensorDS.addSink(new RedisSink[SensorReading](
    conf,
    new MyRedisMapper
))


class MyRedisMapper extends RedisMapper[SensorReading] {
    override def getCommandDescription: RedisCommandDescription = {
        new RedisCommandDescription(RedisCommand.SET)
    }

    override def getKeyFromData(t: SensorReading): String = {
      t.temperature.toString
    }

    override def getValueFromData(t: SensorReading): String = {
      t.id
    }
}
```

### MySQLSink

```scala
SensorDS.addSink(new MyJdbcSink)

class MyJdbcSink extends RichSinkFunction[SensorReading]{
  var conn: Connection = _
  var insertStmt: PreparedStatement = _
  var updateStmt: PreparedStatement = _

  // open主要是创建连接
  override def open(parameters: Configuration): Unit = {
    conn = DriverManager.getConnection("jdbc:mysql://master:3306/lwPigKing", "root", "123456")
  }

  // 调用连接，执行sql
  override def invoke(value: SensorReading, context: SinkFunction.Context): Unit = {
    insertStmt = conn.prepareStatement("INSERT INTO temperatures (sensor, temp) VALUES (?, ?)")
    updateStmt = conn.prepareStatement("UPDATE temperatures SET temp = ? WHERE sensor = ?")
    updateStmt.setDouble(1, value.temperature)
    updateStmt.setString(2, value.id)
    updateStmt.execute()

    if (updateStmt.getUpdateCount == 0){
      insertStmt.setString(1, value.id)
      insertStmt.setDouble(2, value.temperature)
      insertStmt.execute()
    }
  }

  // 关闭连接
  override def close(): Unit = {
    insertStmt.close()
    updateStmt.close()
    conn.close()
  }
}
```

### ClickHouseSink

```scala
class ClickHouseSink(url: String, username: String, password: String) extends RichSinkFunction[(String, Int)]{
  private var connection: ClickHouseConnection = _

  private val properties: Properties = new Properties()
  properties.put("username", username)
  properties.put("password", password)

  override def open(parameters: Configuration): Unit = {
    val dataSource: ClickHouseDataSource = new ClickHouseDataSource(url, properties)
    connection = dataSource.getConnection
  }

  override def invoke(value: (String, Int), context: SinkFunction.Context): Unit = {

    val statement: PreparedStatement = connection.prepareStatement(
      "insert into station_flow (station, flow) values (?, ?)"
    )

    statement.setString(1, value._1)
    statement.setInt(2, value._2)
    statement.executeUpdate()
    statement.close()
  }

  override def close(): Unit = {
    if (connection != null) {
      connection.close()
    }
  }

}
```

### HBaseSink

```scala
class StationHBaseSink extends RichSinkFunction[(String, String, Long)]{
  var conn: Connection = null

  override def open(parameters: Configuration): Unit = {
    val config: conf.Configuration = HBaseConfiguration.create
    config.set(HConstants.ZOOKEEPER_QUORUM,"master,slave1,slave2")
    config.set(HConstants.ZOOKEEPER_CLIENT_PORT,"2181")
    conn = ConnectionFactory.createConnection(config)
  }

  override def invoke(value: (String, String, Long), context: SinkFunction.Context): Unit = {
    val tableName: String = "station_flow"
    val rowKey: String = value._1
    val station: String = value._2
    val flow: Long = value._3
    val table: Table = conn.getTable(TableName.valueOf(tableName))
    val put: Put = new Put(Bytes.toBytes(rowKey))
    put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("station"), Bytes.toBytes(station))
    put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("flow"), Bytes.toBytes(flow.toString))
    table.put(put)
  }

  override def close(): Unit = {
    if (conn != null) {
      conn.close()
    }
  }
}
```

