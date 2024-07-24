package com.lovebugs.api_gateway

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalFilter : AbstractGatewayFilterFactory<GlobalFilter.Config>(Config::class.java) {
    data class Config(
        val baseMessage: String?,
        val preLogger: Boolean,
        val postLogger: Boolean
    )

    companion object {
        private val log = LoggerFactory.getLogger(GlobalFilter::class.java)
    }

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            // Global Pre Filter Scope ===========================================================
            val request: ServerHttpRequest = exchange.request
            val response: ServerHttpResponse = exchange.response

            config.baseMessage?.let { log.info("Base Message : {}", config.baseMessage) }

            if (config.preLogger) {
                log.info("requested endpoint : {}, request id : {}", request.path, request.headers)
            }
            // Global Pre Filter Scope =============================================================

            // Global Post Filter Scope ==========================================================
            return@GatewayFilter chain.filter(exchange).then(Mono.fromRunnable {
                if (config.postLogger) {
                    log.info("Response Code : {}", response.statusCode)
                }
            })
            // Global Post Filter Scope =============================================================
        }
    }
}
