```java
package producer;

import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Project:  BigDataCode
 * Create date:  2023/5/17
 * Created by fujiahao
 */

/**
 * Kafka事务
 */

public class CustomProducerTransactions {
    public static void main(String[] args) {

        // 配置
        Properties properties = new Properties();
        // 连接集群
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "master:9092");
        // 指定对应的key和value的序列化类型
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 1.创建kafka生产者对象
        // "" hello
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        // 指定事务id
        // 开启事务一定要指定事务id
        properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "lwPigKing");

        // 初始化事务
        kafkaProducer.initTransactions();

        // 开启事务
        kafkaProducer.beginTransaction();

        try {
            // 2.发送数据
            for (int i = 0; i < 5; i++) {
                kafkaProducer.send(new ProducerRecord<>("first", "lwPigKing" + i));
            }
            // 提交事务
            kafkaProducer.commitTransaction();
        } catch (Exception e) {
            kafkaProducer.abortTransaction();
        } finally {
            // 3.关闭资源
            kafkaProducer.close();
        }

    }
}

```

