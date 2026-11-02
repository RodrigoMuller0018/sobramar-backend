package io.github.rodrigomuller0018.sobramar.shared.erro;

/** Login, segundo fator ou sessão inválidos. Vira HTTP 401, sem dizer qual parte falhou. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas.");
    }
}
