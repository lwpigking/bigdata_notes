## 多agent顺序连接

Flume支持跨越多个Agent的数据传递，这要求前一个Agent的 Sink和下一个Agent的Source都必须是 `Avro` 类型，Sink 指向 Source 所在主机名 (或 IP 地址) 和端口。



## 多agent的复杂流

日志收集中常常存在大量的客户端（比如分布式 web 服务），Flume 支持使用多个 Agent 分别收集日志，然后通过一个或者多个 Agent 聚合后再存储到文件系统中。



## 多路复用

Flume 支持从一个 Source 向多个 Channel，也就是向多个 Sink 传递事件，这个操作称之为 `Fan Out`(扇出)。默认情况下 `Fan Out` 是向所有的 Channel 复制 `Event`，即所有 Channel 收到的数据都是相同的。同时 Flume 也支持在 `Source` 上自定义一个复用选择器 (multiplexing selector) 来实现自定义的路由规则。

