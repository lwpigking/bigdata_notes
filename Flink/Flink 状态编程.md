# Flink 状态编程

流式计算分为无状态和有状态两种情况。无状态的计算观察每个独立事件，并根据最后一个事件输出结果。例如，流处理应用程序从传感器接收温度读数，并在温度超过90度时发出警告。有状态的计算则会基于多个事件输出结果。以下是一些例子。

所有类型的窗口。例如，计算过去一小时的平均温度，就是有状态的计算。

所有用于复杂事件处理的状态机。例如，若在一分钟内收到两个相差20度以上的温度读数，则发出警告，这是有状态的计算。

流与流之间的所有关联操作，以及流与静态表或动态表之间的关联操作，都是有状态的计算。

下图展示了无状态流处理和有状态流处理的主要区别。无状态流处理分别接收每条数据记录(图中的黑条)，然后根据最新输入的数据生成输出数据(白条)。有状态流处理会维护状态(根据每条输入记录进行更新)，并基于最新输入的记录和当前的状态值生成输出记录(灰条)。

![无状态和有状态的流处理](img\无状态和有状态的流处理.png)

上图中输入数据由黑条表示。无状态流处理每次只转换一条输入记录，并且仅根据最新的输入记录输出结果(白条)。有状态 流处理维护所有已处理记录的状态值，并根据每条新输入的记录更新状态，因此输出记录(灰条)反映的是综合考虑多个事件之后的结果。

尽管无状态的计算很重要，但是流处理对有状态的计算更感兴趣。事实上，正确地实现有状态的计算比实现无状态的计算难得多。旧的流处理系统并不支持有状态的计算，而新一代的流处理系统则将状态及其正确性视为重中之重。

Flink内置的很多算子，数据源source，数据存储sink都是有状态的，流中的数据都是buffer records，会保存一定的元素或者元数据。例如: ProcessWindowFunction会缓存输入流的数据，ProcessFunction会保存设置的定时器信息等等。

在Flink中，状态始终与特定算子相关联。总的来说，有两种类型的状态：

算子状态（operator state）、键控状态（keyed state）



## operator state

算子状态的作用范围限定为算子任务。这意味着由同一并行任务所处理的所有数据都可以访问到相同的状态，状态对于同一任务而言是共享的。算子状态不能由相同或不同算子的另一个任务访问。

![具有算子状态的任务](img\具有算子状态的任务.png)

Flink为算子状态提供三种基本数据结构：

列表状态（List state）：将状态表示为一组数据的列表。

联合列表状态（Union list state）：也将状态表示为数据的列表。它与常规列表状态的区别在于，在发生故障时，或者从保存点（savepoint）启动应用程序时如何恢复。

广播状态（Broadcast state）：如果一个算子有多项任务，而它的每项任务状态又都相同，那么这种特殊情况最适合应用广播状态。

```scala
package Tutorial.StateProgamming

import Tutorial.DataStreamAPI.{ClickSource, Event}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable.ListBuffer

/**
 * Project:  BigDataCode
 * Create date:  2023/6/30
 * Created by lwPigKing
 */
object BufferingSinkExample {
  def main(args: Array[String]): Unit = {

    /**
     * 接下来我们举一个算子状态的应用案例。在下面的例子中，自定义的 SinkFunction 会在 CheckpointedFunction 中进行数据缓存，然后统一发送到下游。
     * 这个例子演示了列表状态的平均分割重组（event-split redistribution）。
     */

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    env.addSource(new ClickSource)
      .assignAscendingTimestamps(_.timestamp)
      .addSink(new BufferingSink(10))

    env.execute()

  }

  // 实现自定义的SinkFunction
  class BufferingSink(threshold: Int) extends SinkFunction[Event] with CheckpointedFunction {

    // 定义列表状态，保存要缓冲的数据
    var bufferedState: ListState[Event] = _

    // 定义一个本地变量列表
    val bufferedList: ListBuffer[Event] = ListBuffer[Event]()

    override def invoke(value: Event, context: SinkFunction.Context): Unit = {
      // 缓冲数据
      bufferedList += value

      // 判断是否达到了阈值
      if (bufferedList.size == threshold) {
        // 输出到外部系统，用打印到控制台模拟
        bufferedList.foreach(data => println(data))
        println("===========输出完毕============")

        // 清空缓冲
        bufferedList.clear()
      }
    }

    override def snapshotState(functionSnapshotContext: FunctionSnapshotContext): Unit = {
      // 清空状态
      bufferedState.clear()
      for (data <- bufferedList) {
        bufferedState.add(data)
      }
    }

    override def initializeState(context: FunctionInitializationContext): Unit = {
      bufferedState = context.getOperatorStateStore.getListState(new ListStateDescriptor[Event](" buffered-list", classOf[Event]))
      // 判断如果是从故障中恢复，那么就将状态中的数据添加到局部变量中
      if (context.isRestored) {
        import scala.collection.convert.ImplicitConversions._
        for (data <- bufferedState.get()) {
          bufferedList += data
        }
      }
    }
  }

}

```





