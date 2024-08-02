package com.lovebugs.api_gateway.dto

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var secretKey: String,
    var prefix: String,
    var accessTokenExpiration: Long,
    var refreshTokenExpiration: Long,
)
