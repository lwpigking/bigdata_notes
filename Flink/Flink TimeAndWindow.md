# Flink TimeAndWindow

## Flink中的时间语义

在Flink的流式处理中，会涉及到时间的不同概念，如下图所示：

![Flink时间概念](img\Flink时间概念.png)

Event Time：是事件创建的时间。它通常由事件中的时间戳描述，例如采集的日志数据中，每一条日志都会记录自己的生成时间，Flink通过时间戳分配器访问事件时间戳。

Ingestion Time：是数据进入Flink的时间。

Processing Time：是每一个执行基于时间操作的算子的本地系统时间，与机器相关，默认的时间属性就是Processing Time。



## 水位线Watermark

### 基本概念

我们知道，流处理从事件产生，到流经source，再到operator，中间是有一个过程和时间的，虽然大部分情况下，流到operator的数据都是按照事件产生的时间顺序来的，但是也不排除由于网络、分布式等原因，导致乱序的产生，所谓乱序，就是指Flink接收到的事件的先后顺序不是严格按照事件的Event Time顺序排列的。

![数据的乱序](img\数据的乱序.png)

那么此时出现一个问题，一旦出现乱序，如果只根据eventTime决定window的运行，我们不能明确数据是否全部到位，但又不能无限期的等下去，此时必须要有个机制来保证一个特定的时间后，必须触发window去进行计算了，这个特别的机制，就是Watermark。

Watermark是一种衡量Event Time进展的机制。

Watermark是用于处理乱序事件的，而正确的处理乱序事件，通常用Watermark机制结合window来实现。

数据流中的Watermark用于表示timestamp小于Watermark的数据，都已经到达了，因此，window的执行也是由Watermark触发的。

Watermark可以理解成一个延迟触发机制，我们可以设置Watermark的延时时长t，每次系统会校验已经到达的数据中最大的maxEventTime，然后认定eventTime小于maxEventTime - t的所有数据都已经到达，如果有窗口的停止时间等于maxEventTime – t，那么这个窗口被触发执行。

有序流的Watermarker如下图所示：（Watermark设置为0）

![有序数据的Watermark](img\有序数据的Watermark.png)

乱序流的Watermarker如下图所示：（Watermark设置为2）

![无序数据的Watermark](img\无序数据的Watermark.png)

当Flink接收到数据时，会按照一定的规则去生成Watermark，这条Watermark就等于当前所有到达数据中的maxEventTime - 延迟时长，也就是说，Watermark是基于数据携带的时间戳生成的，一旦Watermark比当前未触发的窗口的停止时间要晚，那么就会触发相应窗口的执行。由于event time是由数据携带的，因此，如果运行过程中无法获取新的数据，那么没有被触发的窗口将永远都不被触发。

上图中，我们设置的允许最大延迟到达时间为2s，所以时间戳为7s的事件对应的Watermark是5s，时间戳为12s的事件的Watermark是10s，如果我们的窗口1是1s~5s，窗口2是6s~10s，那么时间戳为7s的事件到达时的Watermarker恰好触发窗口1，时间戳为12s的事件到达时的Watermark恰好触发窗口2。

Watermark 就是触发前一窗口的“关窗时间”，一旦触发关门那么以当前时刻为准在窗口范围内的所有所有数据都会收入窗中。

只要没有达到水位那么不管现实中的时间推进了多久都不会触发关窗。

### 水位线生成策略

水位线生成策略（Watermark Strategies）
在Flink的DataStreamAPI中，有一个单独用于生成水位线的方法：
assignTimestampsAndWatermarks()，它主要用来为流中的数据分配时间戳，并且生成水位线来指示事件时间。具体使用时，直接用DataStream调用该方法即可。

assignTimestampsAndWatermarks()方法需要传入一个水位线生成策略（WatermarkStrategy）作为参数。WatermarkStrategy中包含了一个时间戳分配器和一个水位线生成器。Flink提供了内置的水位线生成器（WatermarkTGenerator），不仅开箱即用简化了编程，而且也为我们自定义水位线策略提供了模板。这两个生成器可以通过调用WatermarkStrategy的静态辅助方法来创建。它们都是周期性生成水位线，分别对应着处理有序流和乱序流的场景。