## keyed state

键控状态是根据输入数据流中定义的键（key）来维护和访问的。Flink为每个键值维护一个状态实例，并将具有相同键的所有数据，都分区到同一个算子任务中，这个任务会维护和处理这个key对应的状态。当任务处理一条数据时，它会自动将状态的访问范围限定为当前数据的key。因此，具有相同key的所有数据都会访问相同的状态。Keyed State很类似于一个分布式的key-value map数据结构，只能用于KeyedStream（keyBy算子处理之后）。

![具有键控状态的任务](img\具有键控状态的任务.png)

Flink的Keyed State支持以下数据类型：

ValueState[T]保存单个的值，值的类型为T。

o  get操作: ValueState.value()

o  set操作: ValueState.update(value: T)

ListState[T]保存一个列表，列表里的元素的数据类型为T。基本操作如下：

o  ListState.add(value: T)

o  ListState.addAll(values: java.util.List[T])

o  ListState.get()返回Iterable[T]

o  ListState.update(values: java.util.List[T])

MapState[K, V]保存Key-Value对。

o  MapState.get(key: K)

o  MapState.put(key: K, value: V)

o  MapState.contains(key: K)

o  MapState.remove(key: K)

ReducingState[T]

AggregatingState[I, O]

State.clear()是清空操作。



### ValueState

```scala
package Tutorial.StateProgamming

import Tutorial.DataStreamAPI.{ClickSource, Event}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector



/**
 * Project:  BigDataCode
 * Create date:  2023/6/30
 * Created by lwPigKing
 */
object PeriodicPVExample {
  def main(args: Array[String]): Unit = {
    /**
     * 1.值状态（ValueState）
     * 我们这里会使用用户id来进行分流，然后分别统计每个用户的pv数据，由于我们并不想每次pv 加一，就将统计结果发送到下游去，所以这里我们注册了一个定时器，
     * 用来隔一段时间发送pv的统计结果，这样对下游算子的压力不至于太大。具体实现方式是定义一个用来保存定时器时间戳的值状态变量。
     * 当定时器触发并向下游发送数据以后，便清空储存定时器时间戳的状态变量，这样当新的数据到来时，发现并没有定时器存在，就可以注册新的定时器了，
     * 注册完定时器之后将定时器的时间戳继续保存在状态变量中。
     */

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    env.addSource(new ClickSource)
      .assignAscendingTimestamps(_.timestamp)
      .keyBy(_.user)
      .process(new PeriodicPvResult)
      .print()

    env.execute()

  }

  class PeriodicPvResult extends KeyedProcessFunction[String, Event, String] {

    // 懒加载值状态变量，用来存储当前pv数据
    lazy val countState: ValueState[Long] = getRuntimeContext.getState(new ValueStateDescriptor[Long]("count", classOf[Long]))

    // 懒加载状态变量，用来存储发送pv数据的定时器的时间戳
    lazy val timerTsState: ValueState[Long] = getRuntimeContext.getState(new ValueStateDescriptor[Long]("timer-ts", classOf[Long]))

    override def processElement(i: Event, context: KeyedProcessFunction[String, Event, String]#Context, collector: Collector[String]): Unit = {
      // 更新count值
      val count: Long = countState.value()
      countState.update(count + 1)

      // 如果保存发送pv数据的定时器的时间戳的状态变量为0L，则注册一个10秒后定时器
      if (timerTsState.value() == 0L) {
        // 注册定时器
        context.timerService().registerEventTimeTimer(i.timestamp + 10 * 1000L)
        // 将定时器对的时间戳保存在状态变量中
        timerTsState.update(i.timestamp + 10 * 1000L)
      }
    }

    override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, Event, String]#OnTimerContext, out: Collector[String]): Unit = {
      out.collect("用户：" + ctx.getCurrentKey + "的PV是：" + countState.value())
      // 清空保存定时器时间戳的状态变量，这样新数据到来时又可以注册定时器了
      timerTsState.clear()
    }


  }

}

```



