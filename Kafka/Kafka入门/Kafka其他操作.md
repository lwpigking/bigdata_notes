## 其他

```
增加副本因子
vim increase-replication-factor.json

{
    "version":1,
    "partitions":[
       {"topic":"four",
        "partition":0,
        "replicas":[0,1,2]},
        {"topic":four,
         "partitions":1,
         "replicas":[0,1,2]},
         {"topic":four,
          "partition":2,
          "replicas":[0,1,2]}
          ]
}

kafka-reassign-partitions.sh --bootstrap-server
master:9092 --reassignment-json-file increase-replication-factor.json
--execute
```

```
手动调整分区

创建副本存储计划
vim increase-replication-factor.json
{
    "version":1,
    "partitions":[{"topic":"three","partition":0,"replicas":[0,1]},
                  {"topic":"three","partition":1,"replicas":[0,1]},
                  {"topic":"three","partition":2,"replicas":[1,0]},
                  {"topic":"three","partition":3,"replicas":[1,0]}]
}

执行副本存储计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--reassignment-json-file increase-replication-factor.json --execute

验证副本存储计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--reassignment-json-file increase-replication-factor.json --verify
```

```
服役新节点

1.创建一个负载均衡的主题
vim topic-to-move.json
{
    "topics": [
        {"topic":"first"}
    ],

    "version":1
}

2.生成一个负载均衡的计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--topic-to-move-json-file topics-to-move.json
--broker-list "0, 1, 2, 3" --generate

3.创建副本存储计划
vim increase-replication-factor.json
在该json文件里写入Proposed partition reassignment configuration所生成的内容

4.执行副本存储计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--reassignment-json-file increase-replication-factor.json --execute

5.验证副本存储计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--reassignment-json-file increase-replication-factor.json --verify
```

```
退役旧节点

1.创建一个负载均衡的主题
vim topic-to-move.json
{
    "topics": [
        {"topic":"first"}
    ],

    "version":1
}

2.生成一个负载均衡的计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--topic-to-move-json-file topics-to-move.json
--broker-list "0, 1, 2" --generate

3.创建副本存储计划
vim increase-replication-factor.json
在该json文件里写入Proposed partition reassignment configuration所生成的内容

4.执行副本存储计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--reassignment-json-file increase-replication-factor.json --execute

5.验证副本存储计划
kafka-reassign-partitions.sh --bootstrap-server master:9092
--reassignment-json-file increase-replication-factor.json --verify

6.停止kafka
kafka-server-stop.sh
```

```
查看文件存储index和log信息
kafka-run-class.sh kafka.tools.DumpLogSegments -files ./0000000.index
```

