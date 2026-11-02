package io.github.rodrigomuller0018.sobramar.categoria.infra;

import io.github.rodrigomuller0018.sobramar.categoria.dominio.Categoria;
import io.github.rodrigomuller0018.sobramar.categoria.dominio.CategoriaRepository;
import io.github.rodrigomuller0018.sobramar.shared.erro.ConflitoException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class CategoriaRepositoryAdapter implements CategoriaRepository {

    private final CategoriaJpaRepository jpa;

    CategoriaRepositoryAdapter(CategoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Categoria salvar(Categoria c) {
        try {
            return paraDominio(jpa.saveAndFlush(
                    new CategoriaEntity(c.getId(), c.getContaId(), c.getNome(), c.isArquivada(), c.getCriadoEm())));
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException("CATEGORIA_DUPLICADA", "Já existe uma categoria com esse nome.");
        }
    }

    @Override
    public Optional<Categoria> buscar(UUID contaId, UUID id) {
        return jpa.findByContaIdAndId(contaId, id).map(CategoriaRepositoryAdapter::paraDominio);
    }

    @Override
    public Page<Categoria> listar(UUID contaId, boolean incluirArquivadas, Pageable paginacao) {
        Page<CategoriaEntity> pagina = incluirArquivadas
                ? jpa.findByContaId(contaId, paginacao)
                : jpa.findByContaIdAndArquivadaFalse(contaId, paginacao);
        return pagina.map(CategoriaRepositoryAdapter::paraDominio);
    }

    private static Categoria paraDominio(CategoriaEntity e) {
        return Categoria.restaurar(e.getId(), e.getContaId(), e.getNome(), e.isArquivada(), e.getCriadoEm());
    }
}
