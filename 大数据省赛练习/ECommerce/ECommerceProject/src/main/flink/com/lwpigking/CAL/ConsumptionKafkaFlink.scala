package com.lwpigking.CAL

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisConfigBase, FlinkJedisPoolConfig}
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}

import java.util.Properties


/**
 * Project:  ECommerceProject
 * Create date:  2023/10/2
 * Created by lwPigKing
 */

/**
 * 使用Flink消费kafka中的数据，统计实时营业额存入redis中
 */

object ConsumptionKafkaFlink {
  def main(args: Array[String]): Unit = {
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    environment.setParallelism(1)

    val properties: Properties = new Properties()
    properties.put("bootstrap.servers", "master:9092")
    val kafkaConsumer: FlinkKafkaConsumer[String] = new FlinkKafkaConsumer[String](
      "order",
      new SimpleStringSchema(),
      properties
    )

    val dataStream: DataStream[String] = environment.addSource(kafkaConsumer)

    val result: DataStream[(String, Double)] = dataStream.map(x => {
      val strings: Array[String] = x.split(",")
      val name: String = strings(0).split(":")(0)
      if (name.toUpperCase().trim == "L") {
        val quantity: Int = strings(4).toInt
        val extendedprice: Double = strings(5).toDouble
        val discount: Double = strings(6).toDouble
        val returnflag: String = strings(8).replaceAll("\'", "").trim

        if (returnflag.trim.toUpperCase() == "R" ||
          returnflag.trim.toUpperCase() == "N") {
          val price: Double = quantity * extendedprice * discount
          (name, price)
        } else {
          (name, 0.0)
        }
      } else if (name.toUpperCase().trim == "O") {
        val totalprice: Double = x.split(",")(3).toDouble
        (name, totalprice)
      } else {
        ("报错", 1.0)
      }
    }).keyBy(_._1).sum(1)

    result.print()
    result.addSink(new RedisSink[(String, Double)](
      new FlinkJedisPoolConfig.Builder().setHost("master").setPort(6370).build(),
      new MyRedisMapper
    ))

    environment.execute("ConsumptionKafkaFlink")
  }
}

class MyRedisMapper extends RedisMapper[(String, Double)] {
  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(RedisCommand.SET)

  override def getKeyFromData(t: (String, Double)): String = t._1

  override def getValueFromData(t: (String, Double)): String = t._2.toString
}
