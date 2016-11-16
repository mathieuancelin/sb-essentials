package org.reactivecouchbase.sbessentials.libs.websocket;

import javaslang.collection.HashMap;
import javaslang.control.Option;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

public class WebSocketContext {

    private final WebSocketSession underlying;
    private final javaslang.collection.Map<String, String> pathParams;

    public WebSocketContext(WebSocketSession underlying) {
        this.underlying = underlying;
        this.pathParams = Option.of(this.underlying.getAttributes().get("___pathVariables"))
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
        return pathParams.get(name);
    }
}
