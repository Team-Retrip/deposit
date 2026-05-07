package com.deposit.infrastructure.config;

import com.deposit.infrastructure.security.AuthenticationFilter;
import com.deposit.infrastructure.security.UserContextArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UserContextArgumentResolver());
    }

    @Bean
    @ConditionalOnProperty(name = "auth.filter.enabled", havingValue = "true")
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilter(
            @Value("${jwt.public-key}") String publicKey) {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthenticationFilter(publicKey));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
