package io.github.rodrigomuller0018.sobramar.conta.infra;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContaJpaRepository extends JpaRepository<ContaEntity, UUID> {}
