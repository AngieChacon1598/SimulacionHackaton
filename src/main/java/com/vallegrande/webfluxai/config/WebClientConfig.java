package com.vallegrande.webfluxai.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private static final int TIMEOUT = 30000; 
    private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024; 

    private HttpClient createHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT)
                .responseTimeout(Duration.ofMillis(TIMEOUT))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS)));
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                        .build())
                .build();
    }

    @Bean("jsearchWebClient")
    public WebClient jsearchWebClient(ApiProperties apiProperties) {
        return WebClient.builder()
                .baseUrl(apiProperties.getJsearch().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .defaultHeader("X-RapidAPI-Key", apiProperties.getJsearch().getApiKey())
                .defaultHeader("X-RapidAPI-Host", apiProperties.getJsearch().getApiHost())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                        .build())
                .build();
    }

    @Bean("languageIdentifyWebClient")
    public WebClient languageIdentifyWebClient(ApiProperties apiProperties) {
        return WebClient.builder()
                .baseUrl(apiProperties.getLanguageIdentify().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .defaultHeader("X-RapidAPI-Key", apiProperties.getLanguageIdentify().getApiKey())
                .defaultHeader("X-RapidAPI-Host", apiProperties.getLanguageIdentify().getApiHost())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                        .build())
                .build();
    }
}
