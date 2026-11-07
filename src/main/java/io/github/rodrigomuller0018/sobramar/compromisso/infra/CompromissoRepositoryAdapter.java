package io.github.rodrigomuller0018.sobramar.compromisso.infra;

import io.github.rodrigomuller0018.sobramar.compromisso.dominio.AjusteOcorrencia;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.CompromissoRepository;
import io.github.rodrigomuller0018.sobramar.shared.dominio.Dinheiro;
import jakarta.persistence.criteria.Predicate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class CompromissoRepositoryAdapter implements CompromissoRepository {

    private final CompromissoJpaRepository compromissos;
    private final OcorrenciaAjusteJpaRepository ajustes;

    CompromissoRepositoryAdapter(CompromissoJpaRepository compromissos, OcorrenciaAjusteJpaRepository ajustes) {
        this.compromissos = compromissos;
        this.ajustes = ajustes;
    }

    @Override
    public Compromisso salvar(Compromisso c) {
        CompromissoEntity e = new CompromissoEntity();
        e.id = c.id();
        e.contaId = c.contaId();
        e.natureza = c.natureza().name();
        e.forma = c.forma().name();
        e.certeza = c.certeza().name();
        e.descricao = c.descricao();
        e.valor = Dinheiro.paraBanco(c.valorCentavos());
        e.parcelas = c.parcelas();
        e.dataInicio = c.dataInicio();
        e.dataFim = c.dataFim();
        e.recorrenciaAnual = c.recorrenciaAnual();
        e.carteiraId = c.carteiraId();
        e.cartaoId = c.cartaoId();
        e.faturaInicial = c.faturaInicial() == null ? null : c.faturaInicial().toString();
        e.pessoaId = c.pessoaId();
        e.categoriaId = c.categoriaId();
        e.parte = c.parte();
        e.canceladoAPartirDe = c.canceladoAPartirDe();
        e.criadoEm = c.criadoEm();
        return paraDominio(compromissos.save(e));
    }

    @Override
    public Optional<Compromisso> buscar(UUID contaId, UUID id) {
        return compromissos.findByContaIdAndId(contaId, id).map(CompromissoRepositoryAdapter::paraDominio);
    }

    @Override
    public Page<Compromisso> listar(
            UUID contaId,
            Compromisso.Natureza natureza,
            Compromisso.Forma forma,
            UUID pessoaId,
            boolean somenteAtivos,
            Pageable paginacao) {
        Specification<CompromissoEntity> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(raiz.get("contaId"), contaId));
            if (natureza != null) {
                condicoes.add(cb.equal(raiz.get("natureza"), natureza.name()));
            }
            if (forma != null) {
                condicoes.add(cb.equal(raiz.get("forma"), forma.name()));
            }
            if (pessoaId != null) {
                condicoes.add(cb.equal(raiz.get("pessoaId"), pessoaId));
            }
            if (somenteAtivos) {
                condicoes.add(cb.isNull(raiz.get("canceladoAPartirDe")));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return compromissos.findAll(filtro, paginacao).map(CompromissoRepositoryAdapter::paraDominio);
    }

    @Override
    public List<Compromisso> listarDoCartao(UUID contaId, UUID cartaoId) {
        return compromissos.findByContaIdAndCartaoId(contaId, cartaoId).stream()
                .map(CompromissoRepositoryAdapter::paraDominio)
                .toList();
    }

    @Override
    public void excluir(Compromisso compromisso) {
        compromissos.deleteById(compromisso.id());
    }

    @Override
    public List<AjusteOcorrencia> ajustes(UUID compromissoId) {
        return ajustes.findByCompromissoId(compromissoId).stream()
                .map(e -> new AjusteOcorrencia(
                        e.numero,
                        e.valor == null ? null : Dinheiro.deBanco(e.valor),
                        e.vencimento,
                        e.fatura == null ? null : YearMonth.parse(e.fatura),
                        e.ignorada,
                        e.propaga))
                .toList();
    }

    @Override
    public void salvarAjuste(UUID compromissoId, AjusteOcorrencia a) {
        OcorrenciaAjusteEntity e =
                ajustes.findByCompromissoIdAndNumero(compromissoId, a.numero()).orElseGet(OcorrenciaAjusteEntity::new);
        if (e.id == null) {
            e.id = UUID.randomUUID();
            e.compromissoId = compromissoId;
            e.numero = a.numero();
        }
        e.valor = a.valorCentavos() == null ? null : Dinheiro.paraBanco(a.valorCentavos());
        e.vencimento = a.vencimento();
        e.fatura = a.fatura() == null ? null : a.fatura().toString();
        e.ignorada = a.ignorada();
        e.propaga = a.propaga();
        ajustes.saveAndFlush(e);
    }

    private static Compromisso paraDominio(CompromissoEntity e) {
        return new Compromisso(
                e.id,
                e.contaId,
                Compromisso.Natureza.valueOf(e.natureza),
                Compromisso.Forma.valueOf(e.forma),
                Compromisso.Certeza.valueOf(e.certeza),
                e.descricao,
                Dinheiro.deBanco(e.valor),
                e.parcelas,
                e.dataInicio,
                e.dataFim,
                e.recorrenciaAnual,
                e.carteiraId,
                e.cartaoId,
                e.faturaInicial == null ? null : YearMonth.parse(e.faturaInicial),
                e.pessoaId,
                e.categoriaId,
                e.parte,
                e.canceladoAPartirDe,
                e.criadoEm);
    }
}
