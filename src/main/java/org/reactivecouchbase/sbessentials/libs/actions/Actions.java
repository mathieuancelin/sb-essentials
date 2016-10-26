package org.reactivecouchbase.sbessentials.libs.actions;

import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.concurrent.NamedExecutors;
import org.reactivecouchbase.sbessentials.libs.status.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@Component
public class Actions {

    static WebApplicationContext webApplicationContext;

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        Actions.webApplicationContext = webApplicationContext;
    }

    public static final ExecutorService EXECUTOR_SERVICE =
            NamedExecutors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, "ActionsExecutor");

    // TODO : add global filters
    static final Action EMPTY = (request, block) -> {
        try {
            return block.apply(request);
        } catch (Exception e) {
            e.printStackTrace();
            return Future.failed(e);
        }
    };

    public static Future<Result> sync(Function<RequestContext, Result> block) {
        return EMPTY.sync(block);
    }

    public static Future<Result> sync(ExecutorService ec, Function<RequestContext, Result> block) {
        return EMPTY.sync(ec, block);
    }

    public static Future<Result> async(Function<RequestContext, Future<Result>> block) {
        return EMPTY.async(block);
    }

    public static Future<Result> async(ExecutorService ec, Function<RequestContext, Future<Result>> block) {
        return EMPTY.async(ec, block);
    }
}
