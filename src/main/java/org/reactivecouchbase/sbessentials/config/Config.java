package org.reactivecouchbase.sbessentials.config;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import com.typesafe.config.ConfigFactory;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.NamedExecutors;
import org.reactivecouchbase.sbessentials.libs.actions.ActionSupport;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class Config {

    private final com.typesafe.config.Config config = ConfigFactory.load();

    private final ActorSystem system = ActorSystem.create("global-system",
            config.atPath("systems.global").withFallback(ConfigFactory.empty()));
    private final ActorMaterializer generalPurposeMaterializer = ActorMaterializer.create(system);

    private final ActorSystem wsSystem = ActorSystem.create("ws-system",
            config.atPath("systems.ws").withFallback(ConfigFactory.empty()));
    private final ActorMaterializer wsClientActorMaterializer = ActorMaterializer.create(wsSystem);

    private final ActorSystem blockingSystem = ActorSystem.create("blocking-system",
            config.atPath("systems.blocking").withFallback(ConfigFactory.empty()));
    private final ActorMaterializer blockingActorMaterializer = ActorMaterializer.create(blockingSystem);

    private final AtomicReference<ExecutorService> globalExecutorRef = new AtomicReference<>(null);

    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            system.shutdown();
            wsSystem.shutdown();
            blockingSystem.shutdown();
        }));
    }

    @Value("${app.config.async.timeout}")
    public String timeoutDuration;

    @Value("${app.config.async.globalec.threadcount}")
    public String threadCount;

    @Bean
    public ActorSystem actorSystem() {
        return system;
    }

    @Bean(name = "general-purpose-materializer")
    public ActorMaterializer actorMaterializer() {
        return generalPurposeMaterializer;
    }

    @Bean(name = "blocking-actor-materializer")
    public ActorMaterializer blockingActorMaterializer() {
        return blockingActorMaterializer;
    }

    @Bean(name = "ws-client-actor-materializer")
    public ActorMaterializer wsClientActorMaterializer() {
        return wsClientActorMaterializer;
    }

    @Bean
    public Executor globalExecutor() {
        return system.dispatcher();
    }

    @Bean
    public ExecutorService globalExecutorService() {
        if (globalExecutorRef.get() == null) {
            ExecutorService executorService = NamedExecutors.newFixedThreadPool(Integer.valueOf(threadCount), "GlobalExecutor");
            globalExecutorRef.compareAndSet(null, executorService);
        }
        return globalExecutorRef.get();
    }

    @Bean
    public WebMvcConfigurer reactiveWebMvcConfiguration() {
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
                returnValueHandlers.add(new ActionSupport.ActionReturnValueHandler(blockingActorMaterializer()));
            }
        };
    }
}