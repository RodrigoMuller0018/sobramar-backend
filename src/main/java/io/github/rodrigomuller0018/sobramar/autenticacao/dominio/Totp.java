package io.github.rodrigomuller0018.sobramar.autenticacao.dominio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Código de segundo fator (TOTP, RFC 6238), com janela de um passo para compensar relógios desalinhados. */
public final class Totp {

    private static final long SEGUNDOS_POR_PASSO = 30;
    private static final int DIGITOS = 6;
    private static final int MODULO = 1_000_000;
    private static final int BYTES_DO_SEGREDO = 20;
    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Totp() {}

    public static String novoSegredoBase32() {
        byte[] bytes = new byte[BYTES_DO_SEGREDO];
        ALEATORIO.nextBytes(bytes);
        return paraBase32(bytes);
    }

    public static String codigo(String segredoBase32, Instant instante) {
        return codigoDoPasso(deBase32(segredoBase32), instante.getEpochSecond() / SEGUNDOS_POR_PASSO);
    }

    /**
     * Devolve o passo aceito, para quem chama impedir o reuso do código.
     * Só aceita passos posteriores a {@code ultimoPassoUsado}.
     */
    public static OptionalLong validar(String segredoBase32, String codigo, Instant instante, Long ultimoPassoUsado) {
        byte[] segredo = deBase32(segredoBase32);
        long passoAtual = instante.getEpochSecond() / SEGUNDOS_POR_PASSO;
        for (long passo = passoAtual - 1; passo <= passoAtual + 1; passo++) {
            boolean naoUsado = ultimoPassoUsado == null || passo > ultimoPassoUsado;
            if (naoUsado && iguais(codigoDoPasso(segredo, passo), codigo)) {
                return OptionalLong.of(passo);
            }
        }
        return OptionalLong.empty();
    }

    private static boolean iguais(String esperado, String recebido) {
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }

    private static String codigoDoPasso(byte[] segredo, long passo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(segredo, "HmacSHA1"));
            byte[] hash =
                    mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(passo).array());
            int deslocamento = hash[hash.length - 1] & 0x0F;
            int valor = ByteBuffer.wrap(hash, deslocamento, Integer.BYTES).getInt() & Integer.MAX_VALUE;
            return String.format("%0" + DIGITOS + "d", valor % MODULO);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponível", e);
        }
    }

    private static String paraBase32(byte[] dados) {
        StringBuilder saida = new StringBuilder();
        int acumulado = 0;
        int bits = 0;
        for (byte dado : dados) {
            acumulado = acumulado << 8 | dado & 0xFF;
            bits += 8;
            while (bits >= 5) {
                saida.append(ALFABETO_BASE32.charAt(acumulado >> bits - 5 & 0x1F));
                bits -= 5;
            }
            acumulado &= (1 << bits) - 1;
        }
        if (bits > 0) {
            saida.append(ALFABETO_BASE32.charAt(acumulado << 5 - bits & 0x1F));
        }
        return saida.toString();
    }

    private static byte[] deBase32(String texto) {
        ByteBuffer saida = ByteBuffer.allocate(texto.length() * 5 / 8);
        int acumulado = 0;
        int bits = 0;
        for (char caractere : texto.toCharArray()) {
            acumulado = acumulado << 5 | ALFABETO_BASE32.indexOf(caractere);
            bits += 5;
            if (bits >= 8) {
                saida.put((byte) (acumulado >> bits - 8));
                bits -= 8;
                acumulado &= (1 << bits) - 1;
            }
        }
        return saida.array();
    }
}
