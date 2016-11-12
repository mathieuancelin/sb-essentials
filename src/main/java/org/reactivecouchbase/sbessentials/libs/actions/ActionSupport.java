package org.reactivecouchbase.sbessentials.libs.actions;

import akka.Done;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.List;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class ActionSupport {

    public static class ActionReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

        private final ActorMaterializer materializer;
        private final ExecutorService ec;

        public ActionReturnValueHandler(ExecutorService ec, ActorMaterializer materializer) {
            this.materializer = materializer;
            this.ec = ec;
        }

        @Override
        public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
            return returnValue != null && supportsReturnType(returnType);
        }

        @Override
        public boolean supportsReturnType(MethodParameter returnType) {
            return Action.class.isAssignableFrom(returnType.getParameterType());
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
            if (returnValue == null) {
                mavContainer.setRequestHandled(true);
                return;
            }
            final Action action = Action.class.cast(returnValue);
            final HttpServletResponse response = (HttpServletResponse) webRequest.getNativeResponse();
            WebAsyncUtils.getAsyncManager(webRequest)
                    .startDeferredResultProcessing(
                            new ActionDeferredResult(action, ec, response, materializer), mavContainer);
        }
    }

    public static class ActionDeferredResult extends DeferredResult<ResponseBodyEmitter> {

        public ActionDeferredResult(Action action, ExecutorService ec, HttpServletResponse response, ActorMaterializer materializer) {
            super(null, new Object());
            Assert.notNull(action, "Action cannot be null");
            action.run(ec).andThen(ttry -> {
                for (Result result : ttry.asSuccess()) {
                    for (Map.Entry<String, List<String>> entry : result.headers.toJavaMap().entrySet()) {
                        for (String value : entry.getValue()) {
                            // System.out.println("sent header : " + entry.getKey() + " :: " + value);
                            response.setHeader(entry.getKey(), value);
                        }
                    }
                    result.cookies.forEach(response::addCookie);
                    response.setStatus(result.status);
                    response.setContentType(result.contentType);
                    response.setHeader("Content-Type", result.contentType);
                    response.setHeader("X-Content-Type", result.contentType);
                    response.setHeader("Transfer-Encoding", "chunked");
                    ResponseBodyEmitter rbe = new ResponseBodyEmitter();
                    this.setResult(rbe);

                    Source<ByteString, ?> source = result.source;
                    Pair<?, CompletionStage<Done>> run = source.toMat(Sink.foreach(byteString -> {
                        rbe.send(byteString.toArray(), MediaType.parseMediaType(result.contentType));
                    }), Keep.both()).run(materializer);

                    result.materializedValue.trySuccess(run.first());

                    run.second().whenComplete((success, error) -> {
                        if (success != null) {
                            rbe.complete();
                        } else {
                            rbe.completeWithError(error);
                        }
                    });
                }
                for (Throwable t : ttry.asFailure()) {
                    this.setErrorResult(t);
                }
            });
        }
    }
}
