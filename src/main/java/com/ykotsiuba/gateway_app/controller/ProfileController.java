package com.ykotsiuba.gateway_app.controller;

import com.ykotsiuba.gateway_app.dto.UserInfo;
import com.ykotsiuba.gateway_app.entity.UserSession;
import com.ykotsiuba.gateway_app.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController {

    private final SessionService sessionService;

    @GetMapping("/profile")
    public Mono<UserInfo> profile(ServerWebExchange exchange) {
        return sessionService.checkSession(exchange)
                .flatMap(this::toUserInfo)
                .onErrorResume(Exception.class, e -> {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
                });
    }

    private Mono<UserInfo> toUserInfo(UserSession session) {
        return Mono.just(UserInfo.builder()
                .email(session.getEmail())
                .name(session.getName())
                .build());
    }
}
