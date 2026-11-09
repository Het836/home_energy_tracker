package com.energy_tracker.api_gateway.route;

import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class DeviceServiceRoutes {

    @Bean
    public RouterFunction<ServerResponse> deviceRoutes() {
        return route("device-service")
                .route(RequestPredicates.path("/api/v1/device/**"), http())
                .before(uri("http://localhost:8081"))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker(
                        "deviceServiceCircuitBreaker",
                        URI.create("forward:/deviceServiceFallback")
                ))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> deviceFallbackRoute(){
        return route("device-service-fallback")
                .route(RequestPredicates.path("/deviceServiceFallback"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body("Device Service is down, please try again later"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> deviceServiceApiDocs(){
        return GatewayRouterFunctions.route("device-service-api-docs")
                .route(RequestPredicates.path("/docs/device-service/v3/api-docs"), http())
                .before(uri("http://localhost:8081"))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}
