package org.example;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikimediaChangesProducer {
    public static void main(String[] args) throws InterruptedException {

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "cluster.playground.cdkt.io:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        properties.setProperty(ProducerConfig, StringSerializer.class.getName());
        properties.setProperty("sasl.mechanism", "PLAIN");
        properties.setProperty("security.protocol", "SASL_SSL");
        properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username='8ipX7HmsHMWCc4YRSJHPd' password='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiI4aXBYN0htc0hNV0NjNFlSU0pIUGQiLCJvcmdhbml6YXRpb25JZCI6NzE2MjYsInVzZXJJZCI6ODMwNTgsImZvckV4cGlyYXRpb25DaGVjayI6IjdkMzc0MGJmLTZlZDQtNDE0MS1iY2U3LTcxYTgzOTk0MTg1MyJ9fQ.EaJf82zq0zhPbPmAmX4UTn6P5oPZSDmxzvUkFYQUxXs';");


        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        String topic="wikimedia.recentchange";

        EventHandler eventHandler= new WikimediaChangeHandler(producer,topic);

        String url="https://stream.wikimedia.org/v2/stream/recentchange";
        EventSource.Builder builder=new EventSource.Builder(eventHandler, URI.create(url));
        EventSource eventSource=builder.build();

        eventSource.start();

        TimeUnit.MINUTES.sleep(10);

    }
}
