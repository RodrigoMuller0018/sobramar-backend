package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.github.rodrigomuller0018.sobramar.shared.config.SegurancaProperties;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Emite e valida os JWT. Valida assinatura, algoritmo fixo, expiração, emissor e audiência.
 * Implementação própria por escolha de estudo; a Entrega C reavalia um provedor externo.
 */
@Component
public class ServicoDeTokens {

    private static final String EMISSOR = "sobramar";
    private static final String AUDIENCIA_ACESSO = "sobramar-acesso";
    private static final String AUDIENCIA_DESAFIO = "sobramar-desafio";
    private static final String CLAIM_CONTA = "conta";
    private static final Duration DURACAO_ACESSO = Duration.ofMinutes(15);
    private static final Duration DURACAO_DESAFIO = Duration.ofMinutes(5);

    private final NimbusJwtEncoder codificador;
    private final NimbusJwtDecoder decodificadorAcesso;
    private final NimbusJwtDecoder decodificadorDesafio;
    private final Clock clock;

    public ServicoDeTokens(SegurancaProperties propriedades, Clock clock) {
        SecretKey chave = new SecretKeySpec(Base64.getDecoder().decode(propriedades.jwtSegredo()), "HmacSHA256");
        this.codificador = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        this.decodificadorAcesso = decodificador(chave, AUDIENCIA_ACESSO, clock);
        this.decodificadorDesafio = decodificador(chave, AUDIENCIA_DESAFIO, clock);
        this.clock = clock;
    }

    public String emitirAcesso(UUID usuarioId, UUID contaId) {
        return emitir(usuarioId, AUDIENCIA_ACESSO, DURACAO_ACESSO, contaId);
    }

    public String emitirDesafio(UUID usuarioId) {
        return emitir(usuarioId, AUDIENCIA_DESAFIO, DURACAO_DESAFIO, null);
    }

    public Optional<UsuarioAutenticado> lerAcesso(String token) {
        return decodificar(decodificadorAcesso, token).flatMap(jwt -> {
            String usuario = jwt.getSubject();
            String conta = jwt.getClaimAsString(CLAIM_CONTA);
            return usuario == null || conta == null
                    ? Optional.empty()
                    : Optional.of(new UsuarioAutenticado(UUID.fromString(usuario), UUID.fromString(conta)));
        });
    }

    public Optional<UUID> lerDesafio(String token) {
        return decodificar(decodificadorDesafio, token).map(Jwt::getSubject).map(UUID::fromString);
    }

    public static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private String emitir(UUID usuarioId, String audiencia, Duration duracao, UUID contaId) {
        Instant agora = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .audience(List.of(audiencia))
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(duracao));
        if (contaId != null) {
            claims.claim(CLAIM_CONTA, contaId.toString());
        }
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador
                .encode(JwtEncoderParameters.from(cabecalho, claims.build()))
                .getTokenValue();
    }

    private static Optional<Jwt> decodificar(NimbusJwtDecoder decodificador, String token) {
        if (token == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(decodificador.decode(token));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private static boolean temAudiencia(Jwt jwt, String audiencia) {
        List<String> audiencias = jwt.getAudience();
        return audiencias != null && audiencias.contains(audiencia);
    }

    private static NimbusJwtDecoder decodificador(SecretKey chave, String audiencia, Clock clock) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator validadorDeTempo = new JwtTimestampValidator(Duration.ZERO);
        validadorDeTempo.setClock(clock);
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                validadorDeTempo,
                new JwtIssuerValidator(EMISSOR),
                jwt -> temAudiencia(jwt, audiencia)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
        return decodificador;
    }
}
