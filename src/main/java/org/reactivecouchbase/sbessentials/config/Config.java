package org.reactivecouchbase.sbessentials.config;

import akka.actor.ActorSystem;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.sbessentials.libs.json.JsonMessageConverter;
import org.reactivecouchbase.sbessentials.libs.streams.SourceMessageConverter;
import org.reactivecouchbase.sbessentials.libs.future.FutureSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;
import java.util.concurrent.Executors;

@Configuration
public class Config {

    private final ActorSystem system = ActorSystem.create("SpringBootAppSystem");

    @Bean
    public ActorSystem actorSystem() {
        return system;
    }


    @Bean
    public WebMvcConfigurer rxJavaWebMvcConfiguration() {
        return new WebMvcConfigurerAdapter() {

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new JsonMessageConverter());
                converters.add(new SourceMessageConverter());
            }

            @Override
            public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
                configurer.setDefaultTimeout(Duration.parse("10min").toMillis());
                configurer.setTaskExecutor(new ConcurrentTaskExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)));
                configurer.registerCallableInterceptors(timeoutInterceptor());
            }

            @Bean
            public TimeoutCallableProcessingInterceptor timeoutInterceptor() {
                return new TimeoutCallableProcessingInterceptor();
            }

            @Override
            public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
                returnValueHandlers.add(new FutureSupport.FutureReturnValueHandler(system));
            }
        };
    }
}