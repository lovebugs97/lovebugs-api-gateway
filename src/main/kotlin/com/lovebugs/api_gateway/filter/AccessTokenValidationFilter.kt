package com.lovebugs.api_gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class AccessTokenValidationFilter(
    private val webClient: WebClient,
) : AbstractGatewayFilterFactory<AccessTokenValidationFilter.Config>(Config::class.java) {
    companion object {
        private const val AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION
        private const val AUTHORIZATION_PREFIX = "Bearer "
        private val EXCLUDED_PATHS = setOf("/auth/v1/login", "/auth/v1/signup", "/auth/v1/logout", "/auth/v1/refresh")
        private val log = LoggerFactory.getLogger(AccessTokenValidationFilter::class.java)
    }

    data class Config(
        val baseMessage: String?,
        val preLogger: Boolean,
        val postLogger: Boolean
    )

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            val request: ServerHttpRequest = exchange.request
            val path = request.uri.path

            // 제외할 경로에 포함되지 않는 경우 토큰 인증
            if (EXCLUDED_PATHS.none { path.endsWith(it) }) {
                val authHeader = request.headers.getFirst(AUTHORIZATION_HEADER)

                // Request Header에 token이 존재하지 않을 때
                if (authHeader == null || !authHeader.startsWith(AUTHORIZATION_PREFIX)) {
                    return@GatewayFilter handleUnauthorized(exchange)
                }

                val token = authHeader.substring(AUTHORIZATION_PREFIX.length)

                if (config.preLogger) {
                    log.info("Token Validate Request $token")
                }

                webClient.post()
                    .uri("/token/v1/validation")
                    .bodyValue(mapOf("token" to token))
                    .retrieve()
                    .bodyToMono(Void::class.java)
                    .then(chain.filter(exchange)) // 토큰이 유효할 때 필터 체인 계속 진행
                    .onErrorResume {
                        log.error("Error: ${it.message}")
                        handleUnauthorized(exchange)
                    }

            } else {
                // 필터링 제외 경로는 필터 체인 계속 진행
                chain.filter(exchange)
            }
        }
    }

    private fun handleUnauthorized(exchange: ServerWebExchange): Mono<Void> {
        val response: ServerHttpResponse = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        return response.setComplete()
    }
}