#### 有序流

对于有序流，主要特点就是时间戳单调增长，所以永远不会出现迟到数据的问题。这是周期性生成水位线的最简单的场景，直接调用WatermarkStrategy.forMonotonousTimestamps()方法即可。简单来说，就是直接拿当前最大的时间戳作为水位线就可以了。

```scala
val stream: DataStreamSource[Event] = env.addSource(new ClickSource)
stream.assignTimestampsAndWatermarks(
    WatermarkStrategy.forMonotonousTimestamps[Event]()
    .withTimestampAssigner(new SerializableTimestampAssigner[Event] {
        override def extractTimestamp(element: Event, recordTimestamp: Long): Long = element.timestamp
    })
)
```

#### 无序流

乱序流由于乱序流中需要等待迟到数据到齐，所以必须设置一个固定量的延迟时间（Fixed Amount of Late）。这时生成水位线的时间戳，就是当前数据流中最大的时间戳减去延迟的结果，相当于把表调慢，当前时钟会滞后于数据的最大时间戳。调用WatermarkStrategy.forBoundedOutOfOrderness()方法就可以实现。这个方法需要传入一个maxOutOfOrderness参数，表示最大乱序程度，它表示数据流中乱序数据时间戳的最大差值；如果能确定乱序程度，那么设置对应时间长度的延迟，就可以等到所有的乱序数据了。

```scala
stream.assignTimestampsAndWatermarks(
    WatermarkStrategy.forBoundedOutOfOrderness[Event](Duration.ofSeconds(5))
    .withTimestampAssigner(new SerializableTimestampAssigner[Event] {
        override def extractTimestamp(element: Event, recordTimestamp: Long): Long = element.timestamp
    })
)
.print()
```



## 窗口Window API

### Window概述

streaming流式计算是一种被设计用于处理无限数据集的数据处理引擎，而无限数据集是指一种不断增长的本质上无限的数据集，而window是一种切割无限数据为有限块进行处理的手段。

Window是无限数据流处理的核心，Window将一个无限的stream拆分成有限大小的”buckets”桶，我们可以在这些桶上做计算操作。

### Window类型

Window可以分成两类：

CountWindow：按照指定的数据条数生成一个Window，与时间无关。

TimeWindow：按照时间生成Window。

对于TimeWindow，可以根据窗口实现原理的不同分成三类：滚动窗口（Tumbling Window）、滑动窗口（Sliding Window）和会话窗口（Session Window）。

滚动窗口（Tumbling Windows）将数据依据固定的窗口长度对数据进行切片。

特点：时间对齐，窗口长度固定，没有重叠。

适用场景：适合做BI统计等（做每个时间段的聚合计算）。

滚动窗口分配器将每个元素分配到一个指定窗口大小的窗口中，滚动窗口有一个固定的大小，并且不会出现重叠。例如：如果你指定了一个5分钟大小的滚动窗口，窗口的创建如下图所示：

![滚动窗口](img\滚动窗口.png)



滑动窗口（Sliding Windows）

滑动窗口是固定窗口的更广义的一种形式，滑动窗口由固定的窗口长度和滑动间隔组成。

特点：时间对齐，窗口长度固定，可以有重叠。

适用场景：对最近一个时间段内的统计（求某接口最近5min的失败率来决定是否要报警）。

滑动窗口分配器将元素分配到固定长度的窗口中，与滚动窗口类似，窗口的大小由窗口大小参数来配置，另一个窗口滑动参数控制滑动窗口开始的频率。因此，滑动窗口如果滑动参数小于窗口大小的话，窗口是可以重叠的，在这种情况下元素会被分配到多个窗口中。

例如，你有10分钟的窗口和5分钟的滑动，那么每个窗口中5分钟的窗口里包含着上个10分钟产生的数据，如下图所示：

![滑动窗口](img\滑动窗口.png)



会话窗口（Session Windows）

由一系列事件组合一个指定时间长度的timeout间隙组成，类似于web应用的session，也就是一段时间没有接收到新数据就会生成新的窗口。

特点：时间无对齐。

