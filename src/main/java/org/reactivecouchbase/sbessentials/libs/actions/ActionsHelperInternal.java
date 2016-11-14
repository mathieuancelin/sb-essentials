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
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ExecutorService;

@Component
public class ActionsHelperInternal {

    static WebApplicationContext webApplicationContext;

    static final Logger logger = LoggerFactory.getLogger(ActionsHelperInternal.class);

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        ActionsHelperInternal.webApplicationContext = webApplicationContext;
    }

    static ExecutorService executor() {
        return webApplicationContext.getBean("blocking-executor-service", ExecutorService.class);
    }

    static ActorMaterializer materializer() {
        return webApplicationContext.getBean("blocking-actor-materializer", ActorMaterializer.class);
    }

    // TODO : add global filters
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
