package io.github.rodrigomuller0018.sobramar.categoria.dominio;

import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lista editável, com categorias amplas: rótulos detalhados poderiam revelar dados sensíveis. */
public class Categoria {

    public static final int NOME_MAXIMO = 40;
    public static final List<String> PADRAO = List.of(
            "Moradia",
            "Alimentação",
            "Transporte",
            "Saúde",
            "Educação",
            "Lazer",
            "Compras",
            "Serviços",
            "Renda",
            "Outros");

    private final UUID id;
    private final UUID contaId;
    private final Instant criadoEm;
    private String nome;
    private boolean arquivada;

    private Categoria(UUID id, UUID contaId, String nome, boolean arquivada, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.nome = nome;
        this.arquivada = arquivada;
        this.criadoEm = criadoEm;
    }

    public static Categoria nova(UUID contaId, String nome, Instant agora) {
        return new Categoria(UUID.randomUUID(), contaId, validar(nome), false, agora);
    }

    public static Categoria restaurar(UUID id, UUID contaId, String nome, boolean arquivada, Instant criadoEm) {
        return new Categoria(id, contaId, nome, arquivada, criadoEm);
    }

    public void renomear(String novoNome) {
        this.nome = validar(novoNome);
    }

    public void arquivar() {
        this.arquivada = true;
    }

    private static String validar(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty() || limpo.length() > NOME_MAXIMO) {
            throw new RegraDeNegocioException("NOME_INVALIDO", "O nome precisa ter de 1 a 40 caracteres.");
        }
        return limpo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getNome() {
        return nome;
    }

    public boolean isArquivada() {
        return arquivada;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
