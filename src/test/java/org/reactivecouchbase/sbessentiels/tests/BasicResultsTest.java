package org.reactivecouchbase.sbessentiels.tests;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Traversable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Await;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.concurrent.Promise;
import org.reactivecouchbase.functional.Tuple;
import org.reactivecouchbase.json.JsObject;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.sbessentials.config.SBEssentialsConfig;
import org.reactivecouchbase.sbessentials.config.WebSocketConfig;
import org.reactivecouchbase.sbessentials.libs.result.InternalResultsHelper;
import org.reactivecouchbase.sbessentials.libs.websocket.ActorFlow;
import org.reactivecouchbase.sbessentials.libs.websocket.InternalWebsocketHelper;
import org.reactivecouchbase.sbessentials.libs.ws.InternalWSHelper;
import org.reactivecouchbase.sbessentials.libs.ws.WS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = {
    TestApplication.class,
    SBEssentialsConfig.class,
    WebSocketConfig.class,
    TestController.class,
    InternalResultsHelper.class,
    InternalResultsHelper.class,
    InternalWSHelper.class,
    InternalWebsocketHelper.class,
    InternalWebsocketHelper.class,
})
public class BasicResultsTest {

    private static final Duration MAX_AWAIT = Duration.parse("4s");

    @Autowired public WebApplicationContext ctx;
    @Autowired public ActorSystem actorSystem;
    @Autowired public ActorMaterializer actorMaterializer;

