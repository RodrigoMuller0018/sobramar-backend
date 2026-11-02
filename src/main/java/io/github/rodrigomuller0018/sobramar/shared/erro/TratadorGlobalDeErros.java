package io.github.rodrigomuller0018.sobramar.shared.erro;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage(), e.getCodigo());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", e.getMessage(), "CREDENCIAIS_INVALIDAS");
    }

    @ExceptionHandler(MuitasTentativasException.class)
    ResponseEntity<ProblemDetail> muitasTentativas(MuitasTentativasException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getEsperarSegundos()))
                .body(problema(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas", e.getMessage(), "MUITAS_TENTATIVAS"));
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
