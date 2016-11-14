package org.reactivecouchbase.sbessentials.libs.websocket;

import javaslang.collection.HashMap;
import org.reactivecouchbase.functional.Option;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

public class WebSocketContext {

    private final WebSocketSession underlying;
    private final javaslang.collection.Map<String, String> pathParams;

    public WebSocketContext(WebSocketSession underlying) {
        this.underlying = underlying;
        this.pathParams = org.reactivecouchbase.functional.Option.apply(this.underlying.getAttributes().get("___pathVariables"))
                .map(o -> (java.util.Map<String, String>) o)
                .map(HashMap::ofAll).getOrElse(HashMap.empty());
    }

    public String uri() {
        return underlying.getUri().toString();
    }

    public Map<String, Object> attributes() {
        return underlying.getAttributes();
    }

    public Option<String> pathParam(String name) {
        return pathParams.get(name).transform(opt -> {
            if (opt.isDefined()) {
                return Option.apply(opt.get());
            } else {
                return Option.none();
            }
        });
    }

    // public String pathVariable(String name) {
    //     return Option.of(this.underlying.getAttributes().get("___pathVariables")).map(v -> ((Map<String, String>)v).get(name)).getOrElse(() -> null);
    // }
}
