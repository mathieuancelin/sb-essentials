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

    default Future<Result> sync(Function<RequestContext, Result> block) {
        return sync(Actions.EXECUTOR_SERVICE, block);
    }

    default Future<Result> sync(ExecutorService ec, Function<RequestContext, Result> block) {
        return async(ec, req -> Future.async(() -> block.apply(req), ec));
    }

    default Future<Result> async(Function<RequestContext, Future<Result>> block) {
        return async(Actions.EXECUTOR_SERVICE, block);
    }

    default Future<Result> async(ExecutorService ec, Function<RequestContext, Future<Result>> block) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();
            HttpServletResponse response = servletRequestAttributes.getResponse();
            RequestContext rc = new RequestContext(HashMap.empty(), Actions.webApplicationContext, request, response);
            return Future.async(() -> invoke(rc, block), ec).flatMap(e -> e, ec);
        } else {
            return Future.failed(new RuntimeException("RequestAttributes is not an instance of "));
        }
    }

    default Action combine(Action other) {
        Action that = this;
        return (request, block) -> that.invoke(request, r1 -> other.invoke(r1, block));
    }

    default Action andThen(Action other) {
        return combine(other);
    }
}