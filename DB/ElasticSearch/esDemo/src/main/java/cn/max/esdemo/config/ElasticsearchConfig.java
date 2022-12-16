package cn.max.esdemo.config;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author MaxStar
 * @date 2022/10/29
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient creat() {
        RestHighLevelClient
        RestClient restClient = RestClient.builder(
                new HttpHost("192.168.3.112", 9200)
        ).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient elasticsearchClient = new ElasticsearchClient(transport);
    }
}
