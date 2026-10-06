package br.edu.aeroporto.dominio;

import java.math.BigDecimal;
import java.util.Set;

public record Tarifa(String codigo,String classe,BigDecimal base,BigDecimal taxa,BigDecimal franquia,int pecas,
                     boolean permiteCancelar,BigDecimal multa) {
    public Tarifa {
        codigo=Validacao.texto(codigo,"Codigo da tarifa",20);
        Dados.exigir(Set.of("ECONOMICA","EXECUTIVA","PRIMEIRA").contains(classe),"Classe invalida.");
        base=Dados.valor(base,false); taxa=Dados.valor(taxa,false); franquia=Dados.valor(franquia,false); multa=Dados.valor(multa,false);
        Dados.exigir(pecas>=0 && pecas<=10,"Limite de pecas invalido.");
        Dados.exigir(multa.compareTo(base.add(taxa))<=0,"Multa supera a tarifa.");
    }
}
