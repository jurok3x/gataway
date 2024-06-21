package com.ykotsiuba.gateway_app.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.ykotsiuba.gateway_app.dto.GoogleOauthTokenRequest;
import com.ykotsiuba.gateway_app.dto.GoogleOauthTokenResponse;
import com.ykotsiuba.gateway_app.dto.UserInfo;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;

@Service
public class GoogleOauthService {

    private static final String AUTH_GRANT = "authorization_code";
    private static final int TIMEOUT_SECONDS = 5;
    private static final String GOOGLE_AUTH_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    private String clientId;

    private String clientSecret;

    private String scope;

    private final GoogleIdTokenVerifier tokenVerifier;

    private final WebClient webClient;

    public GoogleOauthService(
            @Value("${oauth.google.clientId}") String clientId,
            @Value("${oauth.google.clientSecret}") String clientSecret,
            @Value("${oauth.google.scope}") String scope,
            GoogleIdTokenVerifier tokenVerifier,
            WebClient webClient
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tokenVerifier = tokenVerifier;
        this.webClient = webClient;
    }

    public String generateAuthenticationUrl(String redirectUri, String state) {
        return GOOGLE_AUTH_BASE_URL +
                "?client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(scope) +
                "&state=" + state;
    }

    public Mono<UserInfo> processAuthenticationCallback(String code, String redirectUri) {
        return requestOauthTokens(code, redirectUri)
                .flatMap(oauthTokenResponse -> parseAndVerifyIdToken(oauthTokenResponse))
                .map(idToken -> UserInfo.builder()
                        .email(idToken.getPayload().getEmail())
                        .name((String) idToken.getPayload().get("name"))
                        .build());
    }

    private Mono<GoogleIdToken> parseAndVerifyIdToken(GoogleOauthTokenResponse oauthTokenResponse) {
        try {
            if (oauthTokenResponse.getIdToken() == null) {
                return Mono.error(new IllegalArgumentException("ID token was empty"));
            }
            GoogleIdToken result = tokenVerifier.verify(oauthTokenResponse.getIdToken());
            if (result == null) {
                return Mono.error(new IllegalArgumentException("Id token verification failed"));
            }
            if (StringUtils.isEmpty(result.getPayload().getEmail())) {
                return Mono.error(new IllegalArgumentException("Email not found in id token"));
            }
            return Mono.just(result);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Invalid id token returned by google", e));
        }
    }

    private Mono<GoogleOauthTokenResponse> requestOauthTokens(String code, String redirectUri) {
        GoogleOauthTokenRequest body = GoogleOauthTokenRequest.builder()
                .code(code)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(AUTH_GRANT)
                .redirectUri(redirectUri)
                .build();
        return webClient.post()
                .uri("/token")
                .body(Mono.just(body), GoogleOauthTokenRequest.class)
                .retrieve()
                .bodyToMono(GoogleOauthTokenResponse.class);
    }
}
