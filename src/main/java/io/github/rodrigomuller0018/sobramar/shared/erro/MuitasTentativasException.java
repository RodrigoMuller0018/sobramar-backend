package io.github.rodrigomuller0018.sobramar.shared.erro;

/** Excesso de tentativas de login. Vira HTTP 429 com Retry-After. */
public class MuitasTentativasException extends RuntimeException {

    private final long esperarSegundos;

    public MuitasTentativasException(long esperarSegundos) {
        super("Muitas tentativas. Aguarde antes de tentar de novo.");
        this.esperarSegundos = esperarSegundos;
    }

    public long getEsperarSegundos() {
        return esperarSegundos;
    }
}