### ListState

```scala
package Tutorial.StateProgamming

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
 * Project:  BigDataCode
 * Create date:  2023/6/30
 * Created by lwPigKing
 */
object TwoStreamJoinExample {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    /**
     * 2.列表状态（ListState）
     * 在Flink SQL中，支持两条流的全量join，语法如下：
     * SELECT * FROM A INNER JOIN B WHERE A.id = B.id；
     * 这样一条 SQL语句要慎用，因为 Flink会将A流和B流的所有数据都保存下来，然后进行 join。
     * 不过在这里可以用列表状态变量来实现一下这个 SQL 语句的功能。
     */

    val stream1: DataStream[(String, String, Long)] = env.fromElements(
      ("a", "stream-1", 1000L),
      ("b", "stream-1", 2000L)
    )
      .assignAscendingTimestamps(_._3)

    val stream2: DataStream[(String, String, Long)] = env.fromElements(
      ("a", "stream-2", 3000L),
      ("b", "stream-2", 4000L)
    ).assignAscendingTimestamps(_._3)

    // 连接两条流
    stream1.keyBy(_._1)
      .connect(stream2.keyBy(_._1))
      .process(new CoProcessFunction[(String, String, Long), (String, String, Long), String] {

        // 保存来自第一条流的事件的列表状态变量
        lazy val stream1ListState: ListState[(String, String, Long)] = getRuntimeContext.getListState(new ListStateDescriptor[(String, String, Long)]("stream1-list", classOf[(String, String, Long)]))

        // 保存来自第二条流的事件的列表状态变量
        lazy val stream2ListState: ListState[(String, String, Long)] = getRuntimeContext.getListState(new ListStateDescriptor[(String, String, Long)]("stream2-list", classOf[(String, String, Long)]))

        override def processElement1(in1: (String, String, Long), context: CoProcessFunction[(String, String, Long), (String, String, Long), String]#Context, collector: Collector[String]): Unit = {
          // 将事件添加到列表状态变量
          stream1ListState.add(in1)

          import scala.collection.convert.ImplicitConversions._
          for (in2 <- stream2ListState.get) {
            collector.collect(in1 + "=>" + in2)
          }
        }

        override def processElement2(in2: (String, String, Long), context: CoProcessFunction[(String, String, Long), (String, String, Long), String]#Context, collector: Collector[String]): Unit = {
          stream2ListState.add(in2)
          import scala.collection.convert.ImplicitConversions._
          for (in1 <- stream1ListState.get) {
            collector.collect(in1 + "=>" + in2)
          }
        }
      })
      .print()

    env.execute()

  }
}

```



### MapState

