package io.github.rodrigomuller0018.sobramar.shared.config;

import io.github.rodrigomuller0018.sobramar.autenticacao.infra.FiltroDeAutenticacao;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Nega por padrão: toda rota exige sessão, exceto as liberadas explicitamente aqui. */
@Configuration
@EnableConfigurationProperties(SegurancaProperties.class)
class SegurancaConfig {

    private static final String[] ROTAS_DE_ENTRADA = {
        "/api/v1/autenticacao/entrar", "/api/v1/autenticacao/segundo-fator", "/api/v1/autenticacao/renovar"
    };

    @Bean
    SecurityFilterChain segurancaHttp(HttpSecurity http, FiltroDeAutenticacao filtro) {
        return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.spa().ignoringRequestMatchers("/api/v1/autenticacao/entrar"))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, ROTAS_DE_ENTRADA)
                        .permitAll()
                        .requestMatchers("/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> escreverProblema(
                                res, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado", "NAO_AUTENTICADO"))
                        .accessDeniedHandler((req, res, ex) -> escreverProblema(
                                res, HttpServletResponse.SC_FORBIDDEN, "Acesso negado", "ACESSO_NEGADO")))
                .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** O filtro roda só dentro da cadeia de segurança; registrado também no servlet, o contexto se perderia. */
    @Bean
    FilterRegistrationBean<FiltroDeAutenticacao> naoRegistrarFiltroNoServlet(FiltroDeAutenticacao filtro) {
        FilterRegistrationBean<FiltroDeAutenticacao> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    PasswordEncoder codificadorDeSenha() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    private static void escreverProblema(HttpServletResponse resposta, int status, String titulo, String codigo)
            throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter()
                .write("{\"title\":\"%s\",\"status\":%d,\"codigo\":\"%s\"}".formatted(titulo, status, codigo));
    }
}