session窗口分配器通过session动来对元素进行分组，session窗口跟滚动窗口和滑动窗口相比，不会有重叠和固定的开始时间和结束时间的情况，相反，当它在一个固定的时间周期内不再收到元素，即非活动间隔产生，那个这个窗口就会关闭。一个session窗口通过一个session间隔来配置，这个session间隔定义了非活跃周期的长度，当这个非活跃周期产生，那么当前的session将关闭并且后续的元素将被分配到新的session窗口中去。

![会话窗口](img\会话窗口.png)



### 窗口分配器

#### TimeWindow

TimeWindow是将指定时间范围内的所有数据组成一个window，一次对一个window里面的所有数据进行计算。

##### 滚动窗口

```scala
dataStream.map(r => (r.id, r.temperature))
	.keyBy(_._1)
    .window(TumblingProcessingTimeWindows.of(Time.seconds(1)))
    .reduce((r1, r2) => (r1._1, r1._2.min(r2._2)))
```

##### 滑动窗口

```scala
dataStream.map(r => (r.id, r.temperature))
	.keyBy(_._1)
	.window(SlidingProcessingTimeWindows.of(Time.seconds(15), Time.seconds(5)))
```

##### 会话窗口

```scala
dataStream.map(r => (r.id, r.temperature))
	.keyBy(_._1)
	.window(ProcessingTimeSessionWindows.withGap(Time.seconds(10)))
```



#### CountWindow

CountWindow根据窗口中相同key元素的数量来触发执行，执行时只计算元素数量达到窗口大小的key对应的结果。
注意：CountWindow的window_size指的是相同key的元素的个数，不是输入的所有元素的总数。

##### 滚动计数窗口

```scala
// 滚动计数窗口：只需传入参数size（表示窗口大小）
// 定义一个长度为5的滚动计数窗口，当窗口中元素数量达到5时，就会触发计算执行并关闭窗口
dataStream.map(r => (r.id, r.temperature))
	.keyBy(_._1)
    .countWindow(5)
    .reduce((r1, r2) => (r1._1, r1._2.max(r2._2)))
```

##### 滑动计数窗口

```scala
// 滑动计数窗口：需传入参数size和slide
// 前者表示窗口大小，后者表示滑动步长
// 定义一个长度为10、滑动步长为3的滑动计数窗口。
// 每个窗口统计10个数据，每隔3个数据就统计输出一次结果。
dataStream.map(r => (r.id, r.temperature))
	.keyBy(_._1)
    .countWindow(10, 3)
```



### 窗口函数

定义了窗口分配器，我们只是知道了数据属于哪个窗口，可以将数据收集起来了；至于收集起来到底要做什么，其实还完全没有头绪。所以在窗口分配器之后，必须再接上一个定义窗口如何进行计算的操作，这就是所谓的“窗口函数”（window functions）。 

经窗口分配器处理之后，数据可以分配到对应的窗口中，而数据流经过转换得到的数据类型是 WindowedStream。这个类型并不是 DataStream，所以并不能直接进行其他转换，而必须进一步调用窗口函数，对收集到的数据进行处理计算之后，才能最终再次得到 DataStream。

窗口函数定义了要对窗口中收集的数据做的计算操作，根据处理的方式可以分为两类：增量聚合函数和全窗口函数。



#### 增量聚合函数

为了提高实时性，我们可以像 DataStream 的简单聚合一样，每来一条数据就立即进行计算，中间只要保持一个简单的聚合状态就可以了；区别只是在于不立即输出结果，而是要等到窗口结束时间。等到窗口到了结束时间需要输出计算结果的时候，我们只需要拿出之前聚合的状态直接输出，这无疑就大大提高了程序运行的效率和实时性。 

典型的增量聚合函数有两个：ReduceFunction 和 AggregateFunction。 



##### ReduceFunction

只要基于 WindowedStream 调用.reduce()方法，然后传入 ReduceFunction 作为参数，就可以指定以归约两个元素的方式去对窗口中数据进行聚合了。

```scala
env.addSource(new ClickSource)     
	.assignAscendingTimestamps(_.timestamp)
	.map(r => (r.user, 1L))
	// 使用用户名对数据流进行分组
	.keyBy(_._1)
	// 设置5秒钟的滚动事件窗口
	.window(TumblingEventTimeWindows.of(Time.seconds(5)))
	// 保留第一个字段，针对第二个字段进行聚合
	.reduce((r1, r2) => (r1._1, r1._2 + r2._2))
	.print()
```