```scala
package Tutorial.StateProgamming

import Tutorial.DataStreamAPI.{ClickSource, Event}
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector
import org.apache.spark.sql.execution.streaming.FileStreamSource.Timestamp

/**
 * Project:  BigDataCode
 * Create date:  2023/6/30
 * Created by lwPigKing
 */
object FakeWindowExample {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    /**
     * 3.映射状态（MapState）
     * 映射状态的用法和Java中的HashMap很相似。
     * 在这里可以通过 MapState的使用来探索一下窗口的底层实现，也就是要用映射状态来完整模拟窗口的功能。
     * 这里模拟一个滚动窗口。要计算的是每一个url在每一个窗口中的pv数据。
     * 之前使用增量聚合和全窗口聚合结合的方式实现过这个需求。这里用MapState再来实现一下。
     */

    // 统计每10s滚动窗口内，每个url的pv
    env.addSource(new ClickSource)
      .assignAscendingTimestamps(_.timestamp)
      .keyBy(_.url)
      .process(new FakeWindowResult(10000L))
      .print()

    env.execute()
  }

  class FakeWindowResult(windowSize: Long) extends KeyedProcessFunction[String, Event, String] {

    // 初始化一个MapState状态变量，key为窗口的开窗时间m，value为窗口对应的pv数据
    lazy val windowPvMapState: MapState[Long, Long] = getRuntimeContext.getMapState(new MapStateDescriptor[Long, Long]("window-pv", classOf[Long], classOf[Long]))

    override def processElement(i: Event, context: KeyedProcessFunction[String, Event, String]#Context, collector: Collector[String]): Unit = {

      // 根据事件的时间戳，计算当前事件所属的窗口开始和结束时间
      val windowStart: Long = i.timestamp / windowSize * windowSize
      val windowEnd: Long = windowStart + windowSize

      // 注册一个windowEnd-1ms的定时器，用来触发窗口计算
      context.timerService().registerEventTimeTimer(windowEnd-1)

      // 更新状态中的pv值
      if (windowPvMapState.contains(windowStart)) {
        val pv: Long = windowPvMapState.get(windowStart)
        windowPvMapState.put(windowStart, pv + 1L)
      } else {
        // 如果key不存在，说明当前窗口的第一一个事件到达
        windowPvMapState.put(windowStart, 1L)
      }
    }

    override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, Event, String]#OnTimerContext, out: Collector[String]): Unit = {
      // 计算窗口的结束时间和开始时间
      val windowEnd: Long = timestamp + 1L
      val windowStart: Long = windowEnd - windowSize

      // 发送窗口计算的结果
      out.collect("url：" + ctx.getCurrentKey + "访问量：" + windowPvMapState.get(windowStart) + "窗口：" + windowStart + "~~~~~" + windowEnd)
      // 模拟窗口的销毁，清除map中的key
      windowPvMapState.remove(windowStart)
    }

  }

}

```



### AggregatingState

```scala
package Tutorial.StateProgamming

import Tutorial.DataStreamAPI.{ClickSource, Event}
import org.apache.flink.api.common.functions.{AggregateFunction, RichFlatMapFunction}
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
 * Project:  BigDataCode
 * Create date:  2023/6/30
 * Created by lwPigKing
 */
object AverageTimestampExample {
  def main(args: Array[String]): Unit = {

    /**
     * 4.聚合状态（AggregatingState）
     * 举一个简单的例子，对用户点击事件流每5个数据统计一次平均时间戳。
     * 这是一个类似计数窗口（CountWindow）求平均值的计算，这里可以使用一个有聚合状态的RichFlatMapFunction来实现。
     */

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    env.addSource(new ClickSource)
      .assignAscendingTimestamps(_.timestamp)
      .keyBy(_.user)
      .flatMap(new AvgTsResult)
      .print()

    env.execute()
  }

  class AvgTsResult extends RichFlatMapFunction[Event, String] {

    // 定义聚合状态
     lazy val avgTsAggState: AggregatingState[Event, Long] = getRuntimeContext.getAggregatingState(new AggregatingStateDescriptor[Event, (Long, Long), Long](
      "avg-ts",
      new AggregateFunction[Event, (Long, Long), Long] {
        override def createAccumulator(): (Long, Long) = (0L, 0L)

        override def add(value: Event, accumulator: (Long, Long)): (Long, Long) = {
          (accumulator._1 + value.timestamp, accumulator._2 + 1)
        }

        override def getResult(accumulator: (Long, Long)): Long = accumulator._1 / accumulator._2

        override def merge(a: (Long, Long), b: (Long, Long)): (Long, Long) = ???
      },
      classOf[(Long, Long)]
    ))

    // 定义一个值状态，保存当前已经到达的数据个数
    lazy val countState: ValueState[Long] = getRuntimeContext.getState(new ValueStateDescriptor[Long]("count", classOf[Long]))

    override def flatMap(value: Event, out: Collector[String]): Unit = {
      avgTsAggState.add(value)
      // 更新count值
      val count: Long = countState.value()
      countState.update(count + 1)

      // 判断是否达到了技术窗口的长度，输出结果
      if (countState.value() == 5) {
        out.collect(s"${value.user}的平均时间戳为${avgTsAggState.get()}")
        // 窗口销毁
        countState.clear()
      }
    }

  }

}
```



