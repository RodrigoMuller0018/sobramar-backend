package io.github.rodrigomuller0018.sobramar.autenticacao.dominio;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** Quem faz login. Os dados financeiros pertencem à conta, não ao usuário. */
public class Usuario {

    private final UUID id;
    private final UUID contaId;
    private final String email;
    private final String senhaHash;
    private final Instant criadoEm;
    private String totpSegredoCifrado;
    private boolean totpAtivo;
    private Long totpUltimoPasso;

    private Usuario(UUID id, UUID contaId, String email, String senhaHash, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.email = email;
        this.senhaHash = senhaHash;
        this.criadoEm = criadoEm;
    }

    public static Usuario novo(UUID contaId, String email, String senhaHash, Instant agora) {
        return new Usuario(UUID.randomUUID(), contaId, normalizarEmail(email), senhaHash, agora);
    }

    public static Usuario restaurar(
            UUID id,
            UUID contaId,
            String email,
            String senhaHash,
            Instant criadoEm,
            String totpSegredoCifrado,
            boolean totpAtivo,
            Long totpUltimoPasso) {
        Usuario usuario = new Usuario(id, contaId, email, senhaHash, criadoEm);
        usuario.totpSegredoCifrado = totpSegredoCifrado;
        usuario.totpAtivo = totpAtivo;
        usuario.totpUltimoPasso = totpUltimoPasso;
        return usuario;
    }

    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /** Guarda o segredo ainda inativo. Só vale depois de confirmado com um código. */
    public void iniciarSegundoFator(String segredoCifrado) {
        this.totpSegredoCifrado = segredoCifrado;
        this.totpAtivo = false;
        this.totpUltimoPasso = null;
    }

    public void confirmarSegundoFator(long passo) {
        this.totpAtivo = true;
        this.totpUltimoPasso = passo;
    }

    public void registrarUsoDoSegundoFator(long passo) {
        this.totpUltimoPasso = passo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getTotpSegredoCifrado() {
        return totpSegredoCifrado;
    }

    public boolean isTotpAtivo() {
        return totpAtivo;
    }

    public Long getTotpUltimoPasso() {
        return totpUltimoPasso;
    }
}