##### AggregateFunction

ReduceFunction 可以解决大多数归约聚合的问题，但是这个接口有一个限制，就是聚合状态的类型、输出结果的类型都必须和输入数据类型一样。为了更加灵活地处理窗口计算，Flink的Window API提供了更加一般化的aggregate()方法。直接基于 WindowedStream 调用 aggregate()方法，就可以定义更加灵活的窗口聚合操作。这个方法需要传入一个 AggregateFunction 的实现类作为参数。

AggregateFunction 可以看作是 ReduceFunction 的通用版本，这里有三种类型：输入类型（IN）、累加器类型（ACC）和输出类型（OUT）。输入类型 IN 就是输入流中元素的数据类型；累加器类型 ACC 则是我们进行聚合的中间状态类型；而输出类型当然就是最终计算结果的类型了。 

AggregateFunction 接口中有四个方法： 

createAccumulator()：创建一个累加器，这就是为聚合创建了一个初始状态，每个聚合任务只会调用一次。 

add()：将输入的元素添加到累加器中。这就是基于聚合状态，对新来的数据进行进一步聚合的过程。方法传入两个参数：当前新到的数据 value，和当前的累加器 accumulator；返回一个新的累加器值，也就是对聚合状态进行更新。每条数据到来之后都会调用这个方法。 

getResult()：从累加器中提取聚合的输出结果。也就是说，我们可以定义多个状态，然后再基于这些聚合的状态计算出一个结果进行输出。比如之前我们提到的计算平均值，就可以把 sum 和 count 作为状态放入累加器，而在调用这个方法时相除得到最终结果。这个方法只在窗口要输出结果时调用。 

merge()：合并两个累加器，并将合并后的状态作为一个累加器返回。这个方法只在需要合并窗口的场景下才会被调用；最常见的合并窗口（Merging Window）的场景就是会话窗口（Session Windows）。 

```scala
env.addSource(new ClickSource)
	.assignAscendingTimestamps(_.timestamp)
	// 通过为每条数据分配同样的key，来将数据发送到同一个分区
	.keyBy(_ => "key")
	.window(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(2)))
	.aggregate(new AvgPv)
	.print()


class AvgPv extends AggregateFunction[Event, (Set[String], Double), Double] {

    // 创建空累加器，类型是元组，元组的第一个元素类型为Set（用来对用户名进行去重）
    // 第二个元素用来累加PV操作（每来一条数据就加一）
    override def createAccumulator(): (Set[String], Double) = (Set[String](), 0L)

    // 累加规则
    override def add(value: Event, accumulator: (Set[String], Double)): (Set[String], Double) = {
      (accumulator._1 + value.user, accumulator._2 + 1L)
    }

    // 获取窗口关闭时向下游发送的结果
    // PV / UV
    override def getResult(accumulator: (Set[String], Double)): Double = {
      accumulator._2 / accumulator._1.size
    }

    // merge方法只有在事件时间的会话窗口时，才需要实现，这里无需实现
    override def merge(a: (Set[String], Double), b: (Set[String], Double)): (Set[String], Double) = ???
}
```





#### 全窗口函数

窗口操作中的另一大类就是全窗口函数。与增量聚合函数不同，全窗口函数需要先收集窗口中的数据，并在内部缓存起来，等到窗口要输出结果的时候再取出数据进行计算。在 Flink 中，全窗口函数也有两种：WindowFunction 和 ProcessWindowFunction。 



##### WindowFunction

WindowFunction 字面上就是“窗口函数”，它其实是老版本的通用窗口函数接口。我们可以基于 WindowedStream 调用.apply()方法，传入一个 WindowFunction 的实现类。（基本不用）



##### ProcessWindowFunction

