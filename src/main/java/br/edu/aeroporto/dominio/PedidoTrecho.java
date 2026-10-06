package br.edu.aeroporto.dominio;

import java.math.BigDecimal;

public record PedidoTrecho(long passageiro,long voo,long tarifa,int ordem,String assento,BigDecimal desconto) {
    public PedidoTrecho {
        Dados.exigir(passageiro>0 && voo>0 && tarifa>0 && ordem>0 && ordem<=32767,"Trecho invalido.");
        assento=assento==null || assento.isBlank()?null:Validacao.texto(assento,"Assento",6).toUpperCase(java.util.Locale.ROOT);
        desconto=Dados.valor(desconto,false);
    }
}
