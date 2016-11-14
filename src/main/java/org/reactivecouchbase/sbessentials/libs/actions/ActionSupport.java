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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
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

public class ActionSupport {

    public static class ActionReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

        private final ActorMaterializer materializer;

        public ActionReturnValueHandler(ActorMaterializer materializer) {
            this.materializer = materializer;
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
                            new ActionDeferredResult(action, response, materializer), mavContainer);
        }
    }

    static class ActionDeferredResult extends DeferredResult<ResponseBodyEmitter> {

        public ActionDeferredResult(Action action, HttpServletResponse response, ActorMaterializer materializer) {
            super(null, new Object());
            Assert.notNull(action, "Action cannot be null");
            action.run().andThen(ttry -> {
                for (Result result : ttry.asSuccess()) {

                    result.cookies.forEach(response::addCookie);
                    SourceResponseBodyEmmitter rbe = new SourceResponseBodyEmmitter(result);
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
            }, action.ec);
        }
    }

    private static class SourceResponseBodyEmmitter extends ResponseBodyEmitter {

        private final Result result;

        public SourceResponseBodyEmmitter(Result result) {
            this.result = result;
        }

        @Override
        protected void extendResponse(ServerHttpResponse response) {
            super.extendResponse(response);

            HttpHeaders headers = response.getHeaders();
            for (Map.Entry<String, List<String>> entry : result.headers.toJavaMap().entrySet()) {
                for (String value : entry.getValue()) {
                    headers.add(entry.getKey(), value);
                }
            }
            response.setStatusCode(HttpStatus.valueOf(result.status));
            headers.setContentType(MediaType.valueOf(result.contentType));
            headers.add("X-Content-Type", result.contentType);
            headers.add("Content-Type", result.contentType);
            // it seems to appear in double
            headers.add("X-Transfer-Encoding", "chunked");
        }
    }
}
