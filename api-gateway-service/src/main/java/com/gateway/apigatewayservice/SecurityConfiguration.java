package com.gateway.apigatewayservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Autowired
    private JwtConfig jwtConfig;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                // make sure we use stateless session; session won't be used to store user's state.
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // handle an authorized attempts
                .exceptionHandling().authenticationEntryPoint((req, rsp, e) -> rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .and()
                // Add a filter to validate the tokens with every request
                .addFilterAfter(new JwtTokenAuthenticationFilter(jwtConfig), UsernamePasswordAuthenticationFilter.class)
                // authorization requests config
                .authorizeRequests()
                //.oauth2Login().permitAll()
                // allow all who are accessing "auth" service
                .antMatchers(HttpMethod.POST, jwtConfig.getUri()).permitAll()
                // allow GET info
                .antMatchers(HttpMethod.GET, "/info").permitAll()
                // allow GET metrics
                .antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                //permitimos callbacks de paypal y google auth
                .antMatchers(HttpMethod.GET, "/paypal/success/**").permitAll()
                .antMatchers(HttpMethod.GET, "/paypal/cancel/**").permitAll()
                .antMatchers(HttpMethod.GET, "/googleAuth/return**").permitAll()
                .antMatchers(HttpMethod.GET, "/googleAuth/oauth2/authorization/google**").permitAll()
                // must be an admin if trying to access admin area (authentication is also required here)
                .antMatchers("/group" + "/admin/**").hasAuthority("ADMIN")
                // Any other request must be authenticated
                .anyRequest().authenticated();
    }

    @Bean
    public JwtConfig jwtConfig() {
        return new JwtConfig();
    }
}