ProcessWindowFunction 是 Window API 中最底层的通用窗口函数接口。之所以说它“最底层”，是因为除了可以拿到窗口中的所有数据之外，ProcessWindowFunction 还可以获取到一个“上下文对象”（Context）。这个上下文对象非常强大，不仅能够获取窗口信息，还可以访问当前的时间和状态信息。这里的时间就包括了处理时间（processing time）和事件时间水位线（event time watermark）。这就使得 ProcessWindowFunction 更加灵活、功能更加丰富，可以认为是一个增强版的 WindowFunction。 具体使用跟 WindowFunction 非常类似，我们可以基于 WindowedStream 调用 process()方法，传入一个 ProcessWindowFunction 的实现类。

```scala
env.addSource(new ClickSource)
	.assignAscendingTimestamps(_.timestamp)
	.keyBy(_ => "key")
	.window(TumblingEventTimeWindows.of(Time.seconds(10)))
	.process(new UvCountByWindow)
	.print()

class UvCountByWindow extends ProcessWindowFunction[Event, String, String, TimeWindow] {
    override def process(key: String, context: Context, elements: Iterable[Event], out: Collector[String]): Unit = {
      // 初始化一个Set，用来对用户名进行去重
      var userSet: Set[String] = Set[String]()
      // 将所用用户名进行去重
      elements.foreach(userSet += _.user)
      // 结合窗口信息，包装输出内容
      val windowStart: Long = context.window.getStart
      val windowEnd: Long = context.window.getEnd
      out.collect("窗口：" + windowStart + "~~~~~~~~" + windowEnd +"的PV值是" + userSet.size)
    }
}
```



#### 增量聚合和全窗口结合使用

增量聚合函数处理计算会更高效。增量聚合相当于把计算量“均摊”到了窗口收集数据的过程中，自然就会比全窗口聚合更加高效、输出更加实时。 而全窗口函数的优势在于提供了更多的信息，可以认为是更加“通用”的窗口操作，窗口计算更加灵活，功能更加强大。 所以在实际应用中，我们往往希望兼具这两者的优点，把它们结合在一起使用。Flink 的Window API 就给我们实现了这样的用法。 

我们之前在调用 WindowedStream 的 reduce()和 aggregate()方法时，只是简单地直接传入了一个 ReduceFunction 或 AggregateFunction 进行增量聚合。除此之外，其实还可以传入第二个参数：一个全窗口函数，可以是 WindowFunction 或者 ProcessWindowFunction。

```scala
env.addSource(new ClickSource)
   .assignAscendingTimestamps(_.timestamp)
   // 使用url作为key对数据进行分区
   .keyBy(_.url)
   .window(SlidingEventTimeWindows.of(Time.minutes(1), Time.seconds(20)))
   // 第一个参数增量聚合函数：聚合操作
   // 第二个参数全窗口函数：输出结果
   // 全窗口函数的IN类型就是增量聚合函数的OUT类型
   .aggregate(new UrlViewCountAgg, new UrlViewCountResult)
   .print()


class UrlViewCountAgg extends AggregateFunction[Event, Long, Long] {
    override def createAccumulator(): Long = 0L

    override def add(value: Event, accumulator: Long): Long = accumulator + 1L

    override def getResult(accumulator: Long): Long = accumulator

    override def merge(a: Long, b: Long): Long = ???
}

class UrlViewCountResult extends ProcessWindowFunction[Long, UrlViewCount, String, TimeWindow] {
    override def process(key: String, context: Context, elements: Iterable[Long], out: Collector[UrlViewCount]): Unit = {
      val start: Long = context.window.getStart
      val end: Long = context.window.getEnd
      val date: Date = new Date()
      out.collect(UrlViewCount(key, elements.iterator.next(), date))
    }
}

case class UrlViewCount(url: String, count: Long, period: Date)
```



### 其他API

对于一个窗口算子而言，窗口分配器和窗口函数是必不可少的。除此之外，Flink 还提供了其他一些可选的 API，让我们可以更加灵活地控制窗口行为。 



#### 触发器

触发器主要是用来控制窗口什么时候触发计算。所谓的“触发计算”，本质上就是执行窗口函数，所以可以认为是计算得到结果并输出的过程。基于 WindowedStream 调用 trigger()方法，就可以传入一个自定义的窗口触发器（Trigger）。

