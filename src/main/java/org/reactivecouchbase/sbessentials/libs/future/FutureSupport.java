package org.reactivecouchbase.sbessentials.libs.future;

import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import javaslang.collection.List;
import org.reactivecouchbase.concurrent.Future;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FutureSupport {

    public static class FutureReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

        private final ActorMaterializer materializer;

        public FutureReturnValueHandler(ActorSystem system) {
            this.materializer = ActorMaterializer.create(system);
        }

        @Override
        public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
            return returnValue != null && supportsReturnType(returnType);
        }

        @Override
        public boolean supportsReturnType(MethodParameter returnType) {
            return Future.class.isAssignableFrom(returnType.getParameterType())
                    && returnType.getGenericParameterType().toString().equalsIgnoreCase("org.reactivecouchbase.concurrent.Future<org.reactivecouchbase.sbessentials.libs.result.Result>");
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
            if (returnValue == null) {
                mavContainer.setRequestHandled(true);
                return;
            }
            final Future<Result> future = Future.class.cast(returnValue);
            final HttpServletResponse response = (HttpServletResponse) webRequest.getNativeResponse();
            WebAsyncUtils.getAsyncManager(webRequest)
                 .startDeferredResultProcessing(
                        new FutureDeferredResult(future, response, materializer), mavContainer);
        }
    }

    public static class FutureDeferredResult extends DeferredResult<ResponseBodyEmitter> {
        public FutureDeferredResult(Future<Result> future, HttpServletResponse response, ActorMaterializer materializer) {
            super(null, new Object());
            Assert.notNull(future, "Future cannot be null");
            future.andThen(ttry -> {
                for (Result result : ttry.asSuccess()) {
                    for (Map.Entry<String, List<String>> entry : result.headers.toJavaMap().entrySet()) {
                        for (String value : entry.getValue()) {
                            response.setHeader(entry.getKey(), value);
                        }
                    }
                    result.cookies.forEach(response::addCookie);
                    response.setStatus(result.status);
                    response.setContentType(result.contentType);
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
