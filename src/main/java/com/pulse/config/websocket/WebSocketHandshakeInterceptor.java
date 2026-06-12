package com.pulse.config.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TOKEN_SESSION_ATTRIBUTE = "ws_auth_token";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // get and store auth bearer token from http request in websocket session attributes
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token != null && !token.isBlank()) {
            attributes.put(TOKEN_SESSION_ATTRIBUTE, token);
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No-op
    }
}