```scala
stream.keyBy(_.user)
	  .window(TumblingEventTimeWindows.of(Time.minutes(1)))
	  .trigger(new MyTrigger())

class MyTrigger extends Trigger[Any, Window] {  
    // 窗口中每到来一个元素，都会调用这个方法
    override def onElement(t: Any, l: Long, w: Window, triggerContext: Trigger.TriggerContext): TriggerResult = {
        
    }
    
    // 当注册的处理时间定时器触发时，将调用这个方法
    override def onProcessingTime(l: Long, w: Window, triggerContext: Trigger.TriggerContext): TriggerResult = ???
    
    // 当注册的事件时间定时器触发时，将调用这个方法
    override def onEventTime(l: Long, w: Window, triggerContext: Trigger.TriggerContext): TriggerResult = ???
	// 当窗口关闭销毁时，调用这个方法，一般用来清楚自定义的状态
    override def clear(w: Window, triggerContext: Trigger.TriggerContext): Unit = ???
}
```



#### 移除器

移除器主要用来定义移除某些数据的逻辑。基于 WindowedStream 调用.evictor()方法，就可以传入一个自定义的移除器（Evictor）。Evictor 是一个接口，不同的窗口类型都有各自预实现的移除器。 

```scala
stream.keyBy(_.user)
      .window(TumblingEventTimeWindows.of(Time.minutes(1)))
      .evictor(new Evictor[Event, Window] {
        // 定义执行窗口函数之前的移除数据操作
        override def evictBefore(iterable: lang.Iterable[TimestampedValue[Event]], i: Int, w: Window, evictorContext: Evictor.EvictorContext): Unit = ???
        // 定义执行窗口函数之后的移除数据操作
        override def evictAfter(iterable: lang.Iterable[TimestampedValue[Event]], i: Int, w: Window, evictorContext: Evictor.EvictorContext): Unit = ???
})
```



#### 允许延迟

在事件时间语义下，窗口中可能会出现数据迟到的情况。迟到数据默认会被直接丢弃，不会进入窗口进行统计计算。这样可能会导致统计结果不准确。 为了解决迟到数据的问题，Flink 提供了一个特殊的接口，可以为窗口算子设置一个“允许的最大延迟”（Allowed Lateness）。也就是说，我们可以设定允许延迟一段时间，在这段时间内，窗口不会销毁，继续到来的数据依然可以进入窗口中并触发计算。直到水位线推进到了窗口结束时间 + 延迟时间，才真正将窗口的内容清空，正式关闭窗口。 基于 WindowedStream 调用 allowedLateness()方法，传入一个 Time 类型的延迟时间，就可以表示允许这段时间内的延迟数据。

```scala
stream.keyBy(_.user)
      .window(TumblingEventTimeWindows.of(Time.seconds(1)))
      .allowedLateness(Time.minutes(1))
```



#### 测输出流

Flink 还提供了另外一种方式处理迟到数据。我们可以将未收入窗口的迟到数据，放入“侧输出流”（side output）进行另外的处理。所谓的侧输出流，相当于是数据流的一个“分支”，这个流中单独放置那些本该被丢弃的数据。 基于 WindowedStream 调用 sideOutputLateData() 方法，就可以实现这个功能。方法需要传入一个“输出标签”（OutputTag），用来标记分支的迟到数据流。因为保存的就是流中的原始数据，所以 OutputTag 的类型与流中数据类型相同。 

```scala
val OutputTag: OutputTag[Event] = new OutputTag[Event]("late")
stream.keyBy(_.user)
      .window(TumblingEventTimeWindows.of(Time.seconds(1)))
      .sideOutputLateData(OutputTag)
```



## 迟到数据处理

所谓的“迟到数据”（late data），是指某个水位线之后到来的数据，它的时间戳其实是在水位线之前的。所以只有在事件时间语义下，讨论迟到数据的处理才是有意义的。

### 设置水位线延迟时间

水位线是事件时间的进展，它是我们整个应用的全局逻辑时钟。水位线生成之后，会随着数据在任务间流动，从而给每个任务指明当前的事件时间。所以从这个意义上讲，水位线是一个覆盖万物的存在，它并不只针对事件时间窗口有效。 

水位线其实是所有事件时间定时器触发的判断标准。那么水位线的延迟，当然也就是全局时钟的滞后。 

