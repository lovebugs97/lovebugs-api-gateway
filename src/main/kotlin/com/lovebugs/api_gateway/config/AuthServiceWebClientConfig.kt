package com.lovebugs.api_gateway.config

import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient


/* WebClient는 Feign과 다르게 Service Discovery 기능이 통합되어 있지 않아 직접 찾는 로직을 구현해야 함 */
@Configuration
class AuthServiceWebClientConfig(private val discoveryClient: DiscoveryClient) {
    companion object {
        private const val AUTH_SERVICE_ID = "auth-server"
    }

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        val authServiceUri = getAuthServiceUri()

        return builder
            .baseUrl(authServiceUri)
            .defaultHeaders {
                it.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            }
            .build()
    }

    private fun getAuthServiceUri(): String {
        val instances = discoveryClient.getInstances(AUTH_SERVICE_ID)

        if (instances.isEmpty()) {
            throw RuntimeException("$AUTH_SERVICE_ID Not Found")
        }

        val instance = instances.first()
        return "http://${instance.host}:${instance.port}"
    }
}