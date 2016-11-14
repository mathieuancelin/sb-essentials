package org.reactivecouchbase.sbessentials.config;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Promise;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Configuration
public class SBEssentialsConfig {

    private final Config config = ConfigFactory.load();
    private final org.reactivecouchbase.sbessentials.config.Configuration configuration =
            new org.reactivecouchbase.sbessentials.config.Configuration(config);

    private final ActorSystem system = ActorSystem.create("global-system",
            config.atPath("systems.global").withFallback(ConfigFactory.empty()));
    private final ActorMaterializer generalPurposeMaterializer = ActorMaterializer.create(system);
    private final FakeExecutorService globalExecutor = new FakeExecutorService(system.dispatcher());

    private final ActorSystem wsSystem = ActorSystem.create("ws-system",
            config.atPath("systems.ws").withFallback(ConfigFactory.empty()));
    private final ActorMaterializer wsClientActorMaterializer = ActorMaterializer.create(wsSystem);
    private final FakeExecutorService wsExecutor = new FakeExecutorService(wsSystem.dispatcher());

    private final ActorSystem blockingSystem = ActorSystem.create("blocking-system",
            config.atPath("systems.blocking").withFallback(ConfigFactory.empty()));
    private final ActorMaterializer blockingActorMaterializer = ActorMaterializer.create(blockingSystem);
    private final FakeExecutorService blockingExecutor = new FakeExecutorService(blockingSystem.dispatcher());

    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            system.shutdown();
            wsSystem.shutdown();
            blockingSystem.shutdown();
        }));
    }

    @Value("${app.config.async.timeout}")
    public String timeoutDuration;

    @Bean
    public Config rawConfig() {
        return config;
    }

    @Bean
    public org.reactivecouchbase.sbessentials.config.Configuration configuration() {
        return configuration;
    }

    @Bean
    public ActorSystem actorSystem() {
        return system;
    }

    @Bean
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

    @Bean(name = "blocking-executor")
    public Executor blockingExecutor() {
        return blockingSystem.dispatcher();
    }

    @Bean(name = "ws-executor")
    public Executor wsExecutor() {
        return wsSystem.dispatcher();
    }

    @Bean
    public ExecutorService globalExecutorService() {
        return globalExecutor;
    }

    @Bean(name = "blocking-executor-service")
    public ExecutorService blockingExecutorService() {
        return blockingExecutor;
    }

    @Bean(name = "ws-executor-service")
    public ExecutorService wsExecutorService() {
        return wsExecutor;
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

    private static class FakeExecutorService implements ExecutorService {

        private final Executor executor;

        public FakeExecutorService(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void shutdown() {

        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            Promise<T> promise = Promise.create();
            execute(() -> {
                try {
                    promise.trySuccess(task.call());
                } catch (Exception e) {
                    promise.tryFailure(e);
                }
            });
            return promise.future().toJdkFuture();
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            Promise<T> promise = Promise.create();
            execute(() -> {
                try {
                    task.run();
                    promise.trySuccess(result);
                } catch (Exception e) {
                    promise.tryFailure(e);
                }
            });
            return promise.future().toJdkFuture();
        }

        @Override
        public Future<?> submit(Runnable task) {
            Promise<Object> promise = Promise.create();
            execute(() -> {
                try {
                    task.run();
                    promise.trySuccess(null);
                } catch (Exception e) {
                    promise.tryFailure(e);
                }
            });
            return promise.future().toJdkFuture();
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            throw new RuntimeException("Not Supported Yet !!!");
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            throw new RuntimeException("Not Supported Yet !!!");
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            throw new RuntimeException("Not Supported Yet !!!");
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new RuntimeException("Not Supported Yet !!!");
        }

        @Override
        public void execute(Runnable command) {
            executor.execute(command);
        }
    }
}

// private final AtomicReference<ExecutorService> globalExecutorRef = new AtomicReference<>(null);
// @Value("${app.config.async.globalec.threadcount}")
// public String threadCount;
// @Bean
// public ExecutorService globalExecutorService() {
//     if (globalExecutorRef.get() == null) {
//         ExecutorService executorService = NamedExecutors.newFixedThreadPool(Integer.valueOf(threadCount), "GlobalExecutor");
//         globalExecutorRef.compareAndSet(null, executorService);
//     }
//     return globalExecutorRef.get();
// }