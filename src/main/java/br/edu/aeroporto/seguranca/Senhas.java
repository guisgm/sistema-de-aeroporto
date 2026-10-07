package br.edu.aeroporto.seguranca;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Senhas {
    private static final int ITERACOES = 600_000;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senhas() {}

    public static String gerar(char[] senha) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(senha, sal, ITERACOES);
        try {
            return "pbkdf2-sha256$"
                    + ITERACOES
                    + "$"
                    + Base64.getEncoder().encodeToString(sal)
                    + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    public static boolean conferir(char[] senha, String armazenada) {
        String[] partes = armazenada.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2-sha256")) return false;
        byte[] calculado = null;
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (iteracoes < 100_000
                    || iteracoes > 2_000_000
                    || sal.length < 16
                    || esperado.length != 32) return false;
            calculado = derivar(senha, sal, iteracoes);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException formatoInvalido) {
            return false;
        } finally {
            if (calculado != null) Arrays.fill(calculado, (byte) 0);
        }
    }

    private static byte[] derivar(char[] senha, byte[] sal, int iteracoes) {
        PBEKeySpec chave = new PBEKeySpec(senha, sal, iteracoes, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(chave)
                    .getEncoded();
        } catch (GeneralSecurityException erro) {
            throw new IllegalStateException(
                    "O ambiente Java não suporta o algoritmo de senha.", erro);
        } finally {
            chave.clearPassword();
        }
    }
}
