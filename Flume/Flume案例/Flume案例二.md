# 需求

需求： 监听指定目录，将目录下新增加的文件存储到 HDFS。

实现：使用 `Spooling Directory Source` 和 `HDFS Sink`。

# 配置

```properties
#指定agent的sources,sinks,channels
a1.sources = s1  
a1.sinks = k1  
a1.channels = c1  
   
#配置sources属性
a1.sources.s1.type =spooldir  
a1.sources.s1.spoolDir =/tmp/logs
a1.sources.s1.basenameHeader = true
a1.sources.s1.basenameHeaderKey = fileName 
#将sources与channels进行绑定  
a1.sources.s1.channels =c1 

   
#配置sink 
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = /flume/events/%y-%m-%d/%H/
a1.sinks.k1.hdfs.filePrefix = %{fileName}
#生成的文件类型，默认是Sequencefile，可用DataStream，则为普通文本
a1.sinks.k1.hdfs.fileType = DataStream  
a1.sinks.k1.hdfs.useLocalTimeStamp = true
#将sinks与channels进行绑定  
a1.sinks.k1.channel = c1
   
#配置channel类型
a1.channels.c1.type = memory
```

# 启动

```bash
flume-ng agent \
--conf conf \
--conf-file /usr/app/apache-flume-1.6.0-cdh5.15.2-bin/examples/spooling-memory-hdfs.properties \
--name a1 -Dflume.root.logger=INFO,console
```

# 测试

拷贝任意文件到监听目录下，可以从日志看到文件上传到 HDFS 的路径：

```bash
cp log.txt logs/
```

查看上传到 HDFS 上的文件内容与本地是否一致：

```bash
hdfs dfs -cat /flume/events/19-04-09/13/log.txt.1554788567801
```

