package org.reactivecouchbase.sbessentials.config;

import akka.actor.ActorSystem;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.NamedExecutors;
import org.reactivecouchbase.sbessentials.libs.future.FutureSupport;
import org.reactivecouchbase.sbessentials.libs.json.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.ExecutorService;

@Configuration
public class Config {

    private final ActorSystem system = ActorSystem.create("SpringBootAppSystem");
    private final ExecutorService globalExecutor = NamedExecutors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, "GlobalExecutor");

    @Value("${app.config.async.timeout}")
    public String timeoutDuration;

    @Bean
    public ActorSystem actorSystem() {
        return system;
    }

    @Bean
    public ExecutorService globalExecutor() {
        return globalExecutor;
    }


    @Bean
    public WebMvcConfigurer rxJavaWebMvcConfiguration() {
        return new WebMvcConfigurerAdapter() {

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new JsonMessageConverter());
            }

            @Override
            public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
                configurer.setDefaultTimeout(Duration.parse(timeoutDuration.trim().toLowerCase()).toMillis());
                configurer.setTaskExecutor(new ConcurrentTaskExecutor(globalExecutor));
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