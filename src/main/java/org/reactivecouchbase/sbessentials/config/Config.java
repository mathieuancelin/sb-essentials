package org.reactivecouchbase.sbessentials.config;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.NamedExecutors;
import org.reactivecouchbase.sbessentials.libs.future.FutureSupport;
import org.reactivecouchbase.sbessentials.libs.json.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class Config {

    private final ActorSystem system = ActorSystem.create("SpringBootAppSystem");
    private final ActorMaterializer materializer = ActorMaterializer.create(system);
    private final AtomicReference<ExecutorService> globalExecutorRef = new AtomicReference<>(null);

    @Value("${app.config.async.timeout}")
    public String timeoutDuration;

    @Value("${app.config.async.globalec.threadcount}")
    public String threadCount;

    @Bean
    public ActorSystem actorSystem() {
        return system;
    }

    @Bean
    public ActorMaterializer actorMaterializer() {
        return materializer;
    }

    // no idea which one is better ...
    // @Bean @Scope("prototype")
    // public ActorMaterializer actorMaterializer() {
    //     return ActorMaterializer.create(system);
    // }

    @Bean
    public ExecutorService globalExecutor() {
        if (globalExecutorRef.get() == null) {
            ExecutorService executorService = NamedExecutors.newFixedThreadPool(Integer.valueOf(threadCount), "GlobalExecutor");
            globalExecutorRef.compareAndSet(null, executorService);
        }
        return globalExecutorRef.get();
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
                configurer.setTaskExecutor(new ConcurrentTaskExecutor(globalExecutor()));
                configurer.registerCallableInterceptors(timeoutInterceptor());
            }

            @Bean
            public TimeoutCallableProcessingInterceptor timeoutInterceptor() {
                return new TimeoutCallableProcessingInterceptor();
            }

            @Override
            public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
                // returnValueHandlers.add(new FutureSupport.FutureReturnValueHandler(ActorMaterializer.create(system)));
                returnValueHandlers.add(new FutureSupport.FutureReturnValueHandler(actorMaterializer()));
            }
        };
    }
}