package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.HashMap;
import javaslang.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public interface ActionStep {

    Future<Result> invoke(RequestContext request, Function<RequestContext, Future<Result>> block);

    default Future<Result> innerInvoke(RequestContext request, Function<RequestContext, Future<Result>> block) {
        try {
            return this.invoke(request, block);
        } catch (Exception e) {
            InternalActionsHelper.logger.error("innerInvoke action error", e);
            return Future.successful(InternalActionsHelper.transformError(e, request));
        }
    }

    default Action sync(Function<RequestContext, Result> block) {
        return async(req -> Future.of(InternalActionsHelper.executor(), () -> {
            try {
                return block.apply(req);
            } catch (Exception e) {
                InternalActionsHelper.logger.error("Sync action error", e);
                return InternalActionsHelper.transformError(e, req);
            }
        }));
    }

    default Action async(Function<RequestContext, Future<Result>> block) {
        return async(InternalActionsHelper.executor(), block);
    }

    default Action async(ExecutorService ec, Function<RequestContext, Future<Result>> block) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();
            HttpServletResponse response = servletRequestAttributes.getResponse();
            RequestContext rc = new RequestContext(HashMap.empty(), InternalActionsHelper.webApplicationContext, request, response, ec);
            return new Action(this, rc, block, ec);
        } else {
            return new Action(this, null, rc ->
                Future.successful(InternalActionsHelper.transformError(new RuntimeException("RequestAttributes is not an instance of "), null))
            , ec);
        }
    }

    default ActionStep combine(ActionStep other) {
        ActionStep that = this;
        return (request, block) -> that.innerInvoke(request, r1 -> other.innerInvoke(r1, block));
    }

    default ActionStep andThen(ActionStep other) {
        return combine(other);
    }
}
