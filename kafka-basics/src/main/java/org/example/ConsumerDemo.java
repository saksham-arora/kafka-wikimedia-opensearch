package org.example;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemo {

    private static final Logger log=LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());

    public static void main(String[] args) throws InterruptedException {

        log.info("Kafka Consumer Started");

        String groupId="first-grp";
        String topic="demo_java";

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "cluster.playground.cdkt.io:9092");
        properties.setProperty("sasl.mechanism", "PLAIN");
        properties.setProperty("security.protocol", "SASL_SSL");
        properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username='8ipX7HmsHMWCc4YRSJHPd' password='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiI4aXBYN0htc0hNV0NjNFlSU0pIUGQiLCJvcmdhbml6YXRpb25JZCI6NzE2MjYsInVzZXJJZCI6ODMwNTgsImZvckV4cGlyYXRpb25DaGVjayI6IjdkMzc0MGJmLTZlZDQtNDE0MS1iY2U3LTcxYTgzOTk0MTg1MyJ9fQ.EaJf82zq0zhPbPmAmX4UTn6P5oPZSDmxzvUkFYQUxXs';");

        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());
        properties.setProperty("group.id", groupId);
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

        final Thread mainThread=Thread.currentThread();

//        shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run(){
                log.info("Detected Shutdown");
                consumer.wakeup();
                try {
                    mainThread.join();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });

        try {
            consumer.subscribe(Arrays.asList(topic));

            while (true) {
                log.info("Polling");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Key : " + record.key() + ", Value : " + record.value());
                    log.info(("Partition : " + record.partition() + ", Offset : " + record.offset()));
                }
            }
        }catch (WakeupException w){
            log.info("Consumer starting the shut down");
        }
        catch (Exception e){
            log.error("Unexpected Exception ",e);
        }
        finally {
            consumer.close();
            log.info("Consumer Closed");
        }
    }
}