package io.github.rodrigomuller0018.sobramar.lancamento.infra;

import io.github.rodrigomuller0018.sobramar.compromisso.dominio.PagamentosRealizados;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.Lancamento;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.LancamentoRepository;
import io.github.rodrigomuller0018.sobramar.shared.dominio.Dinheiro;
import io.github.rodrigomuller0018.sobramar.shared.erro.ConflitoException;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class LancamentoRepositoryAdapter implements LancamentoRepository, PagamentosRealizados {

    private final LancamentoJpaRepository lancamentos;
    private final PagamentoFaturaJpaRepository faturas;
    private final Clock clock;

    LancamentoRepositoryAdapter(
            LancamentoJpaRepository lancamentos, PagamentoFaturaJpaRepository faturas, Clock clock) {
        this.lancamentos = lancamentos;
        this.faturas = faturas;
        this.clock = clock;
    }

    @Override
    public Lancamento salvar(Lancamento l) {
        LancamentoEntity e = new LancamentoEntity();
        e.id = l.id();
        e.contaId = l.contaId();
        e.tipo = l.tipo().name();
        e.descricao = l.descricao();
        e.valor = Dinheiro.paraBanco(l.valorCentavos());
        e.data = l.data();
        e.carteiraId = l.carteiraId();
        e.carteiraDestinoId = l.carteiraDestinoId();
        e.compromissoId = l.compromissoId();
        e.numeroOcorrencia = l.numeroOcorrencia();
        e.pessoaId = l.pessoaId();
        e.categoriaId = l.categoriaId();
        e.ignorado = l.ignorado();
        e.criadoEm = l.criadoEm();
        try {
            return paraDominio(lancamentos.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("OCORRENCIA_JA_PAGA", "Essa parcela já foi paga.");
        }
    }

    @Override
    public Optional<Lancamento> buscar(UUID contaId, UUID id) {
        return lancamentos.findByContaIdAndId(contaId, id).map(LancamentoRepositoryAdapter::paraDominio);
    }

    @Override
    public Optional<Lancamento> buscarDaOcorrencia(UUID contaId, UUID compromissoId, int numero) {
        return lancamentos
                .findByContaIdAndCompromissoIdAndNumeroOcorrencia(contaId, compromissoId, numero)
                .map(LancamentoRepositoryAdapter::paraDominio);
    }

    @Override
    public Page<Lancamento> listar(UUID contaId, Filtro f, Pageable paginacao) {
        Specification<LancamentoEntity> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(raiz.get("contaId"), contaId));
            if (f.de() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(raiz.<LocalDate>get("data"), f.de()));
            }
            if (f.ate() != null) {
                condicoes.add(cb.lessThanOrEqualTo(raiz.<LocalDate>get("data"), f.ate()));
            }
            if (f.carteiraId() != null) {
                condicoes.add(cb.equal(raiz.get("carteiraId"), f.carteiraId()));
            }
            if (f.pessoaId() != null) {
                condicoes.add(cb.equal(raiz.get("pessoaId"), f.pessoaId()));
            }
            if (f.categoriaId() != null) {
                condicoes.add(cb.equal(raiz.get("categoriaId"), f.categoriaId()));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return lancamentos.findAll(filtro, paginacao).map(LancamentoRepositoryAdapter::paraDominio);
    }

    @Override
    public void excluir(Lancamento lancamento) {
        lancamentos.deleteById(lancamento.id());
    }

    @Override
    public void registrarPagamentoDeFatura(
            UUID contaId, UUID cartaoId, YearMonth competencia, long valorCentavos, LocalDate data, UUID carteiraId) {
        PagamentoFaturaEntity e = PagamentoFaturaEntity.novo();
        e.id = UUID.randomUUID();
        e.contaId = contaId;
        e.cartaoId = cartaoId;
        e.competencia = competencia.toString();
        e.valor = Dinheiro.paraBanco(valorCentavos);
        e.data = data;
        e.carteiraId = carteiraId;
        e.criadoEm = clock.instant();
        try {
            faturas.saveAndFlush(e);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("FATURA_JA_PAGA", "Essa fatura já foi paga.");
        }
    }

    @Override
    public boolean excluirPagamentoDeFatura(UUID contaId, UUID cartaoId, YearMonth competencia) {
        return faturas.deleteByContaIdAndCartaoIdAndCompetencia(contaId, cartaoId, competencia.toString()) > 0;
    }

    @Override
    public Map<Integer, Long> ocorrenciasPagas(UUID contaId, UUID compromissoId) {
        return lancamentos.findByContaIdAndCompromissoId(contaId, compromissoId).stream()
                .collect(Collectors.toMap(e -> e.numeroOcorrencia, e -> Dinheiro.deBanco(e.valor)));
    }

    @Override
    public List<FaturaPaga> faturasPagasDetalhadas(UUID contaId, UUID cartaoId) {
        return faturas.findByContaIdAndCartaoId(contaId, cartaoId).stream()
                .map(e -> new FaturaPaga(YearMonth.parse(e.competencia), Dinheiro.deBanco(e.valor), e.criadoEm))
                .toList();
    }

    private static Lancamento paraDominio(LancamentoEntity e) {
        return new Lancamento(
                e.id,
                e.contaId,
                Lancamento.Tipo.valueOf(e.tipo),
                e.descricao,
                Dinheiro.deBanco(e.valor),
                e.data,
                e.carteiraId,
                e.carteiraDestinoId,
                e.compromissoId,
                e.numeroOcorrencia,
                e.pessoaId,
                e.categoriaId,
                e.ignorado,
                e.criadoEm);
    }
}
