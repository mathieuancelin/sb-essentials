package org.reactivecouchbase.sbessentials.libs.actions;

import akka.stream.ActorMaterializer;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.concurrent.NamedExecutors;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.json.mapping.ThrowableWriter;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@Component
public class Actions {

    static WebApplicationContext webApplicationContext;

    static final Logger logger = LoggerFactory.getLogger(Actions.class);

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        Actions.webApplicationContext = webApplicationContext;
    }

    static ExecutorService executionContext() {
        return webApplicationContext.getBean(ExecutorService.class);
    }

    static ActorMaterializer materializer() {
        return webApplicationContext.getBean(ActorMaterializer.class);
    }

    // TODO : add global filters
    static final Action EMPTY = (request, block) -> {
        try {
            return block.apply(request);
        } catch (Exception e) {
            logger.error("Empty action error", e);
            return Future.successful(transformError(e, request));
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

    public static Result transformError(Throwable t, RequestContext request) {
        if (request != null) {
            // TODO : return the right representation based on accept
            Option<String> accept = request.header("Accept");
        }
        return Results.InternalServerError.json(Json.obj().with("error",
                new ThrowableWriter(true).write(t)));
    }
}
