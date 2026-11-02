package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import io.github.rodrigomuller0018.sobramar.shared.config.SegurancaProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Cifra segredos guardados no banco com AES-GCM. A chave fica no ambiente, fora do banco e do Git. */
@Component
public class CifradorDeSegredos {

    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int BYTES_DO_NONCE = 12;
    private static final int BITS_DA_TAG = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKeySpec chave;

    public CifradorDeSegredos(SegurancaProperties propriedades) {
        this.chave = new SecretKeySpec(Base64.getDecoder().decode(propriedades.chaveCifragem()), "AES");
    }

    public String cifrar(String texto) {
        try {
            byte[] nonce = new byte[BYTES_DO_NONCE];
            ALEATORIO.nextBytes(nonce);
            Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
            cifra.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(BITS_DA_TAG, nonce));
            byte[] cifrado = cifra.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = ByteBuffer.allocate(nonce.length + cifrado.length)
                    .put(nonce)
                    .put(cifrado)
                    .array();
            return Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    public String decifrar(String cifradoBase64) {
        try {
            byte[] entrada = Base64.getDecoder().decode(cifradoBase64);
            Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
            cifra.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(BITS_DA_TAG, entrada, 0, BYTES_DO_NONCE));
            byte[] texto = cifra.doFinal(entrada, BYTES_DO_NONCE, entrada.length - BYTES_DO_NONCE);
            return new String(texto, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao decifrar", e);
        }
    }
}
