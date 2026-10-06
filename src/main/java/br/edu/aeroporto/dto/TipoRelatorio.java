package br.edu.aeroporto.dto;

public enum TipoRelatorio {
    OCUPACAO("""
        SELECT v.id,v.numero,v.situacao,COUNT(i.id) AS capacidade,
          COUNT(i.id) FILTER (WHERE NOT i.bloqueado) AS vendaveis,
          COUNT(o.id) AS ocupados,ROUND(100.0*COUNT(o.id)/NULLIF(COUNT(i.id) FILTER (WHERE NOT i.bloqueado),0),2) AS ocupacao_percentual
        FROM voo v LEFT JOIN inventario_assento_voo i ON i.voo_id=v.id
        LEFT JOIN ocupacao_assento o ON o.inventario_id=i.id AND o.liberada_em IS NULL GROUP BY v.id ORDER BY v.id
        """,false),
    ATRASOS("""
        SELECT v.id,v.numero,v.situacao,o.codigo_icao AS origem,d.codigo_icao AS destino,
          v.partida_prevista,v.chegada_prevista,v.partida_estimada,v.chegada_estimada,v.partida_real,v.chegada_real,
          EXTRACT(EPOCH FROM (COALESCE(v.partida_real,v.partida_estimada,v.partida_prevista)-v.partida_prevista))/60 AS atraso_partida_min,
          EXTRACT(EPOCH FROM (COALESCE(v.chegada_real,v.chegada_estimada,v.chegada_prevista)-v.chegada_prevista))/60 AS atraso_chegada_min
        FROM voo v JOIN rota r ON r.id=v.rota_id JOIN aeroporto o ON o.id=r.origem_id JOIN aeroporto d ON d.id=r.destino_id ORDER BY v.id
        """,false),
    MANIFESTO("""
        SELECT i.id,i.reserva_id,i.voo_id,p.nome,i.ordem_trecho,i.situacao,a.codigo AS assento,b.numero AS bilhete,
          ci.realizado_em AS checkin,e.realizado_em AS embarque
        FROM item_reserva i JOIN pessoa p ON p.id=i.passageiro_id
        LEFT JOIN ocupacao_assento o ON o.item_id=i.id AND o.liberada_em IS NULL
        LEFT JOIN inventario_assento_voo a ON a.id=o.inventario_id LEFT JOIN bilhete b ON b.item_id=i.id
        LEFT JOIN check_in ci ON ci.item_id=i.id AND ci.cancelado_em IS NULL LEFT JOIN embarque e ON e.checkin_id=ci.id ORDER BY i.voo_id,i.id
        """,true),
    VENDAS("""
        SELECT r.id,r.localizador,r.situacao,r.criada_em,r.expira_em,COUNT(i.id) AS itens,
          COALESCE(SUM(i.valor_base+i.taxa_embarque-i.desconto),0) AS total_original,
          COALESCE(SUM(i.valor_base+i.taxa_embarque-i.desconto) FILTER (WHERE i.situacao NOT IN ('CANCELADO','EXPIRADO')),0) AS total_vigente,
          COALESCE((SELECT SUM(p.valor) FROM pagamento p WHERE p.reserva_id=r.id AND p.situacao='APROVADO'),0) AS pago,
          COALESCE((SELECT SUM(e.valor) FROM reembolso e WHERE e.reserva_id=r.id AND e.situacao='PROCESSADO'),0) AS devolvido
        FROM reserva r LEFT JOIN item_reserva i ON i.reserva_id=r.id GROUP BY r.id ORDER BY r.id
        """,true),
    PAGAMENTOS("SELECT id,reserva_id,forma,valor,situacao,criado_em,processado_em FROM pagamento ORDER BY id",true),
    REEMBOLSOS("SELECT id,pagamento_id,reserva_id,valor,situacao,solicitado_em,processado_em FROM reembolso ORDER BY id",true),
    RECURSOS("SELECT a.id,r.aeroporto_id,r.tipo,r.codigo,a.voo_id,a.finalidade,a.inicio,a.fim,a.ativa FROM alocacao_recurso a JOIN recurso_aeroportuario r ON r.id=a.recurso_id ORDER BY a.inicio,a.id",false),
    AGENDAS("SELECT id,aeronave_id,voo_id,manutencao_id,inicio,fim,ativa FROM agenda_aeronave ORDER BY inicio,id",false),
    ESCALAS("SELECT e.id,e.funcionario_id,p.nome,e.aeroporto_id,e.voo_id,e.funcao,e.inicio,e.fim,e.ativa FROM escala_funcionario e JOIN pessoa p ON p.id=e.funcionario_id ORDER BY e.inicio,e.id",false),
    HISTORICO("SELECT id,voo_id,usuario_id,alterado_em,situacao_anterior,situacao_nova,motivo FROM historico_voo ORDER BY id",false),
    AUDITORIA("SELECT id,usuario_id,ocorrido_em,acao,entidade,registro_id FROM evento_auditoria ORDER BY id",true),
    BAGAGENS("SELECT b.id,b.etiqueta,ci.voo_id,ci.item_id,b.peso_kg,b.categoria,b.taxa_excesso,b.situacao FROM bagagem b JOIN check_in ci ON ci.id=b.checkin_id ORDER BY b.id",true),
    MANUTENCOES("SELECT id,aeronave_id,responsavel_id,tipo,situacao,inicio_real,fim_real,custo FROM manutencao_aeronave ORDER BY id",false),
    SOLO("SELECT id,voo_id,aeroporto_id,responsavel_id,tipo,situacao,inicio,fim,quantidade,unidade,custo FROM servico_solo ORDER BY id",false),
    OCORRENCIAS("SELECT id,aeroporto_id,voo_id,tipo,gravidade,aberta_em,encerrada_em FROM ocorrencia_operacional ORDER BY id",false),
    TARIFAS("SELECT * FROM tarifa_voo ORDER BY voo_id,id",false);

    private final String sql;
    private final boolean restrito;
    TipoRelatorio(String sql,boolean restrito) { this.sql=sql;this.restrito=restrito; }
    public String sql() { return sql; }
    public boolean restrito() { return restrito; }
}
