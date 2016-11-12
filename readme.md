# sb-essentials

`sb-essentials` is a small library to make Spring Boot livable and Streamable. Every HTTP actionStep is defined to return an `Action` and use `Akka Streams` under the hood.

```java
@GetMapping("/hello")
public Action text() {
    ...
}
```

## Actions

Every Spring actionStep returns a `Action` that can be composed from `ActionStep`s

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.ActionStep;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    @GetMapping("/hello")
    public Action text() {
        return Action.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```

`Actions`s can easily be composed from `ActionStep`s

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.ActionStep;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    // ActionStep that logs before request
    private static ActionStep LogBefore = (req, block) -> {
        Long start = System.currentTimeMillis();
        logger.info("[Log] before actionStep -> {}", req.getRequest().getRequestURI());
        return block.apply(req.setValue("start", start));
    };

    // ActionStep that logs after request
    private static ActionStep LogAfter = (req, block) -> block.apply(req).andThen(ttry -> {
        logger.info(
            "[Log] after actionStep -> {} : took {}",
            req.getRequest().getRequestURI(),
            Duration.of(
                System.currentTimeMillis() - req.getValue("start", Long.class),
                TimeUnit.MILLISECONDS
            ).toHumanReadable()
        );
    });

    // previous ActionSteps composition as a new one
    private static ActionStep LoggedAction = LogBefore.andThen(LogAfter);

    @GetMapping("/hello")
    public Action text() {
        // Use composed actionStep
        return LoggedAction.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```


## Examples

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.ActionStep;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    @GetMapping("/hello")
    public Action text() {
        return Action.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }

    @GetMapping("/json")
    public Action json() {
        return Action.sync(ctx ->
            Ok.json(
                Json.obj().with("message", "Hello World!")
            )
        );
    }

    @GetMapping("/ws")
    public Action testWS() {
        return Action.async(ctx ->
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
    public Action testStream() {
        return Action.sync(ctx -> {
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