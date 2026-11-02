package io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Cria a conta inicial ao subir, se ela estiver configurada no ambiente e ainda não existir. */
@Component
class ContaInicialRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ContaInicialRunner.class);

    private final AutenticacaoService servico;
    private final String email;
    private final String senha;

    ContaInicialRunner(
            AutenticacaoService servico,
            @Value("${sobramar.conta-inicial.email:}") String email,
            @Value("${sobramar.conta-inicial.senha:}") String senha) {
        this.servico = servico;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (email.isBlank() || senha.isBlank()) {
            return;
        }
        if (servico.criarContaSeAusente(email, senha, "Minha conta")) {
            LOG.info("Conta inicial criada.");
        }
    }
}
