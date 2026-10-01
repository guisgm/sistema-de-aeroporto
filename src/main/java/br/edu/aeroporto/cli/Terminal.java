package br.edu.aeroporto.cli;

import java.io.Console;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class Terminal implements AutoCloseable {
    private final Console console = System.console();
    private final Scanner entrada = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);

    public String ler(String rotulo) {
        return lerBruto(rotulo).strip();
    }

    private String lerBruto(String rotulo) {
        if (console != null) {
            String linha = console.readLine("%s: ", rotulo);
            if (linha == null) throw new FimDaEntrada();
            return linha;
        }
        System.out.print(rotulo + ": ");
        if (!entrada.hasNextLine()) throw new FimDaEntrada();
        return entrada.nextLine();
    }

    public char[] senha(String rotulo) {
        if (console != null) {
            char[] senha = console.readPassword("%s: ", rotulo);
            if (senha == null) throw new FimDaEntrada();
            return senha;
        }
        System.out.println("Este terminal não oferece entrada oculta; a senha ficará visível ao digitar.");
        return lerBruto(rotulo).toCharArray();
    }

    public int inteiro(String rotulo, int minimo, int maximo) {
        while (true) {
            try {
                int valor = Integer.parseInt(ler(rotulo));
                if (valor >= minimo && valor <= maximo) return valor;
            } catch (NumberFormatException erro) {
                // Entrada incorreta mantém o usuário no campo atual.
            }
            System.out.printf("Informe um número entre %d e %d.%n", minimo, maximo);
        }
    }

    public long id(String rotulo) {
        while (true) {
            try {
                long valor = Long.parseLong(ler(rotulo));
                if (valor > 0) return valor;
            } catch (NumberFormatException erro) {
                // IDs podem exceder a capacidade de int.
            }
            System.out.println("Informe um id positivo.");
        }
    }

    public LocalDate data(String rotulo) {
        while (true) {
            try {
                return LocalDate.parse(ler(rotulo + " (AAAA-MM-DD)"));
            } catch (DateTimeParseException erro) {
                System.out.println("Data inválida. Exemplo: 2000-06-15.");
            }
        }
    }

    @Override
    public void close() { entrada.close(); }

    public static final class FimDaEntrada extends RuntimeException {}
}
