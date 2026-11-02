package io.github.rodrigomuller0018.sobramar.conta.dominio;

import java.time.Instant;
import java.util.UUID;

/** Dona dos dados financeiros. Hoje é sempre um usuário para uma conta (ADR 0002). */
public record Conta(UUID id, String nome, Instant criadoEm) {}
