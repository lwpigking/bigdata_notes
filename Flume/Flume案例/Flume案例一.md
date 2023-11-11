# 需求

需求： 监听文件内容变动，将新增加的内容输出到控制台。

实现： 主要使用 `Exec Source` 配合 `tail` 命令实现。

# 配置文件

```properties
#指定agent的sources,sinks,channels
a1.sources = s1  
a1.sinks = k1  
a1.channels = c1  
   
#配置sources属性
a1.sources.s1.type = exec
a1.sources.s1.command = tail -F /tmp/log.txt
a1.sources.s1.shell = /bin/bash -c

#将sources与channels进行绑定
a1.sources.s1.channels = c1
   
#配置sink 
a1.sinks.k1.type = logger

#将sinks与channels进行绑定  
a1.sinks.k1.channel = c1  
   
#配置channel类型
a1.channels.c1.type = memory
```

# 启动

```shell
flume-ng agent \
--c conf \
--f /opt/module/flume/job/exec-memory-logger.properties \
--n a1 \
-Dflume.root.logger=INFO,console
```

# 测试

```shell
# 向文件中追加数据
echo "hello flume" >> /tmp/log.txt
echo "hello kafka" >> /tmp/log.txt
echo "hello flink" >> /tmp/log.txt
echo "hello storm" >> /tmp/log.txt
echo "hello SparkStreaming" >> /tmp/log.txt
```

