package org.reactivecouchbase.sbessentials.libs.actions;

import akka.stream.ActorMaterializer;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.json.mapping.ThrowableWriter;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ExecutorService;

@Component
class InternalActionsHelper {

    static final Logger logger = LoggerFactory.getLogger(InternalActionsHelper.class);

    private static ActorMaterializer actorMaterializer;

    private static ExecutorService executorService;

    private static WebApplicationContext webApplicationContext;

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        InternalActionsHelper.webApplicationContext = webApplicationContext;
    }

    @Autowired @Qualifier("blocking-executor-service")
    public void setWSExecutorService(ExecutorService executorService) {
        InternalActionsHelper.executorService = executorService;
    }

    @Autowired @Qualifier("blocking-actor-materializer")
    public void setWSActorMaterializer(ActorMaterializer actorMaterializer) {
        InternalActionsHelper.actorMaterializer = actorMaterializer;
    }

    static ExecutorService executor() {
        return executorService;
    }

    static ActorMaterializer materializer() {
        return actorMaterializer;
    }

    static WebApplicationContext webApplicationContext() {
        return webApplicationContext;
    }

    static final ActionStep EMPTY = (request, block) -> {
        try {
            return block.apply(request);
        } catch (Exception e) {
            logger.error("Empty action error", e);
            return Future.successful(transformError(e, request));
        }
    };

    static Result transformError(Throwable t, RequestContext request) {
        // always return JSON for now
        return Results.InternalServerError.json(Json.obj().with("error",
                new ThrowableWriter(true).write(t)));
    }
}
