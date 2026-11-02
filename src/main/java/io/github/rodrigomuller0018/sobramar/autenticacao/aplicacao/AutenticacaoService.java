package io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao;

import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.SessaoRenovacao;
import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.SessaoRenovacaoRepository;
import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.Totp;
import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.Usuario;
import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.UsuarioRepository;
import io.github.rodrigomuller0018.sobramar.autenticacao.infra.CifradorDeSegredos;
import io.github.rodrigomuller0018.sobramar.autenticacao.infra.ServicoDeTokens;
import io.github.rodrigomuller0018.sobramar.conta.dominio.Conta;
import io.github.rodrigomuller0018.sobramar.conta.dominio.ContaRepository;
import io.github.rodrigomuller0018.sobramar.shared.erro.CredenciaisInvalidasException;
import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login em duas etapas, com sessão por access token curto e refresh token rotativo. */
@Service
public class AutenticacaoService {

    private static final Duration DURACAO_DA_RENOVACAO = Duration.ofDays(14);
    private static final int BYTES_DO_TOKEN = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final ContaRepository contas;
    private final SessaoRenovacaoRepository sessoes;
    private final PasswordEncoder codificador;
    private final ServicoDeTokens tokens;
    private final CifradorDeSegredos cifrador;
    private final LimitadorDeTentativas limitador;
    private final Clock clock;
    private final String hashFicticio;

    public AutenticacaoService(
            UsuarioRepository usuarios,
            ContaRepository contas,
            SessaoRenovacaoRepository sessoes,
            PasswordEncoder codificador,
            ServicoDeTokens tokens,
            CifradorDeSegredos cifrador,
            LimitadorDeTentativas limitador,
            Clock clock) {
        this.usuarios = usuarios;
        this.contas = contas;
        this.sessoes = sessoes;
        this.codificador = codificador;
        this.tokens = tokens;
        this.cifrador = cifrador;
        this.limitador = limitador;
        this.clock = clock;
        this.hashFicticio = codificador.encode(UUID.randomUUID().toString());
    }

    /** Sem segundo fator ativo, a sessão já abre, para a pessoa poder ativá-lo. */
    public record ResultadoEntrada(boolean segundoFatorObrigatorio, String tokenDesafio, TokensDeSessao sessao) {}

    public record TokensDeSessao(String tokenAcesso, String tokenRenovacao) {}

    public record DadosDoUsuario(String email, String nomeConta, boolean segundoFatorAtivo) {}

    /** Não há cadastro público na Entrega A: a conta nasce pela configuração do ambiente. */
    @Transactional
    public boolean criarContaSeAusente(String email, String senha, String nomeConta) {
        if (usuarios.buscarPorEmail(Usuario.normalizarEmail(email)).isPresent()) {
            return false;
        }
        Instant agora = clock.instant();
        Conta conta = new Conta(UUID.randomUUID(), nomeConta, agora);
        contas.criar(conta);
        usuarios.salvar(Usuario.novo(conta.id(), email, codificador.encode(senha), agora));
        return true;
    }

    @Transactional(readOnly = true)
    public DadosDoUsuario dadosDoUsuario(UsuarioAutenticado autenticado) {
        Usuario usuario = usuarios.buscarPorId(autenticado.usuarioId()).orElseThrow(CredenciaisInvalidasException::new);
        Conta conta = contas.buscarPorId(autenticado.contaId()).orElseThrow(CredenciaisInvalidasException::new);
        return new DadosDoUsuario(usuario.getEmail(), conta.nome(), usuario.isTotpAtivo());
    }

    @Transactional
    public ResultadoEntrada entrar(String email, String senha) {
        String emailNormalizado = Usuario.normalizarEmail(email);
        limitador.verificar(emailNormalizado);
        Usuario usuario = usuarios.buscarPorEmail(emailNormalizado).orElse(null);
        // Com e-mail inexistente, verifica mesmo assim, para o tempo de resposta não revelar quem tem conta.
        boolean senhaConfere = codificador.matches(senha, usuario == null ? hashFicticio : usuario.getSenhaHash());
        if (usuario == null || !senhaConfere) {
            limitador.registrarFalha(emailNormalizado);
            throw new CredenciaisInvalidasException();
        }
        if (usuario.isTotpAtivo()) {
            return new ResultadoEntrada(true, tokens.emitirDesafio(usuario.getId()), null);
        }
        limitador.limpar(emailNormalizado);
        return new ResultadoEntrada(false, null, abrirSessao(usuario));
    }

