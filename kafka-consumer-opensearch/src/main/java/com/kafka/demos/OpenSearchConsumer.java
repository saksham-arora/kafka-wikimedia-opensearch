package com.kafka.demos;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
//import java.util.logging.Logger;

public class OpenSearchConsumer {

        public static RestHighLevelClient createOpenSearchClient(){
//            String connString = "http://localhost:9200";
        String connString = "https://dsfawg0uw8:fdeq17pvhw@kafka-consumer-end-7462813109.us-east-1.bonsaisearch.net:443";

            // we build a URI from the connection string
            RestHighLevelClient restHighLevelClient;
            URI connUri = URI.create(connString);
            // extract login information if it exists
            String userInfo = connUri.getUserInfo();

            if (userInfo == null) {
                // REST client without security
                restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

            } else {
                // REST client with security
                String[] auth = userInfo.split(":");

                CredentialsProvider cp = new BasicCredentialsProvider();
                cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

                restHighLevelClient = new RestHighLevelClient(
                        RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                                .setHttpClientConfigCallback(
                                        httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));
            }

            return restHighLevelClient;
        }

        private static KafkaConsumer<String,String> createKafkaConsumer(){

            String groupId="consumer-opensearch-demo";
//            String topic="x/";

            Properties properties = new Properties();
            properties.setProperty("bootstrap.servers", "cluster.playground.cdkt.io:9092");
            properties.setProperty("sasl.mechanism", "PLAIN");
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username='8ipX7HmsHMWCc4YRSJHPd' password='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiI4aXBYN0htc0hNV0NjNFlSU0pIUGQiLCJvcmdhbml6YXRpb25JZCI6NzE2MjYsInVzZXJJZCI6ODMwNTgsImZvckV4cGlyYXRpb25DaGVjayI6IjdkMzc0MGJmLTZlZDQtNDE0MS1iY2U3LTcxYTgzOTk0MTg1MyJ9fQ.EaJf82zq0zhPbPmAmX4UTn6P5oPZSDmxzvUkFYQUxXs';");

            properties.setProperty("key.deserializer", StringDeserializer.class.getName());
            properties.setProperty("value.deserializer", StringDeserializer.class.getName());
            properties.setProperty("group.id", groupId);
            properties.setProperty("auto.offset.reset", "latest");
            properties.setProperty("enable.auto.commit", "False");
//            properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());

            return new KafkaConsumer<>(properties);

    }

    private static String extractId(String json){
            return JsonParser.parseString(json)
                    .getAsJsonObject()
                    .get("meta")
                    .getAsJsonObject()
                    .get("id")
                    .getAsString();
    }

    public static void main(String[] args) throws IOException {
            Logger log= LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());

            RestHighLevelClient openSearchClient = createOpenSearchClient();

        KafkaConsumer<String,String> consumer=createKafkaConsumer();


            try(openSearchClient; consumer) {
                boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"),
                        RequestOptions.DEFAULT);

                if (!indexExists) {

                    CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                    openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                    log.info("Wikimedia Index Created");
                } else {
                    log.info("Index already exists");
                }
                consumer.subscribe(Collections.singleton("wikimedia.recentchange"));


                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));
                    int recordCount = records.count();
                    log.info("Recieved " + recordCount + " records");
                    BulkRequest bulkRequest=new BulkRequest();

                    for (ConsumerRecord<String, String> record : records) {

                        String id= extractId(record.value());

//                    send the records in opensearch
                        try {
                            IndexRequest indexRequest = new IndexRequest("wikimedia").source(record.value(),
                                    XContentType.JSON).id(id);

//                            IndexResponse response = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
                            bulkRequest.add(indexRequest);
                        }
                        catch (Exception e){
                        }

                    }

                    if (bulkRequest.numberOfActions()>0) {
                        BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                        log.info("Inserted " + bulkResponse.getItems().length+" records");
                        try{
                            Thread.sleep(1000);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        consumer.commitSync();
                        log.info("Commited Batch");
                    }

                }
            }

}

}
