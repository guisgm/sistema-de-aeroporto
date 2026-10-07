package br.edu.aeroporto.dominio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MapaAssentos {
    private final int[] filas;
    private final String[] colunas;
    private final String[][] celulas;

    public MapaAssentos(List<Map<String, Object>> assentos) {
        List<Integer> numeros = new ArrayList<>();
        List<String> letras = new ArrayList<>();
        for (Map<String, Object> assento : assentos) {
            int fila = ((Number) assento.get("fila")).intValue();
            String coluna = assento.get("coluna").toString();
            if (!numeros.contains(fila)) numeros.add(fila);
            if (!letras.contains(coluna)) letras.add(coluna);
        }
        Collections.sort(numeros);
        Collections.sort(letras);
        filas = new int[numeros.size()];
        for (int i = 0; i < numeros.size(); i++) filas[i] = numeros.get(i);
        colunas = letras.toArray(new String[0]);
        celulas = new String[filas.length][colunas.length];
        for (String[] fila : celulas) Arrays.fill(fila, "--");
        for (Map<String, Object> assento : assentos) {
            int fila = Arrays.binarySearch(filas, ((Number) assento.get("fila")).intValue());
            int coluna = Arrays.binarySearch(colunas, assento.get("coluna").toString());
            String estado =
                    Boolean.TRUE.equals(assento.get("bloqueado"))
                            ? "B"
                            : Boolean.TRUE.equals(assento.get("ocupado")) ? "O" : "L";
            celulas[fila][coluna] = assento.get("codigo") + ":" + estado;
        }
    }

    public int[] filas() {
        return Arrays.copyOf(filas, filas.length);
    }

    public String[] colunas() {
        return Arrays.copyOf(colunas, colunas.length);
    }

    public String[][] celulas() {
        String[][] copia = new String[celulas.length][];
        for (int i = 0; i < celulas.length; i++)
            copia[i] = Arrays.copyOf(celulas[i], celulas[i].length);
        return copia;
    }

    public boolean mesmoMapa(MapaAssentos outro) {
        return outro != null
                && Arrays.equals(filas, outro.filas)
                && Arrays.equals(colunas, outro.colunas)
                && Arrays.deepEquals(celulas, outro.celulas);
    }
}
