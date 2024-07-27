package com.lovebugs.api_gateway

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
        private const val AUTHORIZATION_PREFIX = "Bearer"
        private val log = LoggerFactory.getLogger(AccessTokenValidationFilter::class.java)
        private val NO_CHECK_URL = arrayOf("/token/v1/validation")
    }

    class Config

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            val request: ServerHttpRequest = exchange.request
            val path = request.path.toString()
            val authHeader = request.headers.getFirst(AUTHORIZATION_HEADER)

            val check = NO_CHECK_URL.none { it == path }

            if (check) {
                log.info("Path: {}, Check: {}, authHeader: {}", path, check, authHeader)

                // Request Header에 token이 존재하지 않을 때
                if (authHeader == null || !authHeader.startsWith(AUTHORIZATION_PREFIX)) {
                    return@GatewayFilter handleBadRequest(exchange)
                }

                val token = authHeader.replace(AUTHORIZATION_PREFIX, "").trim()

                webClient.get()
                    .uri("/token/v1/validation")
                    .headers { it.set(AUTHORIZATION_HEADER, "$AUTHORIZATION_PREFIX $token") }
                    .retrieve()
                    .bodyToMono(Void::class.java)
                    .then(chain.filter(exchange)) // 토큰이 유효할 때 필터 체인 계속 진행
                    .onErrorResume {
                        log.error("Error: ${it.message}")
                        handleBadRequest(exchange)
                    }
            }

            // 필터링 제외 경로는 필터 체인 계속 진행
            chain.filter(exchange)
        }
    }

    private fun handleBadRequest(exchange: ServerWebExchange): Mono<Void> {
        val response: ServerHttpResponse = exchange.response
        response.statusCode = HttpStatus.BAD_REQUEST
        return response.setComplete()
    }
}