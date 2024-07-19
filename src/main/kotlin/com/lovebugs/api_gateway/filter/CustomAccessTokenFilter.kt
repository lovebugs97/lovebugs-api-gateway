package com.lovebugs.api_gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class CustomAccessTokenFilter : AbstractGatewayFilterFactory<CustomAccessTokenFilter.Config>(Config::class.java) {
    override fun apply(config: Config?): GatewayFilter {
        return (GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            val request: ServerHttpRequest = exchange.request
            val path = request.uri.path

            // 로그인 및 회원가입 경로 필터링 제외
            if (!path.endsWith("/auth/v1/login") && !path.endsWith("/auth/v1/signup")) {
                // Request Header 에 token 이 존재하지 않을 때
                if (!request.headers.containsKey("Authorization")) {
                    return@GatewayFilter handleUnAuthorized(exchange) // 401 Error
                }

                // Request Header 에서 token 문자열 받아오기
                val token = request.headers["Authorization"]
                val tokenString = token?.get(0)!!

                // 토큰 검증
                if (!validateToken(tokenString)) {
                    return@GatewayFilter handleUnAuthorized(exchange) // 토큰이 일치하지 않을 때
                }
            }

            chain.filter(exchange) // 토큰이 일치할 때
        })
    }

    /* TODO: AccessToken 검증 로직 추가 */
    private fun validateToken(tokenString: String?): Boolean {
        return true
    }

    private fun handleUnAuthorized(exchange: ServerWebExchange): Mono<Void?> {
        val response: ServerHttpResponse = exchange.response

        response.setStatusCode(HttpStatus.UNAUTHORIZED)
        return response.setComplete()
    }

    class Config
}