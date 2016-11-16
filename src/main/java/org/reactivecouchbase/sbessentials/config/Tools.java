package org.reactivecouchbase.sbessentials.config;

import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Option;
import org.reactivecouchbase.json.Throwables;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Tools {

    public static <T> Future<T> fromJdkCompletableFuture(ExecutorService ec, java.util.concurrent.CompletableFuture<T> completableFuture) {
        Promise<T> p = Promise.make(ec);
        completableFuture.whenComplete((value, exception) -> {
            if (value != null) {
                p.trySuccess(value);
            } else {
                p.tryFailure(exception);
            }
        });
        return p.future();
    }

    public static <T> Future<T> fromJdkCompletionStage(java.util.concurrent.CompletionStage<T> completableFuture) {
        Promise<T> p = Promise.make();
        completableFuture.whenComplete((value, exception) -> {
            if (value != null) {
                p.trySuccess(value);
            } else {
                p.tryFailure(exception);
            }
        });
        return p.future();
    }

    public static <T> Future<T> fromJdkCompletableFuture(java.util.concurrent.CompletableFuture<T> completableFuture) {
       return fromJdkCompletionStage(completableFuture);
    }

    public static String fromInputStream(InputStream input) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> T await(Future<T> fu) {
        fu.await();
        return fu.get();
    }

    public static <T, U> U fold(Option<T> option, Supplier<U> onNone, Function<T, U> onSome) {
        if (option.isDefined()) {
            return onSome.apply(option.get());
        } else {
            return onNone.get();
        }
    }
}
