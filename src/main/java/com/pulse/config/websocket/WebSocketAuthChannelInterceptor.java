package com.pulse.config.websocket;

import com.pulse.service.GroupService;
import com.pulse.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern GROUP_TOPIC_PATTERN = Pattern.compile("^/topic/groups/(\\d+)$");

    private final JwtService jwtService;
    private final GroupService groupService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnection(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new AccessDeniedException("Missing WebSocket session attributes");
        }

        String token = (String) sessionAttributes.get(WebSocketHandshakeInterceptor.TOKEN_SESSION_ATTRIBUTE);
        if (token == null || token.isBlank()) {
            throw new AccessDeniedException("Missing WebSocket token");
        }

        String username = jwtService.extractUsername(token);
        Long userId = jwtService.extractUserId(token);
        if (username == null || userId == null || !jwtService.validateToken(token, username)) {
            throw new AccessDeniedException("Invalid WebSocket token");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        accessor.setUser(authentication);
        sessionAttributes.put("ws_user_id", userId);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getDestination() == null) {
            return;
        }

        Matcher matcher = GROUP_TOPIC_PATTERN.matcher(accessor.getDestination());
        if (!matcher.matches()) {
            return;
        }

        Long groupId = Long.valueOf(matcher.group(1));
        Long userId = resolveUserId(accessor);

        if (!groupService.isMember(groupId, userId)) {
            throw new AccessDeniedException("Not a member of this group");
        }
    }

    private Long resolveUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AccessDeniedException("Unauthorized WebSocket subscription");
        }

        String principalName = accessor.getUser().getName();
        if (principalName != null && !principalName.isBlank()) {
            try {
                return Long.parseLong(principalName);
            } catch (NumberFormatException ignored) {
                throw new AccessDeniedException("Unauthorized WebSocket subscription");
            }
        }

        throw new AccessDeniedException("Unauthorized WebSocket subscription");
    }
}