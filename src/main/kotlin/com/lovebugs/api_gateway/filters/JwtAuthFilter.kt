package com.lovebugs.api_gateway.filters

import com.lovebugs.api_gateway.dto.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.util.*

@Component
@Order(1)
class JwtAuthFilter(
    private val jwtProperties: JwtProperties,
) : AbstractGatewayFilterFactory<JwtAuthFilter.Config>(Config::class.java) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JwtAuthFilter::class.java)
        private val NO_CHECK_ENDPOINT = listOf("/auth", "/token/reissue")
    }

    override fun apply(config: Config?): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            val request = exchange.request

            if (request.method == HttpMethod.OPTIONS) {
                return@GatewayFilter chain.filter(exchange)
            }

            val path = request.path.toString()
            val isFiltered = NO_CHECK_ENDPOINT.none { path.startsWith(it) }

            logger.info("Request Path: {}, Filtered: {}", path, isFiltered)

            if (isFiltered) {
                val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)

                if (authHeader == null || !authHeader.startsWith(jwtProperties.prefix)) {
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    return@GatewayFilter exchange.response.setComplete()
                }

                val token = authHeader.substring(jwtProperties.prefix.length)

                try {
                    val claims: Claims = getClaimsFromToken(token)
                    val now = Date()

                    if (now.after(claims.expiration)) {
                        logger.info("Token Expired at {}, now: {}", claims.expiration, Date())
                        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                        return@GatewayFilter exchange.response.setComplete()
                    }

                    val email = claims["email"].toString()
                    val authorities = claims["authorities"].toString()
                    logger.info("Email: {}, Authorities: {}", email, authorities)

                    exchange.request.mutate().header("email", email).build()
                    exchange.request.mutate().header("authorities", authorities).build()
                } catch (e: Exception) {
                    logger.error(e.message)
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    return@GatewayFilter exchange.response.setComplete()
                }
            }

            chain.filter(exchange)
        }
    }

    private fun getClaimsFromToken(token: String): Claims {
        val keyBytes = Base64.getDecoder().decode(jwtProperties.secretKey)
        val key = Keys.hmacShaKeyFor(keyBytes)

        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    class Config
}