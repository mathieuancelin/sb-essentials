package org.reactivecouchbase.sbessentiels.tests;

import akka.Done;
import akka.NotUsed;
import akka.actor.*;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.actor.AbstractActorPublisher;
import akka.stream.actor.ActorPublisherMessage;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Traversable;
import org.junit.Assert;
import org.junit.Before;
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
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.actions.ActionStep;
import org.reactivecouchbase.sbessentials.libs.actions.InternalActionsHelper;
import org.reactivecouchbase.sbessentials.libs.result.InternalResultsHelper;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import org.reactivecouchbase.sbessentials.libs.websocket.*;
import org.reactivecouchbase.sbessentials.libs.ws.InternalWSHelper;
import org.reactivecouchbase.sbessentials.libs.ws.WS;
import org.reactivecouchbase.sbessentials.libs.ws.WSResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static akka.pattern.PatternsCS.after;
import static org.reactivecouchbase.sbessentials.libs.result.Results.BadRequest;
import static org.reactivecouchbase.sbessentials.libs.result.Results.Ok;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = { BasicResultsTest.Application.class, SBEssentialsConfig.class, BasicResultsTest.TestController.class })
public class BasicResultsTest {

    private static final Duration MAX_AWAIT = Duration.parse("4s");

    @Autowired public WebApplicationContext ctx;
    @Autowired public ActorSystem actorSystem;
    @Autowired public ActorMaterializer actorMaterializer;

