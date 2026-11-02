package io.github.rodrigomuller0018.sobramar.pessoa.dominio;

import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.UUID;

/** Marcador de quem é cada item, só com apelido. Sem CPF, nascimento ou contato. */
public class Pessoa {

    public static final int APELIDO_MAXIMO = 40;

    private final UUID id;
    private final UUID contaId;
    private final Instant criadoEm;
    private String apelido;
    private boolean arquivada;

    private Pessoa(UUID id, UUID contaId, String apelido, boolean arquivada, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.apelido = apelido;
        this.arquivada = arquivada;
        this.criadoEm = criadoEm;
    }

    public static Pessoa nova(UUID contaId, String apelido, Instant agora) {
        return new Pessoa(UUID.randomUUID(), contaId, validarApelido(apelido), false, agora);
    }

    public static Pessoa restaurar(UUID id, UUID contaId, String apelido, boolean arquivada, Instant criadoEm) {
        return new Pessoa(id, contaId, apelido, arquivada, criadoEm);
    }

    public void renomear(String novoApelido) {
        this.apelido = validarApelido(novoApelido);
    }

    public void arquivar() {
        this.arquivada = true;
    }

    private static String validarApelido(String apelido) {
        String limpo = apelido == null ? "" : apelido.strip();
        if (limpo.isEmpty()) {
            throw new RegraDeNegocioException("APELIDO_OBRIGATORIO", "Informe o apelido.");
        }
        if (limpo.length() > APELIDO_MAXIMO) {
            throw new RegraDeNegocioException("APELIDO_MUITO_LONGO", "O apelido tem mais de 40 caracteres.");
        }
        return limpo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getApelido() {
        return apelido;
    }

    public boolean isArquivada() {
        return arquivada;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