既然水位线这么重要，那一般情况就不应该把它的延迟设置得太大，否则流处理的实时性就会大大降低。因为水位线的延迟主要是用来对付分布式网络传输导致的数据乱序，而网络传输的乱序程度一般并不会很大，大多集中在几毫秒至几百毫秒。所以实际应用中，我们往往会给水位线设置一个“能够处理大多数乱序数据的小延迟”，视需求一般设在毫秒~秒级。 

当我们设置了水位线延迟时间后，所有定时器就都会按照延迟后的水位线来触发。如果一个数据所包含的时间戳，小于当前的水位线，那么它就是所谓的“迟到数据”。

### 允许窗口处理迟到数据

除设置水位线延迟外，Flink 的窗口也是可以设置延迟时间，允许继续处理迟到数据的。 

这种情况下，由于大部分乱序数据已经被水位线的延迟等到了，所以往往迟到的数据不会太多。这样，我们会在水位线到达窗口结束时间时，先快速地输出一个近似正确的计算结果；然后保持窗口继续等到延迟数据，每来一条数据，窗口就会再次计算，并将更新后的结果输出。

这样就可以逐步修正计算结果，最终得到准确的统计值了。 

这其实就是著名的 Lambda 架构。原先需要两套独立的系统来同时保证实时性和结果的最终正确性，如今 Flink 一套系统就全部搞定了。

### 将迟到数据放入窗口侧输出流

只能将之前的窗口计算结果保存下来，然后获取侧输出流中的迟到数据，判断数据所属的窗口，手动对结果进行合并更新。尽管有些烦琐，实时性也不够强，但能够保证最终结果一定是正确的。 

### 总结

Flink 处理迟到数据，对于结果的正确性有三重保障：水位线的延迟，窗口允许迟到数据，以及将迟到数据放入窗口侧输出流。

```scala
package Tutorial.WindowAPI

import Tutorial.DataStreamAPI.ClickSource
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.time.Duration
import java.util.Date

/**
 * Project:  BigDataCode
 * Create date:  2023/6/29
 * Created by lwPigKing
 */
object AllowLateness {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val stream: DataStream[Event] = env.socketTextStream("localhost", 7777)
      .map(data => {
        val fields: Array[String] = data.split(",")
        Event(fields(0), fields(1), fields(2).toLong)
      })
      // 方法一：设置watermark延迟时间2秒钟
      .assignTimestampsAndWatermarks(
        WatermarkStrategy.forBoundedOutOfOrderness[Event](Duration.ofSeconds(2))
          .withTimestampAssigner(new SerializableTimestampAssigner[Event] {
            override def extractTimestamp(element: Event, recordTimestamp: Long): Long = element.timestamp
          })
      )

    val outputTag: OutputTag[Event] = OutputTag[Event]("late")
    val result: DataStream[UrlViewCount] = stream.keyBy(_.url)
      .window(TumblingEventTimeWindows.of(Time.seconds(10)))
      // 方式二：允许窗口处理迟到数据，设置1分钟的等待时间
      .allowedLateness(Time.minutes(1))
      // 方式三：将最后的迟到数据输出到侧输出流
      .sideOutputLateData(outputTag)
      .aggregate(new UrlViewCountAgg, new UrlViewCountResult)

    result.print("result")
    result.getSideOutput(outputTag).print("late")
    stream.print("input")


    env.execute()
  }
  case class Event(user: String, url: String, timestamp: Long)
  case class UrlViewCount(url: String, count: Long, windowStart: Long, windowEnd: Long)

  class UrlViewCountAgg extends AggregateFunction[Event, Long, Long] {
    override def createAccumulator(): Long = 0L

    override def add(value: Event, accumulator: Long): Long = accumulator + 1L

    override def getResult(accumulator: Long): Long = accumulator

    override def merge(a: Long, b: Long): Long = ???
  }

  class UrlViewCountResult extends ProcessWindowFunction[Long, UrlViewCount, String, TimeWindow] {
    override def process(key: String, context: Context, elements: Iterable[Long], out: Collector[UrlViewCount]): Unit = {
      out.collect(UrlViewCount(key, elements.iterator.next(), context.window.getStart, context.window.getEnd))
    }
  }
}
```