    @Test
    public void testTextResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/text").call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.body(),
                    r.headers()
                )
            ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("Hello World!\n", body._1);
        Assert.assertEquals("text/plain", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testPathParamResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/hello/Mathieu").call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.body(),
                    r.headers()
                )
            ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("Hello Mathieu!\n", body._1);
        Assert.assertEquals("text/plain", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testHugeTextResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/huge")
            .withHeader("Api-Key", "12345")
            .call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.body(),
                    r.headers()
                )
            ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals(TestController.VERY_HUGE_TEXT + "\n", body._1);
        Assert.assertEquals("text/plain", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testJsonResult() throws Exception {
        // Thread.sleep(Duration.of("10min").toMillis());
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/json")
            .withHeader("Api-Key", "12345")
            .call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.json(),
                    r.headers()
                )
            ));
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals(Json.obj().with("message", "Hello World!"), body._1);
        Assert.assertEquals("application/json", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testAsyncJsonResult() throws Exception {
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/ws")
            .withHeader("Api-Key", "12345")
            .withQueryParam("q", "81.246.24.51")
            .call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.json(),
                    r.headers()
                )
            ));
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        JsObject jsonBody = body._1.asObject();
        Assert.assertTrue(jsonBody.exists("latitude"));
        Assert.assertTrue(jsonBody.exists("longitude"));
        Assert.assertTrue(jsonBody.exists("ip"));
        Assert.assertTrue(jsonBody.exists("city"));
        Assert.assertTrue(jsonBody.exists("country_name"));
        Assert.assertEquals("application/json", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testAsyncJsonResult2() throws Exception {
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/ws2")
            .withHeader("Api-Key", "12345")
            .call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.json(),
                    r.headers()
                )
            ));
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        JsObject jsonBody = body._1.asObject();
        Assert.assertTrue(jsonBody.exists("latitude"));
        Assert.assertTrue(jsonBody.exists("longitude"));
        Assert.assertTrue(jsonBody.exists("ip"));
        Assert.assertTrue(jsonBody.exists("city"));
        Assert.assertTrue(jsonBody.exists("country_name"));
        Assert.assertEquals("application/json", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testPostJsonResult() throws Exception {
        String uuid = UUID.randomUUID().toString();
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/post")
            .withMethod(HttpMethods.POST)
            .withHeader("Api-Key", "12345")
            .withHeader("Content-Type", "application/json")
            .withBody(Json.obj().with("uuid", uuid))
            .call().flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.json(),
                    r.headers()
                )
            ));
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals(Json.obj().with("uuid", uuid).with("processed_by", "SB"), body._1);
        Assert.assertEquals("application/json", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testHtmlResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/html")
            .withHeader("Api-Key", "12345")
            .call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.body(),
                    r.headers()
                )
            ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("<h1>Hello World!</h1>", body._1);
        Assert.assertEquals("text/html", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testTemplateResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
                .withPath("/tests/template")
                .withHeader("Api-Key", "12345")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("<div><h1>Hello Mathieu!</h1></div>", body._1);
        Assert.assertEquals("text/html", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testSSEResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
            .withPath("/tests/sse")
            .withHeader("Api-Key", "12345")
            .call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.body(),
                    r.headers()
                )
            ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        java.util.List<JsObject> parts = Arrays.asList(body._1.split("\n"))
                .stream()
                .filter(s -> !s.trim().isEmpty())
                .map(s -> s.replace("data: ", ""))
                .map(s -> Json.parse(s).asObject())
                .collect(Collectors.toList());
        for (JsObject obj : parts) {
            Assert.assertTrue(obj.exists("value"));
            Assert.assertTrue(obj.exists("time"));
        }
        Assert.assertTrue(parts.size() < 7);
        Assert.assertEquals("text/event-stream", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testSSEResultWitActor() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001")
                .withPath("/tests/sse2")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        java.util.List<JsObject> parts = Arrays.asList(body._1.split("\n"))
                .stream()
                .filter(s -> !s.trim().isEmpty())
                .map(s -> s.replace("data: ", ""))
                .map(s -> Json.parse(s).asObject())
                .collect(Collectors.toList());
        for (JsObject obj : parts) {
            Assert.assertTrue(obj.exists("Hello"));
            Assert.assertEquals(obj.field("Hello").asString(), "World!");
        }
        Assert.assertEquals(3, parts.size());
        Assert.assertEquals("text/event-stream", body._2.get("X-Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
        Assert.assertEquals("chunked", body._2.get("X-Transfer-Encoding").flatMap(Traversable::headOption).getOrElse("none"));
    }


    @Test
    public void testWebsocketResult() throws Exception {
        final Sink<Message, CompletionStage<Message>> sink = Sink.head();
        final Source<Message, Cancellable> source = jsonSource(Json.obj().with("hello", "world"), 100);
        final Flow<Message, Message, CompletionStage<Message>> flow = Flow.fromSinkAndSourceMat(
            sink,
            source,
            Keep.left()
        );
        Future<JsObject> future = Future.from(WS.websocketHost("ws://localhost:7001")
            .addPathSegment("tests")
            .addPathSegment("websocket")
            .addPathSegment("Mathieu")
            .call(flow)
            .materialized()
                .thenApply(message -> {
                    System.out.println("Closed ...");
                    return Json.parse(message.asTextMessage().getStrictText()).asObject();
                }));
        JsObject jsonBody = Await.result(future, MAX_AWAIT);
        System.out.println(jsonBody.pretty());
        Assert.assertTrue(jsonBody.exists("sourceMessage"));
        Assert.assertEquals(Json.obj().with("hello", "world"), jsonBody.field("sourceMessage").asObject());
        Assert.assertTrue(jsonBody.exists("resource"));
        Assert.assertEquals("Mathieu", jsonBody.field("resource").asString());
        Assert.assertTrue(jsonBody.exists("sent_at"));
    }

    private Source<Message, Cancellable> jsonSource(JsValue value, long millis) {
        return Source.tick(FiniteDuration.Zero(), FiniteDuration.apply(millis, TimeUnit.MILLISECONDS), TextMessage.create(value.stringify()));
    }

    @Test
    public void testWebsocketExternal() throws Exception {
        final Sink<Message, CompletionStage<Message>> sink = Sink.head();
        final Source<Message, Cancellable> source = jsonSource(Json.obj().with("hello", "world"), 100);
        final Flow<Message, Message, CompletionStage<Message>> flow = Flow.fromSinkAndSourceMat(
            sink,
            source,
            Keep.left()
        );
        Future<JsObject> future = Future.from(WS.websocketHost("ws://echo.websocket.org/")
            .call(flow)
            .materialized()
            .thenApply(message -> {
                System.out.println("Closed ...");
                return Json.parse(message.asTextMessage().getStrictText()).asObject();
            }));
        JsObject jsonBody = Await.result(future, MAX_AWAIT);
        System.out.println(jsonBody.pretty());
        Assert.assertEquals(Json.obj().with("hello", "world"), jsonBody.asObject());
    }

    @Test
    public void testWebsocketPing() throws Exception {
        final Sink<Message, CompletionStage<Message>> sink = Sink.head();
        final Source<Message, Cancellable> source = jsonSource(Json.obj().with("hello", "world"), 100);
        final Flow<Message, Message, CompletionStage<Message>> flow = Flow.fromSinkAndSourceMat(
            sink,
            source,
            Keep.left()
        );
        Future<JsObject> future = Future.from(WS.websocketHost("ws://localhost:7001")
            .addPathSegment("tests")
            .addPathSegment("websocketping")
            .call(flow)
            .materialized()
            .thenApply(message -> {
                System.out.println("Closed ...");
                return Json.parse(message.asTextMessage().getStrictText()).asObject();
            }));
        JsObject jsonBody = Await.result(future, MAX_AWAIT);
        System.out.println(jsonBody.pretty());
        Assert.assertEquals(Json.obj().with("hello", "world"), jsonBody.asObject());
    }

    @Test
    public void testWebsocketPing2() throws Exception {
        Promise<List<Message>> promise = Promise.create();
        final Flow<Message, Message, NotUsed> flow =  ActorFlow.actorRef(
            out -> WebSocketClientActor.props(out, promise)
        );
        WS.websocketHost("ws://localhost:7001")
                .addPathSegment("tests")
                .addPathSegment("websocketping")
                .callNoMat(flow);
        List<String> messages = Await
                .result(promise.future(), MAX_AWAIT)
                .map(Message::asTextMessage)
                .map(TextMessage::getStrictText);
        System.out.println(messages.mkString(", "));
        Assert.assertEquals(List.of("chunk", "chunk", "chunk", "chunk", "chunk", "chunk", "chunk", "chunk", "chunk", "chunk"), messages);
    }

    @Test
    public void testWebsocketSimple() throws Exception {
        final Sink<Message, CompletionStage<Message>> sink = Sink.head();
        final Source<Message, Cancellable> source = jsonSource(Json.obj().with("hello", "world"), 100);
        final Flow<Message, Message, CompletionStage<Message>> flow = Flow.fromSinkAndSourceMat(
            sink,
            source,
            Keep.left()
        );
        Future<JsObject> future = Future.from(WS.websocketHost("ws://localhost:7001")
            .addPathSegment("tests")
            .addPathSegment("websocketsimple")
            .call(flow)
            .materialized()
            .thenApply(message -> {
                System.out.println("Closed ...");
                return Json.parse(message.asTextMessage().getStrictText()).asObject();
            }));
        JsObject jsonBody = Await.result(future, MAX_AWAIT);
        System.out.println(jsonBody.pretty());
        Assert.assertEquals(Json.obj().with("msg", "Hello World!"), jsonBody.asObject());
    }

}
