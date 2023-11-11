## 简介

Apache Flume 是一个分布式，高可用的数据收集系统。它可以从不同的数据源收集数据，经过聚合后发送到存储系统中，通常用于日志数据的收集。Flume 分为 NG 和 OG (1.0 之前) 两个版本，NG 在 OG 的基础上进行了完全的重构，是目前使用最为广泛的版本。下面的介绍均以 NG 为基础。



## 架构和基本概念

### 基本架构

外部数据源以特定格式向 Flume 发送 `events` (事件)，当 `source` 接收到 `events` 时，它将其存储到一个或多个 `channel`，`channe` 会一直保存 `events` 直到它被 `sink` 所消费。`sink` 的主要功能从 `channel` 中读取 `events`，并将其存入外部存储系统或转发到下一个 `source`，成功后再从 `channel` 中移除 `events`。

### 基本概念

**1. Event**

`Event`是Flume数据传输的基本单元。类似于 JMS 和消息系统中的消息。一个 `Event` 由标题和正文组成：前者是键/值映射，后者是任意字节数组。

**2. Source** 

数据收集组件，从外部数据源收集数据，并存储到 Channel 中。

**3. Channel**

`Channel` 是源和接收器之间的管道，用于临时存储数据。可以是内存或持久化的文件系统：

+ `Memory Channel` : 使用内存，优点是速度快，但数据可能会丢失 (如突然宕机)；
+ `File Channel` : 使用持久化的文件系统，优点是能保证数据不丢失，但是速度慢。

**4. Sink**

`Sink` 的主要功能从 `Channel` 中读取 `Event`，并将其存入外部存储系统或将其转发到下一个 `Source`，成功后再从 `Channel` 中移除 `Event`。

**5. Agent**

是一个独立的 (JVM) 进程，包含 `Source`、 `Channel`、 `Sink` 等组件。



### 组件种类

Flume 中的每一个组件都提供了丰富的类型，适用于不同场景：

- Source 类型 ：内置了几十种类型，如 `Avro Source`，`Thrift Source`，`Kafka Source`，`JMS Source`；

- Sink 类型 ：`HDFS Sink`，`Hive Sink`，`HBaseSinks`，`Avro Sink` 等；

- Channel 类型 ：`Memory Channel`，`JDBC Channel`，`Kafka Channel`，`File Channel` 等。

对于 Flume 的使用，除非有特别的需求，否则通过组合内置的各种类型的 Source，Sink 和 Channel 就能满足大多数的需求。在Flume 官网 上对所有类型组件的配置参数均以表格的方式做了详尽的介绍，并附有配置样例；同时不同版本的参数可能略有所不同，所以使用时建议选取官网对应版本的 User Guide 作为主要参考资料。
