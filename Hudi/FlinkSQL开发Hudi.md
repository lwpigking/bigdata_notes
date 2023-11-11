# FlinkSQL开发Hudi

## 读数据

```java
package com.lwPigKing.hudi.flink;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

/**
 * Project:  BigDataProject
 * Create date:  2023/8/8
 * Created by lwPigKing
 */
public class FlinkSQLReadDemo {
    public static void main(String[] args) {
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        TableEnvironment tableEnv = TableEnvironment.create(settings) ;

        tableEnv.executeSql(
                "CREATE TABLE order_hudi(\n" +
                        "  orderId STRING PRIMARY KEY NOT ENFORCED,\n" +
                        "  userId STRING,\n" +
                        "  orderTime STRING,\n" +
                        "  ip STRING,\n" +
                        "  orderMoney DOUBLE,\n" +
                        "  orderStatus INT,\n" +
                        "  ts STRING,\n" +
                        "  partition_day STRING\n" +
                        ")\n" +
                        "PARTITIONED BY (partition_day)\n" +
                        "WITH (\n" +
                        "  'connector' = 'hudi',\n" +
                        "  'path' = 'file:///D:/flink_hudi_order',\n" +
                        "  'table.type' = 'MERGE_ON_READ',\n" +
                        "  'read.streaming.enabled' = 'true',\n" +
                        "  'read.streaming.check-interval' = '4'\n" +
                        ")"
        );

        tableEnv.executeSql(
                "SELECT\n" +
                        "orderId, userId, orderTime, ip, orderMoney, orderStatus, ts, partition_day\n" +
                        "FROM order_hudi"
        ).print();

    }
}

```



## 插数据

```java
package com.lwPigKing.hudi.flink;

/**
 * Project:  BigDataProject
 * Create date:  2023/8/8
 * Created by lwPigKing
 */

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Based on Flink SQL: the data in the topic is consumed in real time,
 * and after conversion processing, it's stored in the Hudi table in real time
 */
public class FlinkSQLHudiDemo {
    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(5000);
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        tableEnv.executeSql(
                "CREATE TABLE order_kafka_source (\n" +
                        "  orderId STRING,\n" +
                        "  userId STRING,\n" +
                        "  orderTime STRING,\n" +
                        "  ip STRING,\n" +
                        "  orderMoney DOUBLE,\n" +
                        "  orderStatus INT\n" +
                        ") WITH (\n" +
                        "  'connector' = 'kafka',\n" +
                        "  'topic' = 'order-topic',\n" +
                        "  'properties.bootstrap.servers' = 'node1.itcast.cn:9092',\n" +
                        "  'properties.group.id' = 'gid-1001',\n" +
                        "  'scan.startup.mode' = 'latest-offset',\n" +
                        "  'format' = 'json',\n" +
                        "  'json.fail-on-missing-field' = 'false',\n" +
                        "  'json.ignore-parse-errors' = 'true'\n" +
                        ")"
        );

        Table etlTable = tableEnv
                .from("order_kafka_source")
                .addColumns(
                        $("orderId").substring(0, 17).as("ts")
                )
                .addColumns(
                        $("orderTime").substring(0, 10).as("partition_day")
                );
        tableEnv.createTemporaryView("view_order", etlTable);

        tableEnv.executeSql(
                "CREATE TABLE order_hudi_sink (\n" +
                        "  orderId STRING PRIMARY KEY NOT ENFORCED,\n" +
                        "  userId STRING,\n" +
                        "  orderTime STRING,\n" +
                        "  ip STRING,\n" +
                        "  orderMoney DOUBLE,\n" +
                        "  orderStatus INT,\n" +
                        "  ts STRING,\n" +
                        "  partition_day STRING\n" +
                        ")\n" +
                        "PARTITIONED BY (partition_day) \n" +
                        "WITH (\n" +
                        "  'connector' = 'hudi',\n" +
                        "  'path' = 'file:///D:/flink_hudi_order',\n" +
                        "  'table.type' = 'MERGE_ON_READ',\n" +
                        "  'write.operation' = 'upsert',\n" +
                        "  'hoodie.datasource.write.recordkey.field' = 'orderId'," +
                        "  'write.precombine.field' = 'ts'" +
                        "  'write.tasks'= '1'" +
                        ")"
        );

        tableEnv.executeSql(
                "INSERT INTO order_hudi_sink\n" +
                        "SELECT\n" +
                        "orderId, userId, orderTime, ip, orderMoney, orderStatus, ts, partition_day\n" +
                        "FROM view_order"
        );


    }
}

```

