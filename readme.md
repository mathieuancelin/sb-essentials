# sb-essentials

`sb-essentials` is a small library to make Spring Boot livable and Streamable. Every action is defined as to return a `Future<Result>` and use `Akka Streams` under the hood.

```java
@GetMapping("/hello")
public Future<Result> text() {
    ...
}
```

## Actions

Every Spring action is actually an `Action`

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.actions.Actions;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    @GetMapping("/hello")
    public Future<Result> text() {
        return Actions.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```

Actions can easily be composed

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.actions.Actions;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    // Action that logs before request
    private static Action LogBefore = (req, block) -> {
        Long start = System.currentTimeMillis();
        logger.info("[Log] before action -> {}", req.getRequest().getRequestURI());
        return block.apply(req.setValue("start", start));
    };

    // Action that logs after request
    private static Action LogAfter = (req, block) -> block.apply(req).andThen(ttry -> {
        logger.info(
            "[Log] after action -> {} : took {}",
            req.getRequest().getRequestURI(),
            Duration.of(
                System.currentTimeMillis() - req.getValue("start", Long.class),
                TimeUnit.MILLISECONDS
            ).toHumanReadable()
        );
    });

    // Actions composition
    private static Action LoggedAction = LogBefore.andThen(LogAfter);

    @GetMapping("/hello")
    public Future<Result> text() {
        // Use composed action
        return LoggedAction.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```


## Examples

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.actions.Actions;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    @GetMapping("/hello")
    public Future<Result> text() {
        return Actions.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }

    @GetMapping("/json")
    public Future<Result> json() {
        return Actions.sync(ctx ->
            Ok.json(
                Json.obj().with("message", "Hello World!")
            )
        );
    }

    @GetMapping("/ws")
    public Future<Result> testWS() {
        return Actions.async(ctx ->
            WS.host("http://freegeoip.net")
                .withPath("/json/")
                .call()
                .flatMap(WSResponse::body)
                .map(r -> r.json().pretty())
                .map(p -> Ok.json(p))
        );
    }

    // Implement SSE ;-)
    @GetMapping("/sse")
    public Future<Result> testStream() {
        return Actions.sync(ctx -> {
            return Ok.stream(
                Source.tick(
                    FiniteDuration.apply(0, TimeUnit.MILLISECONDS),
                    FiniteDuration.apply(1, TimeUnit.SECONDS),
                    ""
                )
                .map(l -> Json.obj().with("time", System.currentTimeMillis()).with("value", l))
                .map(Json::stringify)
                .map(j -> "data: " + j + "\n\n")
            ).as("text/event-stream");
        });
    }
}
```

## Use it in your project

in your `build.gradle` file


```groovy
repositories {
    mavenCentral()
    maven {
        url 'https://raw.github.com/mathieuancelin/sb-essentials/master/repository/snapshots/'
    }
}

dependencies {
    compile("org.reactivecouchbase:sb-essentials:1.0.0-SNAPSHOT")
}
```