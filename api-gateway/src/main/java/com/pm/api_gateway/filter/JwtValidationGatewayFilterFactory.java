package com.pm.api_gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@Component
public class JwtValidationGatewayFilterFactory extends
    AbstractGatewayFilterFactory<Object> {
  // JwtValidation
  // ↓
  // JwtValidationGatewayFilterFactory
  // ↓
  // apply(...)
  // ↓
  // GatewayFilter

  private final WebClient webClient;

  public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
      @Value("${auth.service.url}") String authServiceUrl) {
    this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (token == null || !token.startsWith("Bearer ")) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
      }

      return webClient.get()
          .uri("/validate") // calls Auth Service http://auth-service:4005/auth/validate
          .header(HttpHeaders.AUTHORIZATION, token)
          .retrieve()
          .toBodilessEntity()
          .then(chain.filter(exchange)) // After the Auth Service call succeeds, continue processing the original
                                        // request through the Gateway
          .onErrorResume(WebClientResponseException.class, ex -> {
            exchange.getResponse().setStatusCode(ex.getStatusCode());
            // return exchange.getResponse().setComplete();
            return exchange.getResponse()
                .writeWith(Mono.just(
                    exchange.getResponse()
                        .bufferFactory()
                        .wrap(ex.getResponseBodyAsByteArray())));
          });
    };
  }
}

// HTTP REQUEST
// │
// ▼
// localhost:4004
// │
// ▼
// ┌────────────────┐
// │ API Gateway │
// └───────┬────────┘
// │
// Path = /api/patients
// │
// ▼
// patient-service-route
// │
// ▼
// ┌──────────────────┐
// │ JwtValidation │
// │ Gateway Filter │
// └────────┬─────────┘
// │
// │ WebClient
// ▼
// ┌──────────────────┐
// │ Auth Service │
// │ :4005 │
// └────────┬─────────┘
// │
// ▼
// JwtFilter
// │
// Verify JWT
// │
// ┌─────┴─────┐
// │ │
// valid invalid
// │ │
// 200 401
// │ │
// ▼ ▼
// chain.filter() STOP
// │
// ▼
// Patient Service
// :4000