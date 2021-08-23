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

                //permito callbacks de paypal y google auth
                .antMatchers(HttpMethod.GET, "/paypal/success/**").permitAll()
                .antMatchers(HttpMethod.GET, "/paypal/cancel/**").permitAll()

                .antMatchers(HttpMethod.GET, "/googleAuth/return**").permitAll()
                .antMatchers(HttpMethod.GET, "/googleAuth/oauth2/authorization/google**").permitAll()

                // se ha de tener rol ADMIN para acceder a los siguientes endpoints
                .antMatchers(HttpMethod.POST,"/products/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/products/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/products/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE,"/products/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/catalogs/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/catalogs/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/catalogs/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE,"/catalogs/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/inventories/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/inventories/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/inventories/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE,"/inventories/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/carts/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/orders/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/orders/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/orders/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE,"/orders/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/deliveries/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/deliveries/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/deliveries/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE,"/deliveries/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/discounts/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/discounts/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/discounts/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE,"/discounts/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/wishlists/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,"/resources/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/resources/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/resources/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE,"/resources/**").hasRole("ADMIN")

                // para el resto, solo es necesario estar autentificado, por lo que tanto los usuario con rol ADMIN como los usuarios
                // con rol USER podran acceder
                .anyRequest().authenticated();
    }

    @Bean
    public JwtConfig jwtConfig() {
        return new JwtConfig();
    }
}