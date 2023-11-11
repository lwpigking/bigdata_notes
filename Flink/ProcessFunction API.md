# ProcessFunction API

之前所介绍的流处理 API，无论是基本的转换、聚合，还是更为复杂的窗口操作，其实都是基于 DataStream 进行转换的；所以可以统称为 DataStream API，这也是 Flink 编程的核心。而我们知道，为了让代码有更强大的表现力和易用性，Flink 本身提供了多层 API，DataStream API 只是中间的一环，在更底层，我们可以不定义任何具体的算子（比如 map()，filter()，或者 window()），而只是提炼出一个统一的“处理”（process）操作——它是所有转换算子的一个概括性的表达，可以自定义处理逻辑，所以这一层接口就被叫作“处理函数”（process function）。 



## ProcessFunction

处理函数主要是定义数据流的转换操作，所以也可以把它归到转换算子中。我们知道在 Flink 中几乎所有转换算子都提供了对应的函数类接口，处理函数也不例外；它所对应的函数类，就叫作ProcessFunction。

我们之前学习的转换算子，一般只是针对某种具体操作来定义的，能够拿到的信息比较有限。比如 map()算子，我们实现的 MapFunction 中，只能获取到当前的数据，定义它转换之后的形式；而像窗口聚合这样的复杂操作，AggregateFunction 中除数据外，还可以获取到当前的状态（以累加器 Accumulator 形式出现）。另外我们还介绍过富函数类，比如 RichMapFunction，它提供了获取运行时上下文的方法 getRuntimeContext()，可以拿到状态，还有并行度、任务名称之类的运行时信息。 

但是无论哪种算子，如果我们想要访问事件的时间戳，或者当前的水位线信息，都是完全做不到的。这时就需要使用处理函数（ProcessFunction）。 

处理函数提供了一个“定时服务”（TimerService），我们可以通过它访问流中的事件（event）、时间戳（timestamp）、水位线（watermark），甚至可以注册“定时事件”。而且处理函数继承了 AbstractRichFunction 抽象类，所以拥有富函数类的所有特性，同样可以访问状态（state）和其他运行时信息。此外，处理函数还可以直接将数据输出到侧输出流（side output）中。所以，处理函数是最为灵活的处理方法，可以实现各种自定义的业务逻辑；同时也是整个DataStream API 的底层基础。 处理函数的使用与基本的转换操作类似，只需要直接基于 DataStream 调用 process()方法就可以了。方法需要传入一个 ProcessFunction 作为参数，用来定义处理逻辑。 

```scala
val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
env.setParallelism(1)

env.addSource(new ClickSource)
   .assignAscendingTimestamps(_.timestamp)
   .process(new ProcessFunction[Event, String] {
       override def processElement(i: Event, context: ProcessFunction[Event, String]#Context, collector: Collector[String]): Unit = {
           if (i.user.equals("Mary")) {
               collector.collect(i.user)
           } else if (i.user.equals("Bob")) {
               collector.collect(i.user)
           }
           println(context.timerService().currentWatermark())
       }
   })
	.print()
```

Flink 中的处理函数其实是一个大家族，ProcessFunction 只是其中一员。 

Flink 提供了 8 个不同的处理函数： 

（1）ProcessFunction

最基本的处理函数，基于 DataStream 直接调用 process()时作为参数传入。 

（2）KeyedProcessFunction

对流按键分区后的处理函数，基于 KeyedStream 调用 process()时作为参数传入。要想使用

定时器，必须基于 KeyedStream。 

（3）ProcessWindowFunction

开窗之后的处理函数，也是全窗口函数的代表。基于 WindowedStream 调用 process()时作

为参数传入。 

（4）ProcessAllWindowFunction

同样是开窗之后的处理函数，基于 AllWindowedStream 调用 process()时作为参数传入。 

（5）CoProcessFunction

合并（connect）两条流之后的处理函数，基于 ConnectedStreams 调用 process()时作为参

数传入。 

（6）ProcessJoinFunction

间隔连接（interval join）两条流之后的处理函数，基于 IntervalJoined 调用 process()时作为参数传入。 

（7）BroadcastProcessFunction

广播连接流处理函数，基于 BroadcastConnectedStream 调用 process()时作为参数传入。这里的“广播连接流”BroadcastConnectedStream，是一个未 keyBy 的普通 DataStream 与一个广播流（BroadcastStream）做连接（conncet）之后的产物。 

（8）KeyedBroadcastProcessFunction

按键分区的广播连接流处理函数，同样是基于 BroadcastConnectedStream 调用 process()时作为参数传入。与 BroadcastProcessFunction 不同的是，这时的广播连接流，是一个 KeyedStream 与广播流（BroadcastStream）做连接之后的产物。 



## KeyedProcessFunction

在 Flink 程序中，为了实现数据的聚合统计，或者开窗计算之类的功能，我们一般都要先用 keyBy()算子对数据流进行“按键分区”，得到一个 KeyedStream。而只有在 KeyedStream 中，才支持使用 TimerService 设置定时器的操作。所以一般情况下，我们都是先做了 keyBy()分区之后，再去定义处理操作；代码中更加常见的处理函数是 KeyedProcessFunction。

KeyedProcessFunction 的一个特色，就是可以灵活地使用定时器。 

定时器（timers）是处理函数中进行时间相关操作的主要机制。在 onTimer()方法中可以实现定时处理的逻辑，而它能触发的前提，就是之前曾经注册过定时器、并且现在已经到了触发时间。注册定时器的功能，是通过上下文中提供的“定时服务”（TimerService）来实现的。ProcessFunction的上下文（Context）中提供了timerService()方法，可以直接返回一个TimerService对象。

代码中使用KeyedProcessFunction，只要基于keyBy()之后的KeyedStream，直接调用process()方法，这时需要传入的参数就是KeyedProcessFunction的实现类。

```scala
val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
env.addSource(new ClickSource)
	.keyBy( r => true)
    .process(new KeyedProcessFunction[Boolean, Event, String] {
        override def processElement(i: Event, context: KeyedProcessFunction[Boolean, Event, String]#Context, collector: Collector[String]): Unit = {
            val currTs: Long = context.timerService().currentProcessingTime()
            collector.collect("数据到达，到达时间：" + new Timestamp(currTs))
            // 注册10秒钟之后的处理时间定时器
            context.timerService().registerProcessingTimeTimer(currTs + 10 * 1000L)
        }
        
        override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Boolean, Event, String]#OnTimerContext, out: Collector[String]): Unit = {
            out.collect("定时器触发，触发时间：" + new Timestamp(timestamp))
        }
    })
	.print()
env.execute()
```

