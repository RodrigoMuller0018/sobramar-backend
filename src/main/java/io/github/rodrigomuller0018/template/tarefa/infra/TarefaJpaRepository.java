package io.github.rodrigomuller0018.template.tarefa.infra;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TarefaJpaRepository extends JpaRepository<TarefaEntity, UUID> {}
