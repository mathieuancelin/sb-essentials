package org.reactivecouchbase.examples.sbessentials;

import akka.NotUsed;
import akka.http.javadsl.ConnectHttp;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.Seq;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.ws.WS;
import org.reactivecouchbase.sbessentials.libs.ws.WSResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.reactivecouchbase.sbessentials.libs.result.Results.Ok;

@ComponentScan
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    public static class MyStacktrace {
        public final String query;
        public final Seq<String> elements;
        public MyStacktrace(String query, Seq<String> elements) {
            this.query = query;
            this.elements = elements;
        }
    }

    @RestController
    @RequestMapping("/")
    public static class BaseController {

        private static final Logger logger = LoggerFactory.getLogger(BaseController.class);
        private static final String baseUrl = "https://api.stackexchange.com";

        @PostMapping("logs")
        public Action logs() {
            return Action.sync(ctx -> {
                Source<ByteString, ?> source = ctx.bodyAsStream();

                Flow<ByteString, MyStacktrace, ?> stacktraceDelimiter = Flow.<ByteString>create()
                        .via(Framing.delimiter(ByteString.fromString("\n"), 100000))
                        .map(ByteString::utf8String)
                        .via(filterStacktrace())
                        .take(5);

                Source<String, ?> output = source
                        .via(stacktraceDelimiter)
                        .flatMapConcat(BaseController::callStackOverflow)
                        .map(JsValue::stringify);

                return Ok.stream(output).as("application/json");
            });
        }

        public static Flow<String, MyStacktrace, ?> filterStacktrace() {
            logger.info("filterStacktrace()");
            final List<String> builder = new ArrayList<>();
            return Flow.<String>create().flatMapConcat(str -> {
                if (str.startsWith("org.") && builder.isEmpty()) {
                    builder.add(str);
                   return Source.empty();
                } else if (!builder.isEmpty() && str.isEmpty()) {
                    Seq<String> stack = javaslang.collection.List.ofAll(builder);
                    MyStacktrace stacktrace = new MyStacktrace(stack.head(), stack.tail());
                    builder.clear();
                    return Source.single(stacktrace);
                } else if (!builder.isEmpty() && !str.isEmpty()) {
                    builder.add(str);
                    return Source.empty();
                } else {
                    return Source.empty();
                }
            });
        }

        public static Source<JsValue, NotUsed> callStackOverflow(MyStacktrace stacktrace) {
            Future<JsValue> future = WS.host(baseUrl)
                .addPathSegment("2.2")
                .addPathSegment("search")
                .addPathSegment("advanced")
                .withQueryParam("order", "desc")
                .withQueryParam("sort", "activity")
                .withQueryParam("site", "stackoverflow")
                .withQueryParam("q", stacktrace.query.split(":")[0])
                .call()
                .flatMap(WSResponse::body)
                .map(body -> body.json().field("items").asArray().values.get(0))
                .flatMap(firstQuestion -> {
                    Integer questionId = firstQuestion.field("question_id").asInteger();
                    return WS.host(baseUrl)
                        .addPathSegment("2.2")
                        .addPathSegment("questions")
                        .addPathSegment(questionId)
                        .addPathSegment("answers")
                        .withQueryParam("order", "desc")
                        .withQueryParam("sort", "activity")
                        .withQueryParam("site", "stackoverflow")
                        .call()
                        .flatMap(WSResponse::body)
                        .map(body -> body.json().field("items").asArray().values.get(0))
                        .flatMap(firstAnswer -> {
                            Integer answerId = firstAnswer.field("answer_id").asInteger();
                            return WS.host(baseUrl)
                                .addPathSegment("2.2")
                                .addPathSegment("answers")
                                .addPathSegment(answerId)
                                .withQueryParam("order", "desc")
                                .withQueryParam("sort", "activity")
                                .withQueryParam("site", "stackoverflow")
                                .withQueryParam("filter", "!9YdnSMKKT")
                                .call()
                                .flatMap(WSResponse::body)
                                .map(body -> body.json().field("items").asArray().values.get(0))
                                .map(answerDetails -> {
                                    String answerBody = answerDetails.field("body").asString();
                                    return Json.obj()
                                        .with("body", answerBody)
                                        .with("query", stacktrace.query);

                                });
                        });
                });
            return Source.fromCompletionStage(future.toJdkCompletableFuture());
        }
    }
}