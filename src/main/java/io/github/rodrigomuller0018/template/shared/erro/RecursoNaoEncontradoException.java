package io.github.rodrigomuller0018.template.shared.erro;

/** Recurso solicitado não existe. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    private final String codigo;

    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
