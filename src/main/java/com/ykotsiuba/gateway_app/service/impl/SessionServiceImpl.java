package com.ykotsiuba.gateway_app.service.impl;

import com.ykotsiuba.gateway_app.dto.UserInfo;
import com.ykotsiuba.gateway_app.entity.UserSession;
import com.ykotsiuba.gateway_app.repository.UserSessionRepository;
import com.ykotsiuba.gateway_app.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    public static final Duration SESSION_DURATION = Duration.ofHours(1);

    public static final String COOKIE_SESSION_ID = "SESSION-ID";


    private final UserSessionRepository userSessionRepository;

    public Mono<UserSession> checkSession(ServerWebExchange exchange) {
        HttpCookie sessionCookie = exchange.getRequest().getCookies().getFirst(COOKIE_SESSION_ID);
        if (sessionCookie == null) {
            return Mono.error(new Exception("Session Cookie not found"));
        }
        return userSessionRepository.findById(sessionCookie.getValue())
                .flatMap(session ->

                        session.isExpired()
                                ? Mono.error(new Exception("Session expired"))
                                : Mono.just(session)
                ).switchIfEmpty(Mono.error(new Exception("Session not found")));
    }

    public Mono<UserSession> saveSession(UserInfo userInfo) {
        return userSessionRepository.createSession(userInfo, Instant.now().plus(SESSION_DURATION));
    }

    public Mono<Void> addSessionCookie(ServerWebExchange exchange, UserSession session) {
        return Mono.fromRunnable(() -> exchange.getResponse().addCookie(ResponseCookie.from(COOKIE_SESSION_ID)
                .value(session.getId())
                .path("/")
                .maxAge(SESSION_DURATION)
                .secure(true)
                .httpOnly(true)
                .build()));
    }
}