    @Transactional
    public TokensDeSessao confirmarSegundoFator(String tokenDesafio, String codigo) {
        UUID usuarioId = tokens.lerDesafio(tokenDesafio).orElseThrow(CredenciaisInvalidasException::new);
        Usuario usuario = usuarios.buscarPorId(usuarioId).orElseThrow(CredenciaisInvalidasException::new);
        limitador.verificar(usuario.getEmail());
        OptionalLong passo = validarCodigo(usuario, codigo);
        if (passo.isEmpty()) {
            limitador.registrarFalha(usuario.getEmail());
            throw new CredenciaisInvalidasException();
        }
        usuario.registrarUsoDoSegundoFator(passo.getAsLong());
        usuarios.salvar(usuario);
        limitador.limpar(usuario.getEmail());
        return abrirSessao(usuario);
    }

    /**
     * Rotaciona o refresh token. Um token já revogado sendo reapresentado indica possível roubo,
     * então todas as sessões do usuário são encerradas. Essa revogação precisa persistir apesar da exceção.
     */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokensDeSessao renovar(String tokenRenovacao) {
        Instant agora = clock.instant();
        SessaoRenovacao sessao = sessoes.buscarPorHash(ServicoDeTokens.hash(tokenRenovacao))
                .orElseThrow(CredenciaisInvalidasException::new);
        if (sessao.revogada()) {
            sessoes.revogarTodasDoUsuario(sessao.usuarioId(), agora);
            throw new CredenciaisInvalidasException();
        }
        if (sessao.expirada(agora)) {
            throw new CredenciaisInvalidasException();
        }
        sessoes.revogar(sessao.id(), agora);
        Usuario usuario = usuarios.buscarPorId(sessao.usuarioId()).orElseThrow(CredenciaisInvalidasException::new);
        return abrirSessao(usuario);
    }

    @Transactional
    public void sair(String tokenRenovacao) {
        if (tokenRenovacao == null) {
            return;
        }
        sessoes.buscarPorHash(ServicoDeTokens.hash(tokenRenovacao))
                .ifPresent(sessao -> sessoes.revogar(sessao.id(), clock.instant()));
    }

    /** Devolve a URI para o aplicativo autenticador. O segredo só vale depois de confirmado. */
    @Transactional
    public String iniciarAtivacaoDoSegundoFator(UUID usuarioId) {
        Usuario usuario = usuarios.buscarPorId(usuarioId).orElseThrow(CredenciaisInvalidasException::new);
        if (usuario.isTotpAtivo()) {
            throw new RegraDeNegocioException("SEGUNDO_FATOR_JA_ATIVO", "O segundo fator já está ativo.");
        }
        String segredo = Totp.novoSegredoBase32();
        usuario.iniciarSegundoFator(cifrador.cifrar(segredo));
        usuarios.salvar(usuario);
        String rotulo = URLEncoder.encode("Sobramar:" + usuario.getEmail(), StandardCharsets.UTF_8);
        return "otpauth://totp/%s?secret=%s&issuer=Sobramar".formatted(rotulo, segredo);
    }

    @Transactional
    public void confirmarAtivacaoDoSegundoFator(UUID usuarioId, String codigo) {
        Usuario usuario = usuarios.buscarPorId(usuarioId).orElseThrow(CredenciaisInvalidasException::new);
        if (usuario.getTotpSegredoCifrado() == null || usuario.isTotpAtivo()) {
            throw new RegraDeNegocioException("SEGUNDO_FATOR_NAO_INICIADO", "Inicie a ativação do segundo fator.");
        }
        long passo = validarCodigo(usuario, codigo).orElseThrow(CredenciaisInvalidasException::new);
        usuario.confirmarSegundoFator(passo);
        usuarios.salvar(usuario);
    }

    private OptionalLong validarCodigo(Usuario usuario, String codigo) {
        if (usuario.getTotpSegredoCifrado() == null) {
            return OptionalLong.empty();
        }
        String segredo = cifrador.decifrar(usuario.getTotpSegredoCifrado());
        return Totp.validar(segredo, codigo, clock.instant(), usuario.getTotpUltimoPasso());
    }

    private TokensDeSessao abrirSessao(Usuario usuario) {
        Instant agora = clock.instant();
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        ALEATORIO.nextBytes(bytes);
        String tokenRenovacao = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.criar(new SessaoRenovacao(
                UUID.randomUUID(),
                usuario.getId(),
                ServicoDeTokens.hash(tokenRenovacao),
                agora.plus(DURACAO_DA_RENOVACAO),
                null,
                agora));
        return new TokensDeSessao(tokens.emitirAcesso(usuario.getId(), usuario.getContaId()), tokenRenovacao);
    }
}
