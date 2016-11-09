package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.HashMap;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public interface Action {

    Future<Result> invoke(RequestContext request, Function<RequestContext, Future<Result>> block);

    default Future<Result> innerInvoke(RequestContext request, Function<RequestContext, Future<Result>> block) {
        try {
            return this.invoke(request, block);
        } catch (Exception e) {
            Actions.logger.error("innerInvoke action error", e);
            return Future.successful(Actions.transformError(e, request));
        }
    }

    default Future<Result> sync(Function<RequestContext, Result> block) {
        return sync(Actions.executionContext(), block);
    }

    default Future<Result> sync(ExecutorService ec, Function<RequestContext, Result> block) {
        return async(ec, req -> Future.async(() -> {
            try {
                return block.apply(req);
            } catch (Exception e) {
                Actions.logger.error("Sync action error", e);
                return Actions.transformError(e, req);
            }
        }, ec));
    }

    default Future<Result> async(Function<RequestContext, Future<Result>> block) {
        return async(Actions.executionContext(), block);
    }

    default Future<Result> async(ExecutorService ec, Function<RequestContext, Future<Result>> block) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();
            HttpServletResponse response = servletRequestAttributes.getResponse();
            RequestContext rc = new RequestContext(HashMap.empty(), Actions.webApplicationContext, request, response);
            return Future.async(() -> innerInvoke(rc, block), ec).flatMap(e -> e, ec).recoverWith(t ->
                Future.successful(Actions.transformError(t, rc))
            , Actions.executionContext());
        } else {
            return Future.successful(Actions.transformError(new RuntimeException("RequestAttributes is not an instance of "), null));
        }
    }

    default Action combine(Action other) {
        Action that = this;
        return (request, block) -> that.innerInvoke(request, r1 -> other.innerInvoke(r1, block));
    }

    default Action andThen(Action other) {
        return combine(other);
    }
}