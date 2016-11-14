package org.reactivecouchbase.sbessentials.config;

import akka.actor.ActorSystem;
import javaslang.Tuple;
import javaslang.collection.List;
import org.reactivecouchbase.sbessentials.libs.websocket.FlowWebSocketHandler;
import org.reactivecouchbase.sbessentials.libs.websocket.WebSocket;
import org.reactivecouchbase.sbessentials.libs.websocket.WebSocketMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@EnableWebSocket
@org.springframework.context.annotation.Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired WebApplicationContext webApplicationContext;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        ActorSystem actorSystem = webApplicationContext.getBean("websocket-actor-system", ActorSystem.class);
        Map<String, Object> beansWithAnnotation = webApplicationContext.getBeansWithAnnotation(Controller.class);
        List.ofAll(beansWithAnnotation.values())
            .flatMap(o -> List.of(o.getClass().getDeclaredMethods()).map(a -> Tuple.of(o, a)))
            .filter(m -> m._2().isAnnotationPresent(WebSocketMapping.class))
            .flatMap(p -> {
                Object controller = p._1();
                Method method = p._2();
                return buildPaths(controller, method).map(path -> {
                    LOGGER.info("Mapped {} to websocket", path);
                    try {
                        Object invoke = method.invoke(controller);
                        if(invoke instanceof WebSocket) {
                            return Tuple.of(path, WebSocket.class.cast(invoke));
                        } else {
                            throw new RuntimeException("Wrong method signature, expected WebSocket foo() { ... } ");
                        }
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new RuntimeException("Error", e);
                    }
                });
            })
            .forEach(p ->
                    registry.addHandler(new FlowWebSocketHandler(actorSystem, p._2.handler), p._1).addInterceptors(new UriTemplateHandshakeInterceptor())
            );
    }

    private List<String> buildPaths(Object controller, Method method) {
        WebSocketMapping annotation = method.getAnnotation(WebSocketMapping.class);
        String wsPath = annotation.path().startsWith("/") ? annotation.path() : "/" + annotation.path();
        return rootPaths(controller).map(path -> path + wsPath);
    }

    private List<String> rootPaths(Object controller) {
        if(controller.getClass().isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = controller.getClass().getAnnotation(RequestMapping.class);
            String[] paths = requestMapping.path().length > 0 ? requestMapping.path() : requestMapping.value();
            return List.of(paths)
                    .map(rootPath -> {
                        if(rootPath.endsWith("/")) {
                            return rootPath.substring(0, rootPath.length()-1);
                        } else {
                            return rootPath;
                        }
                    });

        }
        return List.of("");
    }

    private static class UriTemplateHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) throws Exception {

            /* Retrieve original HTTP request */
            HttpServletRequest origRequest = ((ServletServerHttpRequest) request).getServletRequest();

            /* Retrieve template variables */
            Map<String, String> uriTemplateVars = (Map<String, String>) origRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            attributes.put("___pathVariables", uriTemplateVars);
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Exception exception) {}

    }
}