    @Before
    public void injectStaticStuff() {
        new InternalActionsHelper().setWebApplicationContext(ctx);
        new InternalResultsHelper().setWebApplicationContext(ctx);
        new InternalWSHelper().setWebApplicationContext(ctx);
        new InternalWebsocketHelper().setActorMaterializer(actorMaterializer);
        new InternalWebsocketHelper().setActorSystem(actorSystem);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testTextResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001").withPath("/tests/text").call()
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
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:7001").withPath("/tests/hello/Mathieu").call()
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
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RestController
    @RequestMapping("/tests")
    public static class TestController {

        private final static Logger logger = LoggerFactory.getLogger(TestController.class);

        @Autowired ActorSystem actorSystem;
        @Autowired ActorMaterializer materializer;

        private static ActionStep ApiKeyCheck = (req, block) -> req.header("Api-Key").orElse(req.queryParam("Api-Key")).fold(
            () -> {
                logger.info("No API KEY provided");
                return Future.successful(BadRequest.json(Json.obj().with("error", "No API KEY provided")));
            },
            (value) -> {
                if (value.equalsIgnoreCase("12345")) {
                    return block.apply(req);
                } else {
                    logger.info("Bad API KEY provided {}", value);
                    return Future.successful(BadRequest.json(Json.obj().with("error", "Bad API KEY")));
                }
            }
        );

        private static ActionStep LogBefore = (req, block) -> {
            Long start = System.currentTimeMillis();
            logger.info("[Log] before action -> {}", req.getRequest().getRequestURI());
            return block.apply(req.setValue("start", start));
        };

        private static ActionStep LogAfter = (req, block) -> block.apply(req).andThen(ttry -> {
            logger.info(
                    "[Log] after action -> {} : took {}",
                    req.getRequest().getRequestURI(),
                    Duration.of(System.currentTimeMillis() - req.getValue("start", Long.class), TimeUnit.MILLISECONDS).toHumanReadable()
            , req.currentExecutor());
        });

        private static ActionStep Throttle(int limit, long perMillis) {
            AtomicLong next = new AtomicLong(System.currentTimeMillis());
            AtomicLong counter = new AtomicLong(0L);
            return (request, block) -> {
                if (System.currentTimeMillis() > next.get()) {
                    next.set(System.currentTimeMillis() + perMillis);
                    counter.set(0L);
                }
                if (counter.get() == limit) {
                    logger.info("Too much call for {}", request.getRequest().getRequestURI());
                    return Future.successful(BadRequest.json(Json.obj().with("error", "too much calls")));
                }
                counter.incrementAndGet();
                return block.apply(request);
            };
        }

        private static ActionStep ApiManagedAction = LogBefore
                .andThen(ApiKeyCheck)
                .andThen(Throttle(2, 100))
                .andThen(LogAfter);

        @RequestMapping(method = RequestMethod.GET, path = "/sse")
        public Action testStream() {
            return Action.sync(ctx -> {

                Result result = Ok.stream(
                    Source.tick(
                        FiniteDuration.apply(0, TimeUnit.MILLISECONDS),
                        FiniteDuration.apply(100, TimeUnit.MILLISECONDS),
                        ""
                    )
                    .map(l -> Json.obj().with("time", System.currentTimeMillis()).with("value", l))
                    .map(Json::stringify)
                    .map(j -> "data: " + j + "\n\n")
                ).as("text/event-stream");

                result.materializedValue(Cancellable.class).andThen(ttry -> {
                    for (Cancellable c : ttry.asSuccess()) {
                        after(
                                FiniteDuration.create(500, TimeUnit.MILLISECONDS),
                                actorSystem.scheduler(),
                                actorSystem.dispatcher(),
                                CompletableFuture.completedFuture(Done.getInstance())
                        ).thenAccept(d ->
                                c.cancel()
                        );
                    }
                });

                return result;
            });
        }

        @RequestMapping(method = RequestMethod.GET, path = "/sse2")
        public Action testStream2() {
            return Action.sync(ctx -> {
                Result result = Ok.stream(SSEActor.source()).as("text/event-stream");
                result.materializedValue(ActorRef.class).andThen(ttry -> {
                   for (ActorRef ref : ttry.asSuccess()) {
                       ref.tell("START", ActorRef.noSender());
                   }
                });
                return result;
            });
        }

        @GetMapping("/text")
        public Action text() {
            return Action.sync(ctx ->
                Ok.text("Hello World!\n")
            );
        }

        @GetMapping("/hello/{name}")
        public Action hello() {
            return Action.sync(ctx ->
                    Ok.text("Hello " + ctx.pathParam("name").getOrElse("World") + "!\n")
            );
        }

        @GetMapping("/huge")
        public Action hugeText() {
            return ApiManagedAction.sync(ctx ->
                Ok.text(VERY_HUGE_TEXT + "\n")
            );
        }

        @GetMapping("/json")
        public Action json() {
            return ApiManagedAction.sync(ctx ->
                Ok.json(Json.obj().with("message", "Hello World!"))
            );
        }

        @GetMapping("/html")
        public Action html() {
            return ApiManagedAction.sync(ctx ->
                Ok.html("<h1>Hello World!</h1>")
            );
        }

        @GetMapping("/template")
        public Action template() {
            return ApiManagedAction.sync(ctx ->
                Ok.template("hello", HashMap.<String, String>empty().put("name", "Mathieu"))
            );
        }

        @PostMapping("/post")
        public Action testPost() {
            return ApiManagedAction.async(ctx ->
                ctx.body()
                    .map(body -> body.asJson().asObject())
                .map(payload -> payload.with("processed_by", "SB"))
                .map(Ok::json)
            );
        }

        @GetMapping("/ws")
        public Action testWS() {
            return ApiManagedAction.async(ctx ->
                WS.host("http://freegeoip.net").withPath("/json/")
                    .call()
                    .flatMap(WSResponse::body)
                    .map(r -> r.json().pretty())
                    .map(p -> Ok.json(p))
            );
        }

        @GetMapping("/ws2")
        public Action testWS2() {
            return ApiManagedAction.async(ctx ->
                WS.host("http://freegeoip.net")
                    .withPath("/json/")
                    .withHeader("Sent-At", System.currentTimeMillis() + "")
                    .call()
                    .flatMap(WSResponse::body)
                    .map(r -> r.json().pretty())
                    .map(p -> Ok.json(p))
            );
        }

        @GetMapping("/download")
        public Action testBigDownload() {
            return Action.async(ctx ->
                WS.host("http://releases.ubuntu.com")
                    .addPathSegment("16.04.1")
                    .addPathSegment("ubuntu-16.04.1-desktop-amd64.iso")
                    .withHeader("From", "SB")
                    .call()
                    .map(r -> Ok.chunked(r.bodyAsStream()).as("application/octet-stream"))
            );
        }

        @GetMapping("/bad")
        public Action testBigBadDownload() {
            return Action.async(ctx ->
                WS.host("http://releases.ubuntu.com")
                    .withPath("/16.04.1/ubuntu-16.04.1-desktop-amd64.iso")
                    .withHeader("From", "SB")
                    .call()
                    .flatMap(r -> r.body())
                    .map(r -> Ok.binary(r.bytes()))
            );
        }

        @GetMapping("/wsui")
        public Action testWebsocket() {
            return Action.sync(ctx ->
                Ok.html("<html><body><script>\n"
                    + "window.ws = new WebSocket('ws://localhost:7001/tests/websocketsimple');\n"
                    + "window.ws.onopen = function(e) { \n"
                    + "  console.log('open', e);\n"
                    + "  setTimeout(function() {\n"
                    + "    window.ws.send(JSON.stringify({ 'yo': 'bitch' }));\n"
                    + "  }, 100);\n"
                    + "};\n"
                    + "window.ws.onclose = function(e) { console.log('onclose', e) };\n"
                    + "window.ws.onmessage = function(e) { console.log('message', e.data) };\n"
                    + "\n"
                    + "</script></body></html>")
            );
        }

        @WebSocketMapping(path = "/websocketsimple")
        public WebSocket simpleWebsocket() {
            return WebSocket.accept(ctx ->
                Flow.fromSinkAndSource(
                    Sink.foreach(msg -> logger.info(msg.getPayload().toString())),
                    Source.tick(
                        FiniteDuration.Zero(),
                        FiniteDuration.create(10, TimeUnit.MILLISECONDS),
                        new org.springframework.web.socket.TextMessage(Json.obj().with("msg", "Hello World!").stringify())
                    )
                )
            );
        }

        @WebSocketMapping(path = "/websocketping")
        public WebSocket webSocketPing() {
            return WebSocket.accept(context ->
                ActorFlow.actorRef(
                    out -> WebsocketPing.props(context, out)
                )
            );
        }

        @WebSocketMapping(path = "/websocket/{id}")
        public WebSocket webSocketWithContext() {
            return WebSocket.accept(context ->
                ActorFlow.actorRef(
                    out -> MyWebSocketActor.props(context, out)
                )
            );
        }

        private final static String HUGE_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum rhoncus ultrices neque, nec consectetur ex molestie et. Integer dolor purus, laoreet vel condimentum vel, pulvinar at augue. Quisque tempor ac nisl vitae faucibus. Nunc placerat lacus dolor, nec finibus nibh semper eget. Nullam ac ipsum egestas, porttitor leo eget, suscipit risus. Donec sit amet est at erat pellentesque condimentum eu quis mauris. Aliquam tristique consectetur neque, a euismod magna mattis in. Nullam ac orci lectus. Interdum et malesuada fames ac ante ipsum primis in faucibus. Curabitur iaculis, mauris non tempus sagittis, eros nisl maximus quam, sed euismod sapien est id nisl. Nulla vitae enim dictum, tincidunt lorem nec, posuere arcu. Nulla tempus elit eu magna euismod maximus. Morbi varius nulla velit, eget pulvinar augue gravida eu.\n" +
                "Curabitur enim nisl, sollicitudin at odio laoreet, finibus gravida tellus. Nulla auctor urna magna, non egestas eros dignissim sollicitudin. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Nullam eget magna sit amet magna venenatis consequat vel vel lectus. Morbi fringilla pulvinar diam sed fermentum. Praesent ac tincidunt urna. Praesent in mi dolor. Curabitur posuere massa quis lectus fringilla, at congue ante faucibus. Mauris massa lacus, egestas quis consequat ac, pretium quis arcu. Fusce placerat vel massa eu blandit.\n" +
                "Curabitur fermentum, ante a tristique interdum, enim diam pulvinar urna, nec aliquet tellus lectus id lectus. Integer ullamcorper lacinia est vulputate pretium. In a dictum velit. In mattis justo sollicitudin iaculis iaculis. Quisque suscipit lorem vel felis accumsan, quis lobortis diam imperdiet. Nullam ornare metus massa, rutrum ullamcorper metus scelerisque a. Nullam finibus diam magna, et fringilla dui faucibus vel. Etiam semper libero sit amet ullamcorper consectetur. Curabitur velit ipsum, cursus sit amet justo eget, rhoncus congue enim. In elit ex, sodales vel odio non, ultricies egestas risus. Proin venenatis consectetur augue, et vestibulum leo dictum vel. Etiam id risus vitae dolor viverra blandit ut ac ante.\n" +
                "Quisque a nibh sem. Nulla facilisi. Ut gravida, dui et malesuada interdum, nunc arcu eleifend ligula, quis ornare tortor quam at ante. Vestibulum ac porta nibh, vitae imperdiet erat. Pellentesque nec lacus ex. Nullam sed hendrerit lacus. Curabitur varius sem sit amet tortor sollicitudin auctor. Donec eu feugiat enim, quis pellentesque urna. Morbi finibus fermentum varius. Aliquam quis efficitur nisi. Cras at tortor erat. Vestibulum interdum diam lacus, a lacinia mauris dapibus ut. Suspendisse potenti.\n" +
                "Vestibulum vel diam nec felis sodales porta nec sit amet eros. Quisque sit amet molestie risus. Pellentesque turpis ante, aliquam at urna vel, pulvinar fermentum massa. Proin posuere eu erat id condimentum. Nulla imperdiet erat a varius laoreet. Curabitur sollicitudin urna non commodo condimentum. Ut id ligula in ligula maximus pulvinar et id eros. Fusce et consequat orci. Maecenas leo sem, tristique quis justo nec, accumsan interdum quam. Nunc imperdiet scelerisque iaculis. Praesent sollicitudin purus et purus porttitor volutpat. Duis tincidunt, ipsum vel dignissim imperdiet, ligula nisi ultrices velit, at sodales felis urna at mi. Donec arcu ligula, pulvinar non posuere vel, accumsan eget lorem. Vivamus ac iaculis enim, ut rutrum felis. Praesent non ultrices nibh. Proin tristique, nibh id viverra varius, orci nisi faucibus turpis, quis suscipit sem nisi eu purus.";

        private final static String VERY_HUGE_TEXT = IntStream.rangeClosed(1, 1000).mapToObj(a -> HUGE_TEXT).collect(Collectors.joining("\n"));
    }

    private static class MyWebSocketActor extends UntypedActor {

        private final ActorRef out;
        private final WebSocketContext ctx;
        private static final Logger logger = LoggerFactory.getLogger(WebsocketPing.class);

        public MyWebSocketActor(WebSocketContext ctx, ActorRef out) {
            this.out = out;
            this.ctx = ctx;
        }

        public static Props props(WebSocketContext ctx, ActorRef out) {
            return Props.create(MyWebSocketActor.class, () -> new MyWebSocketActor(ctx, out));
        }

        public void onReceive(Object message) throws Exception {
            logger.info("[MyWebSocketActor] received message {}", message);
            if (message instanceof org.springframework.web.socket.TextMessage) {
                JsValue value = Json.parse(((org.springframework.web.socket.TextMessage) message).getPayload());
                JsValue response = Json.obj()
                        .with("sent_at", System.currentTimeMillis())
                        .with("resource", ctx.pathParam("id").getOrElse("No value !!!"))
                        .with("sourceMessage", value);
                out.tell(new org.springframework.web.socket.TextMessage(response.stringify()), getSelf());
            } else {
                unhandled(message);
            }
        }
    }

    private static class WebSocketClientActor extends UntypedActor {

        private final ActorRef out;

        private static final Logger logger = LoggerFactory.getLogger(WebSocketClientActor.class);

        private AtomicInteger count = new AtomicInteger(0);

        private List<Message> messages = List.empty();

        private Promise<List<Message>> promise;

        public WebSocketClientActor(ActorRef out, Promise<List<akka.http.javadsl.model.ws.Message>> promise) {
            this.out = out;
            this.promise = promise;
        }

        public static Props props(ActorRef out, Promise<List<akka.http.javadsl.model.ws.Message>> promise) {
            return Props.create(WebSocketClientActor.class, () -> new WebSocketClientActor(out, promise));
        }

        @Override
        public void preStart() {
            ActorRef self = getSelf();
            getContext().system().scheduler().schedule(FiniteDuration.Zero(), FiniteDuration.apply(100, TimeUnit.MILLISECONDS), () -> {
                self.tell(TextMessage.create("chunk"), ActorRef.noSender());
            }, context().dispatcher());
        }

        public void onReceive(Object message) throws Exception {
            logger.info("[WebSocketClientActor] received message {}", message);
            if (message != null && akka.http.javadsl.model.ws.Message.class.isAssignableFrom(message.getClass())) {
                if (count.get() == 10) {
                    promise.trySuccess(messages);
                    out.tell(PoisonPill.getInstance(), ActorRef.noSender());
                    getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
                } else {
                    logger.info("[WebSocketClientActor] Sending a chunk {}", count.get());
                    count.incrementAndGet();
                    messages = messages.append((akka.http.javadsl.model.ws.Message) message);
                    out.tell(TextMessage.create("chunk"), getSelf());
                }
            } else {
                unhandled(message);
            }
        }
    }

    private static class WebsocketPing extends UntypedActor {

        private final ActorRef out;
        private final WebSocketContext ctx;
        private static final Logger logger = LoggerFactory.getLogger(WebsocketPing.class);

        public WebsocketPing(WebSocketContext ctx, ActorRef out) {
            this.out = out;
            this.ctx = ctx;
        }

        public static Props props(WebSocketContext ctx, ActorRef out) {
            return Props.create(WebsocketPing.class, () -> new WebsocketPing(ctx, out));
        }

        public void onReceive(Object message) throws Exception {
            logger.info("[WebsocketPing] received message {}", message);
            if (message instanceof org.springframework.web.socket.WebSocketMessage) {
                logger.info("[WebsocketPing] Sending back message");
                out.tell(message, getSelf());
            } else {
                unhandled(message);
            }
        }
    }

    private static class SSEActor extends AbstractActorPublisher<String> {

        public static Props props() {
            return Props.create(SSEActor.class, () -> new SSEActor());
        }

        public static Source<String, ActorRef> source() {
            return Source.actorPublisher(props());
        }

        private int total = 0;

        public SSEActor() {
            receive(ReceiveBuilder.
                matchEquals("START", val -> {
                    System.out.println("Received Start");
                }).
                match(ActorPublisherMessage.Request.class, request -> {
                    System.out.println("Ask for " + totalDemand());
                    for (int i = 0; i < totalDemand() && i < 3; i++) {
                        total++;
                        onNext("data: {\"Hello\": \"World!\"}\n\n");
                    }
                    if (total == 3) {
                        onComplete();
                    }
                }).
                match(ActorPublisherMessage.Cancel.class, cancel -> context().stop(self())).
                build());
        }
    }
}
