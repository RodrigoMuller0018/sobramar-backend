package io.github.rodrigomuller0018.sobramar.shared.erro;

/** A operação conflita com o estado atual, como um apelido duplicado. Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    private final String codigo;

    public ConflitoException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
