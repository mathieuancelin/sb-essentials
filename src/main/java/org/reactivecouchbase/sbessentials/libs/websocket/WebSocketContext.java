package org.reactivecouchbase.sbessentials.libs.websocket;

import javaslang.control.Option;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

public class WebSocketContext {

    private final WebSocketSession underlying;

    public WebSocketContext(WebSocketSession underlying) {
        this.underlying = underlying;
    }

    public String uri() {
        return underlying.getUri().toString();
    }

    public Map<String, Object> attributes() {
        return underlying.getAttributes();
    }

    public String pathVariable(String name) {
        return Option.of(this.underlying.getAttributes().get("___pathVariables")).map(v -> ((Map<String, String>)v).get(name)).getOrElse(() -> null);
    }
}
