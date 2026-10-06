package br.edu.aeroporto;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.seguranca.Senhas;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class UnidadeTeste {
    public static void main(String[] args) {
        assertiva(Validacao.cpf("529.982.247-25").equals("52998224725"),"CPF valido normalizado");
        falha(()->Validacao.cpf("11111111111"),"CPF repetido rejeitado");
        falha(()->Validacao.nascimento(LocalDate.now().plusDays(1),LocalDate.now()),"Nascimento futuro rejeitado");
        falha(()->Dados.valor(new BigDecimal("1.001"),false),"Dinheiro com 3 casas rejeitado");
        assertiva(!SituacaoVoo.CONCLUIDO.permiteTransicaoPara(SituacaoVoo.PROGRAMADO),"Voo encerrado nao reabre");
        var linhas=List.of(Map.<String,Object>of("codigo","2C","fila",2,"coluna","C","ocupado",true,"bloqueado",false),Map.<String,Object>of("codigo","1A","fila",1,"coluna","A","ocupado",false,"bloqueado",false));
        var mapa=new MapaAssentos(linhas);
        assertiva(Arrays.deepEquals(mapa.celulas(),new String[][]{{"1A:L","--"},{"--","2C:O"}}),"Matriz irregular de assentos");
        var copia=mapa.celulas();copia[0][0]="alterado";
        assertiva(mapa.celulas()[0][0].equals("1A:L"),"Copia defensiva da matriz");
        assertiva(mapa.mesmoMapa(new MapaAssentos(linhas)),"Comparacao de mapas");
        falha(()->TipoCadastro.Campo.class.cast(new TipoCadastro.Campo("ativo","b",false)).converter("qualquer"),"Booleano invalido rejeitado");
        String hash=Senhas.gerar("SenhaFicticia!2026".toCharArray());
        assertiva(Senhas.conferir("SenhaFicticia!2026".toCharArray(),hash),"Senha correta");
        assertiva(!Senhas.conferir("incorreta".toCharArray(),hash),"Senha incorreta rejeitada");
        assertiva(!Senhas.conferir("senha".toCharArray(),"hash-invalido"),"Hash invalido rejeitado");
        System.out.println("12 verificacoes unitarias Java aprovadas.");
    }

    static void assertiva(boolean condicao,String nome) { if(!condicao) throw new AssertionError(nome); }
    static void falha(Runnable operacao,String nome) { try { operacao.run(); } catch(br.edu.aeroporto.excecao.RegraNegocioException esperado) { return; } throw new AssertionError(nome); }
}
