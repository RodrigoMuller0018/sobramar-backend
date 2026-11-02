package io.github.rodrigomuller0018.template.shared.erro;

/** Uma regra de negócio foi violada. Vira HTTP 422, com um código de erro estável para o cliente. */
public class RegraDeNegocioException extends RuntimeException {

    private final String codigo;

    public RegraDeNegocioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
