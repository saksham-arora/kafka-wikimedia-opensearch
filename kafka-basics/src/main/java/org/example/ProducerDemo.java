package org.example;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemo {

    private static final Logger log=LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());

    public static void main(String[] args) throws InterruptedException {

        log.info("Kafka Producer Started");

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "cluster.playground.cdkt.io:9092");
        properties.setProperty("sasl.mechanism", "PLAIN");
        properties.setProperty("security.protocol", "SASL_SSL");
        properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username='8ipX7HmsHMWCc4YRSJHPd' password='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiI4aXBYN0htc0hNV0NjNFlSU0pIUGQiLCJvcmdhbml6YXRpb25JZCI6NzE2MjYsInVzZXJJZCI6ODMwNTgsImZvckV4cGlyYXRpb25DaGVjayI6IjdkMzc0MGJmLTZlZDQtNDE0MS1iY2U3LTcxYTgzOTk0MTg1MyJ9fQ.EaJf82zq0zhPbPmAmX4UTn6P5oPZSDmxzvUkFYQUxXs';");

        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);


        for( int j=0;j<2;j++) {
            for (int i = 0; i < 10; i++) {

                String key = "key_" + i;
                String value = "Hello" + i;

                ProducerRecord<String, String> producerRecord = new ProducerRecord<>("demo_java", key, value);

                producer.send(producerRecord,
                        new Callback() {
                            @Override
                            public void onCompletion(RecordMetadata metadata, Exception e) {
                                if (e == null) {
                                    log.info("Recieved new metadata \n" + "Key " + key +
                                            " | Partition " + metadata.partition()
                                            );
                                } else {
                                    log.error("Error while producing", e);
                                }
                            }
                        });
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        producer.flush();

        producer.close();
    }
}