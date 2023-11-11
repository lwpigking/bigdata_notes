# 需求

需求： 将本服务器收集到的数据发送到另外一台服务器。

实现：使用 `avro sources` 和 `avro Sink` 实现。

# 配置

新建配置 `netcat-memory-avro.properties`，监听文件内容变化，然后将新的文件内容通过 `avro sink` 发送到slave1这台服务器的 8888 端口：

```properties
#指定agent的sources,sinks,channels
a1.sources = s1
a1.sinks = k1
a1.channels = c1

#配置sources属性
a1.sources.s1.type = exec
a1.sources.s1.command = tail -F /tmp/log.txt
a1.sources.s1.shell = /bin/bash -c
a1.sources.s1.channels = c1

#配置sink
a1.sinks.k1.type = avro
a1.sinks.k1.hostname = slave1
a1.sinks.k1.port = 8888
a1.sinks.k1.batch-size = 1
a1.sinks.k1.channel = c1

#配置channel类型
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100
```

使用 `avro source` 监听slave1服务器的 8888 端口，将获取到内容输出到控制台：

```properties
#指定agent的sources,sinks,channels
a2.sources = s2
a2.sinks = k2
a2.channels = c2

#配置sources属性
a2.sources.s2.type = avro
a2.sources.s2.bind = master
a2.sources.s2.port = 8888

#将sources与channels进行绑定
a2.sources.s2.channels = c2

#配置sink
a2.sinks.k2.type = logger

#将sinks与channels进行绑定
a2.sinks.k2.channel = c2

#配置channel类型
a2.channels.c2.type = memory
a2.channels.c2.capacity = 1000
a2.channels.c2.transactionCapacity = 100
```

# 启动

```bash
flume-ng agent \
--conf conf \
--conf-file /usr/app/apache-flume-1.6.0-cdh5.15.2-bin/examples/netcat-memory-avro.properties \
--name a1 -Dflume.root.logger=INFO,console
```

这里建议按以上顺序启动，原因是 `avro.source` 会先与端口进行绑定，这样 `avro sink` 连接时才不会报无法连接的异常。但是即使不按顺序启动也是没关系的，`sink` 会一直重试，直至建立好连接。

# 测试

向文件 `tmp/log.txt` 中追加内容：

```bash
# 向文件中追加数据
echo "hello flume" >> /tmp/log.txt
echo "hello kafka" >> /tmp/log.txt
echo "hello flink" >> /tmp/log.txt
echo "hello storm" >> /tmp/log.txt
echo "hello SparkStreaming" >> /tmp/log.txt
```

可以看到已经从 8888 端口监听到内容，并成功输出到控制台。
