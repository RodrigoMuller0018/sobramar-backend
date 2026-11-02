package io.github.rodrigomuller0018.template.shared.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tratamento de erro centralizado no formato Problem Details (RFC 9457).
 * Erros de validação e de requisição malformada já são tratados pela classe base.
 * Nenhum stack trace ou detalhe interno é devolvido ao cliente.
 */
@RestControllerAdvice
class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage(), e.getCodigo());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    ProblemDetail regraViolada(RegraDeNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", e.getMessage(), e.getCodigo());
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
