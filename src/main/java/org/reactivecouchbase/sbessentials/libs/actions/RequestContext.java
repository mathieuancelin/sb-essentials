package org.reactivecouchbase.sbessentials.libs.actions;

import akka.http.scaladsl.coding.Gzip$;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.AsPublisher;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.sbessentials.libs.config.Configuration;
import org.reactivestreams.Publisher;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

public class RequestContext {

    private final HashMap<String, Object> state;

    private final WebApplicationContext applicationContext;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final ExecutorService ec;

    private final RequestHeaders headers;

    private final RequestQueryParams queryParams;

    private final RequestCookies cookies;

    private final RequestPathParams pathParams;

    private final Configuration configuration;

    public RequestContext(HashMap<String, Object> state, WebApplicationContext applicationContext, HttpServletRequest request, HttpServletResponse response, ExecutorService ec) {
        this.state = state;
        this.applicationContext = applicationContext;
        this.request = request;
        this.response = response;
        this.headers = new RequestHeaders(request);
        this.queryParams = new RequestQueryParams(request);
        this.cookies = new RequestCookies(request);
        this.pathParams = new RequestPathParams(request);
        this.ec = ec;
        this.configuration = applicationContext.getBean(Configuration.class);
    }

    public ExecutorService currentExecutor() {
        return ec;
    }

    public <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public <T> T getBean(Class<T> clazz, String name){
        return applicationContext.getBean(clazz, name);
    }

    public Object getValue(String key) {
        return this.state.get(key);
    }

    public <T> T getValue(String key, Class<T> clazz) {
        return this.state.get(key).map(clazz::cast).get();
    }

    public RequestContext setValue(String key, Object value) {
        if(key == null || value == null) {
            return this;
        } else {
            return new RequestContext(state.put(key, value), applicationContext, request, response, ec);
        }
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public Future<RequestBody> body() {
        return body(InternalActionsHelper.executor());
    }

    public Future<RequestBody> body(ExecutorService ec) {
        ActorMaterializer materializer = InternalActionsHelper.materializer();
        return Future.fromJdkCompletableFuture(
            bodyAsStream().runFold(ByteString.empty(), ByteString::concat, materializer).toCompletableFuture()
        ).map(RequestBody::new, ec);
    }

    public <T> Future<T> body(BiFunction<RequestHeaders, Source<ByteString, ?>, Future<T>> bodyParser) {
        return bodyParser.apply(headers, bodyAsStream());
    }

    public <T> Future<T> body(BiFunction<RequestHeaders, Publisher<ByteString>, Future<T>> bodyParser, AsPublisher asPublisher) {
        return bodyParser.apply(headers, bodyAsPublisher(asPublisher));
    }

    public Source<ByteString, ?> bodyAsStream() {
        if (header("Content-Encoding").getOrElse("none").equalsIgnoreCase("gzip")) {
            return rawBodyAsStream().via(Gzip$.MODULE$.decoderFlow());
        }
        return rawBodyAsStream();
    }

    public Source<ByteString, ?> rawBodyAsStream() {
        return StreamConverters.fromInputStream(() -> getRequest().getInputStream());
    }

    public Publisher<ByteString> bodyAsPublisher(AsPublisher asPublisher) {
        ActorMaterializer materializer = InternalActionsHelper.materializer();
        return bodyAsStream().runWith(Sink.asPublisher(asPublisher), materializer);
    }

    public Publisher<ByteString> rawBodyAsPublisher(AsPublisher asPublisher) {
        ActorMaterializer materializer = InternalActionsHelper.materializer();
        return rawBodyAsStream().runWith(Sink.asPublisher(asPublisher), materializer);
    }

    public Option<String> header(String name) {
        return Option.apply(request.getHeader(name));
    }

    public RequestHeaders headers() {
        return headers;
    }

    public RequestQueryParams queryParams() {
        return queryParams;
    }

    public Option<String> queryParam(String name) {
        return queryParams.param(name);
    }

    public RequestCookies cookies() {
        return cookies;
    }

    public Option<Cookie> cookie(String name) {
        return cookies.cookie(name);
    }

    public RequestPathParams pathParams() {
        return pathParams;
    }

    public Option<String> pathParam(String name) {
        return pathParams.param(name);
    }

    public Configuration configuration() {
        return configuration;
    }
}
