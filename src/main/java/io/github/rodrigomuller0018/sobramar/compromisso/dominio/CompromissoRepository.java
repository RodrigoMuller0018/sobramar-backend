package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Porta de persistência. Toda consulta recebe a conta: não existe busca só por id. */
public interface CompromissoRepository {

    Compromisso salvar(Compromisso compromisso);

    Optional<Compromisso> buscar(UUID contaId, UUID id);

    Page<Compromisso> listar(
            UUID contaId,
            Compromisso.Natureza natureza,
            Compromisso.Forma forma,
            UUID pessoaId,
            boolean somenteAtivos,
            Pageable paginacao);

    List<Compromisso> listarDoCartao(UUID contaId, UUID cartaoId);

    void excluir(Compromisso compromisso);

    List<AjusteOcorrencia> ajustes(UUID compromissoId);

    void salvarAjuste(UUID compromissoId, AjusteOcorrencia ajuste);
}
