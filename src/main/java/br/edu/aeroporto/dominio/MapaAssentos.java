package br.edu.aeroporto.dominio;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class MapaAssentos {
    private final int[] filas;
    private final String[] colunas;
    private final String[][] celulas;

    public MapaAssentos(List<Map<String,Object>> assentos) {
        filas=assentos.stream().mapToInt(a->((Number)a.get("fila")).intValue()).distinct().sorted().toArray();
        colunas=assentos.stream().map(a->a.get("coluna").toString()).distinct().toArray(String[]::new);
        Arrays.sort(colunas);
        celulas=new String[filas.length][colunas.length];
        for(String[] fila:celulas) Arrays.fill(fila,"--");
        for(var assento:assentos) {
            int fila=Arrays.binarySearch(filas,((Number)assento.get("fila")).intValue());
            int coluna=Arrays.binarySearch(colunas,assento.get("coluna").toString());
            String estado=Boolean.TRUE.equals(assento.get("bloqueado"))?"B":Boolean.TRUE.equals(assento.get("ocupado"))?"O":"L";
            celulas[fila][coluna]=assento.get("codigo")+":"+estado;
        }
    }

    public int[] filas() { return Arrays.copyOf(filas,filas.length); }
    public String[] colunas() { return Arrays.copyOf(colunas,colunas.length); }
    public String[][] celulas() {
        String[][] copia=new String[celulas.length][];
        Arrays.setAll(copia,i->Arrays.copyOf(celulas[i],celulas[i].length));
        return copia;
    }

    public boolean mesmoMapa(MapaAssentos outro) {
        return outro!=null && Arrays.equals(filas,outro.filas) && Arrays.equals(colunas,outro.colunas) && Arrays.deepEquals(celulas,outro.celulas);
    }
}