## 状态持久化和状态后端

在 Flink 的状态管理机制中，很重要的一个功能就是对状态进行持久化（persistence）保存，这样就可以在发生故障后进行重启恢复。Flink 对状态进行持久化的方式，就是将当前所有分布式状态进行“快照”保存，写入一个“检查点”（checkpoint）或者保存点（savepoint）保存到外部存储系统中。具体的存储介质，一般是分布式文件系统（distributed file system）。 

### checkpoint

有状态流应用中的检查点（checkpoint），其实就是所有任务的状态在某个时间点的一个快照（一份拷贝）。简单来讲，就是一次“存盘”，让我们之前处理数据的进度不要丢掉。在一个流应用程序运行时，Flink 会定期保存检查点，在检查点中会记录每个算子的 id 和状态；如果发生故障，Flink 就会用最近一次成功保存的检查点来恢复应用的状态，重新启动处理流程，就如同“读档”一样。 

如果保存检查点之后又处理了一些数据，然后发生了故障，那么重启恢复状态之后这些数据带来的状态改变会丢失。为了让最终处理结果正确，我们还需要让源（Source）算子重新读取这些数据，再次处理一遍。这就需要流的数据源具有“数据重放”的能力，一个典型的例子就是 Kafka，我们可以通过保存消费数据的偏移量、故障重启后重新提交来实现数据的重放。这是对“至少一次”（at least once）状态一致性的保证，如果希望实现“精确一次”（exactly once）的一致性，还需要数据写入外部系统时的相关保证。默认情况下，检查点是被禁用的，需要在代码中手动开启。直接调用执行环境的.enableCheckpointing()方法就可以开启检查点。 

除了检查点之外，Flink 还提供了“保存点”（savepoint）的功能。保存点在原理和形式上跟检查点完全一样，也是状态持久化保存的一个快照；区别在于，保存点是自定义的镜像保存，所以不会由 Flink 自动创建，而需要用户手动触发。这在有计划地停止、重启应用时非常有用。 

### State Backends

检查点的保存离不开 JobManager 和 TaskManager，以及外部存储系统的协调。在应用进行检查点保存时，首先会由 JobManager 向所有 TaskManager 发出触发检查点的命令； TaskManger 收到之后，将当前任务的所有状态进行快照保存，持久化到远程的存储介质中；完成之后向 JobManager 返回确认信息。这个过程是分布式的，当 JobManger 收到所有 TaskManager 的返回信息后，就会确认当前检查点成功保存，而这一切工作的协调，就需要一个“专职人员”来完成。

在 Flink 中，状态的存储、访问以及维护，都是由一个可插拔的组件决定的，这个组件就叫作状态后端（state backend）。状态后端主要负责两件事：一是本地的状态管理，二是将检查点（checkpoint）写入远程的持久化存储。 

