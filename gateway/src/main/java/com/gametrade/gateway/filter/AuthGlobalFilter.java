package com.gametrade.gateway.filter;

import com.gametrade.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates the JWT on protected routes and forwards the identity to downstream
 * services via {@code X-User-Id} / {@code X-User-Role} headers.
 *
 * <p>Whitelisted paths (login, register, docs, actuator) bypass authentication.</p>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_AUTH = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtUtil jwtUtil;

    /** Paths that do not require authentication. */
    @Value("${security.whitelist:/api/users/login,/api/users/register,/actuator/**}")
    private List<String> whitelist;

    public AuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublic(request.getMethod(), path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HEADER_AUTH);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "missing bearer token");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtUtil.parse(token);
            Object uid = claims.get(JwtUtil.CLAIM_USER_ID);
            Object role = claims.get(JwtUtil.CLAIM_ROLE);

            ServerHttpRequest mutated = request.mutate()
                    .header(HEADER_USER_ID, uid == null ? "" : uid.toString())
                    .header(HEADER_USER_ROLE, role == null ? "" : role.toString())
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            return unauthorized(exchange, "invalid or expired token");
        }
    }

    /**
     * Public routes: configured whitelist (login/register/actuator) plus anonymous
     * catalog browsing (GET on the item list and numeric item detail). Seller
     * endpoints like {@code /api/items/mine} are intentionally NOT public.
     */
    private boolean isPublic(org.springframework.http.HttpMethod method, String path) {
        if (whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern.trim(), path))) {
            return true;
        }
        if (org.springframework.http.HttpMethod.GET.equals(method)) {
            return path.equals("/api/items") || path.matches("/api/items/\\d+");
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":2001,\"message\":\"" + reason + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run early, before routing filters.
        return -100;
    }
}
