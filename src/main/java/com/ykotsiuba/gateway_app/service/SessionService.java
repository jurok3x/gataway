package com.ykotsiuba.gateway_app.service;

import com.ykotsiuba.gateway_app.dto.UserInfo;
import com.ykotsiuba.gateway_app.entity.UserSession;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface SessionService {

    Mono<UserSession> saveSession(UserInfo userInfo);

    Mono<Void> addSessionCookie(ServerWebExchange exchange, UserSession session);

    Mono<UserSession> checkSession(ServerWebExchange exchange);
}
