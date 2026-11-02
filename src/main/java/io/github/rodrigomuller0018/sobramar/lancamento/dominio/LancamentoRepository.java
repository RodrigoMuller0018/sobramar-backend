package io.github.rodrigomuller0018.sobramar.lancamento.dominio;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Porta de persistência. Toda consulta recebe a conta: não existe busca só por id. */
public interface LancamentoRepository {

    record Filtro(LocalDate de, LocalDate ate, UUID carteiraId, UUID pessoaId, UUID categoriaId) {}

    /** Lança ConflitoException se a ocorrência já tiver pagamento. */
    Lancamento salvar(Lancamento lancamento);

    Optional<Lancamento> buscar(UUID contaId, UUID id);

    Optional<Lancamento> buscarDaOcorrencia(UUID contaId, UUID compromissoId, int numero);

    Page<Lancamento> listar(UUID contaId, Filtro filtro, Pageable paginacao);

    void excluir(Lancamento lancamento);

    /** Lança ConflitoException se a fatura já estiver paga. */
    void registrarPagamentoDeFatura(
            UUID contaId, UUID cartaoId, YearMonth competencia, long valorCentavos, LocalDate data, UUID carteiraId);

    boolean excluirPagamentoDeFatura(UUID contaId, UUID cartaoId, YearMonth competencia);
}
