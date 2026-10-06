package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class DadosFicticiosServico {
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;

    public DadosFicticiosServico(BancoDados banco,AutorizacaoJdbc autorizacao,AuditoriaJdbc auditoria,Clock relogio) {
        this.banco=banco;this.autorizacao=autorizacao;this.auditoria=auditoria;this.relogio=relogio;
    }

    public Map<String,Object> criar(Sessao s) {
        return criar(s,"PRINCIPAL");
    }

    public Map<String,Object> criar(Sessao s,String cenario) {
        String chave=Validacao.texto(cenario,"Cenario",8).toUpperCase(Locale.ROOT);
        Dados.exigir(chave.matches("[A-Z0-9]{1,8}"),"Identificador de cenario invalido.");
        boolean principal=chave.equals("PRINCIPAL");
        return banco.transacao(c->{
            autorizacao.exigirAdministrador(c,s);
            Sql.unico(c,"SELECT pg_advisory_xact_lock(hashtext('aeroporto.demo'))",r->true);
            var existente=Sql.unico(c,"""
                    SELECT registro_id AS voo,(detalhes->>'passageiro')::bigint AS passageiro,(detalhes->>'documento')::bigint AS documento,
                        (detalhes->>'tarifa')::bigint AS tarifa,(detalhes->>'alocacao')::bigint AS alocacao
                    FROM evento_auditoria WHERE acao='CRIAR_DEMO' AND COALESCE(detalhes->>'cenario','PRINCIPAL')=? ORDER BY id LIMIT 1
                    """,Sql::linha,chave);
            if(existente.isPresent()) return existente.get();
            var planejamento=new PlanejamentoJdbc();planejamento.serializar(c);
            Sql.executar(c,"INSERT INTO pais(codigo,nome) VALUES ('ZZ','Pais FICTICIO de demonstracao') ON CONFLICT DO NOTHING");
            long pais=Dados.id(Sql.registro(c,"SELECT id FROM pais WHERE codigo='ZZ'"),"id");
            long cidade=Sql.inserir(c,"INSERT INTO cidade(pais_id,nome) VALUES (?,?) RETURNING id",pais,"Cidade FICTICIA DEMO "+chave);
            long origem=Sql.inserir(c,"INSERT INTO aeroporto(cidade_id,codigo_icao,nome,fuso_horario) VALUES (?,?,'Origem FICTICIA DEMO','America/Sao_Paulo') RETURNING id",cidade,principal?"XDEA":codigoLivre(c,true));
            long destino=Sql.inserir(c,"INSERT INTO aeroporto(cidade_id,codigo_icao,nome,fuso_horario) VALUES (?,?,'Destino FICTICIO DEMO','America/Sao_Paulo') RETURNING id",cidade,principal?"XDEB":codigoLivre(c,true));
            long terminal=Sql.inserir(c,"INSERT INTO terminal(aeroporto_id,codigo,nome) VALUES (?,'DEMO','Terminal FICTICIO') RETURNING id",origem);
            long portao=Sql.inserir(c,"INSERT INTO recurso_aeroportuario(aeroporto_id,terminal_id,tipo,codigo,envergadura_max_m) VALUES (?,?,'PORTAO','DEMO',40) RETURNING id",origem,terminal);
            long companhia=Sql.inserir(c,"INSERT INTO companhia_aerea(pais_id,codigo_icao,nome) VALUES (?,?,'Companhia FICTICIA DEMO') RETURNING id",pais,principal?"FIC":codigoLivre(c,false));
            long modelo=Sql.inserir(c,"INSERT INTO modelo_aeronave(fabricante,nome,envergadura_m,comprimento_pista_min_m) VALUES ('FICTICIO',?,30,1500) RETURNING id","DEMO-"+chave);
            long aeronave=Sql.inserir(c,"INSERT INTO aeronave(modelo_id,companhia_id,matricula,fabricacao_ano) VALUES (?,?,?,2025) RETURNING id",modelo,companhia,"DEMO-"+chave);
            var codigos=new ArrayList<>(List.of("1A","1B","2A","2B","3A"));Collections.shuffle(codigos,new Random(42));
            for(String codigo:codigos) Sql.executar(c,"INSERT INTO assento_aeronave(aeronave_id,codigo,fila,coluna,classe) VALUES (?,?,?,?,'ECONOMICA')",aeronave,codigo,Integer.parseInt(codigo.substring(0,1)),codigo.substring(1));
            long rota=Sql.inserir(c,"INSERT INTO rota(origem_id,destino_id,distancia_km) VALUES (?,?,1000) RETURNING id",origem,destino);
            Instant partida=relogio.instant().plus(Duration.ofHours(2)),chegada=partida.plus(Duration.ofHours(2));
            auditoria.definirAutorDoVoo(c,s.usuarioId(),"Demonstracao FICTICIA");
            long voo=planejamento.programar(c,new ProgramacaoVoo(companhia,rota,aeronave,"DEMO01",partida,chegada,partida.minus(Duration.ofHours(3)),partida.minus(Duration.ofMinutes(45)),partida.minus(Duration.ofMinutes(40)),partida.minus(Duration.ofMinutes(10))));
            long tarifa=planejamento.tarifa(c,voo,null,new Tarifa("DEMO","ECONOMICA",new BigDecimal("100"),new BigDecimal("20"),new BigDecimal("23"),1,true,new BigDecimal("10")),true);
            long pessoa=new PassageiroJdbc().cadastrar(c,new CadastroPassageiro("Passageiro FICTICIO DEMO",LocalDate.of(1990,1,1),"","demo@example.invalid","")).id();
            Sql.executar(c,"UPDATE pessoa SET nacionalidade_id=? WHERE id=?",pais,pessoa);
            long documento=Sql.inserir(c,"INSERT INTO documento_pessoa(pessoa_id,pais_emissor_id,tipo,numero,validade) VALUES (?,?,'PASSAPORTE',?,?) RETURNING id",pessoa,pais,"FICTICIO-DEMOPAX-"+chave,LocalDate.now(relogio).plusYears(2));
            long cargo=Sql.inserir(c,"INSERT INTO cargo(nome,area) VALUES (?,'TRIPULACAO') RETURNING id","Tripulacao FICTICIA DEMO "+chave);
            for(String funcao:new String[]{"COMANDANTE","COPILOTO","COMISSARIO"}) {
                long tripulante=Sql.inserir(c,"INSERT INTO pessoa(nome,nascimento) VALUES (?,?) RETURNING id",funcao+" FICTICIO DEMO",LocalDate.of(1985,1,1));
                Sql.executar(c,"INSERT INTO funcionario(pessoa_id,cargo_id,aeroporto_base_id,companhia_id,matricula,admissao) VALUES (?,?,?,?,?,?)",tripulante,cargo,origem,companhia,"DEMO-"+funcao+"-"+chave,LocalDate.now(relogio).minusDays(1));
                Sql.executar(c,"INSERT INTO habilitacao_tripulante(funcionario_id,modelo_id,funcao,numero_licenca,validade) VALUES (?,?,?,?,?)",tripulante,modelo,funcao,"FICTICIA-"+funcao,LocalDate.now(relogio).plusYears(2));
                Sql.executar(c,"INSERT INTO escala_funcionario(funcionario_id,aeroporto_id,voo_id,funcao,inicio,fim) VALUES (?,?,?,?,?,?)",tripulante,origem,voo,funcao,partida.minus(Duration.ofMinutes(45)),chegada.plus(Duration.ofMinutes(30)));
            }
            long alocacao=Sql.inserir(c,"INSERT INTO alocacao_recurso(recurso_id,voo_id,finalidade,inicio,fim) VALUES (?,?,'PARTIDA',?,?) RETURNING id",portao,voo,partida.minus(Duration.ofMinutes(45)),partida);
            Sql.executar(c,"INSERT INTO evento_auditoria(usuario_id,acao,entidade,registro_id,detalhes) VALUES (?,'CRIAR_DEMO','voo',?,jsonb_build_object('passageiro',?::bigint,'documento',?::bigint,'tarifa',?::bigint,'alocacao',?::bigint,'cenario',?::text))",s.usuarioId(),voo,pessoa,documento,tarifa,alocacao,chave);
            return Map.of("voo",voo,"passageiro",pessoa,"documento",documento,"tarifa",tarifa,"alocacao",alocacao);
        });
    }

    private String icao() { var codigo=new StringBuilder("X");new java.security.SecureRandom().ints(3,0,26).forEach(n->codigo.append((char)('A'+n)));return codigo.toString(); }

    private String codigoLivre(java.sql.Connection c,boolean aeroporto) throws java.sql.SQLException {
        for(int tentativa=0;tentativa<1000;tentativa++) {
            String codigo=aeroporto?icao():icao().substring(1);
            String tabela=aeroporto?"aeroporto":"companhia_aerea";
            if(Sql.listar(c,"SELECT id FROM "+tabela+" WHERE codigo_icao=?",r->r.getLong(1),codigo).isEmpty()) return codigo;
        }
        throw new br.edu.aeroporto.excecao.RegraNegocioException("Sem codigo ficticio disponivel.");
    }
}
