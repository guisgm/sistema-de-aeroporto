package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

public final class ArquivosServico {
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;
    private final PassageiroJdbc passageiros=new PassageiroJdbc();

    public ArquivosServico(BancoDados banco,AutorizacaoJdbc autorizacao,AuditoriaJdbc auditoria,Clock relogio) {
        this.banco=banco;this.autorizacao=autorizacao;this.auditoria=auditoria;this.relogio=relogio;
    }

    public int importarPassageiros(Sessao s,Path arquivo) throws IOException {
        banco.consultar(c->{autorizacao.exigirAtendimento(c,s);return null;});
        File fonte=arquivo.toFile();
        Dados.exigir(fonte.exists() && fonte.isFile() && fonte.canRead() && fonte.length()<=5_000_000,"Importacao requer arquivo legivel de ate 5 MB.");
        var cadastros=new ArrayList<CadastroPassageiro>();var cpfs=new HashSet<String>();int linha=0;
        try(BufferedReader reader=new BufferedReader(new FileReader(fonte,StandardCharsets.UTF_8))) {
            String texto;
            while((texto=reader.readLine())!=null) {
                linha++;if(linha==1) { Dados.exigir(texto.equals("nome\tnascimento\tcpf\temail\ttelefone"),"Cabecalho de importacao invalido.");continue; }
                if(texto.isBlank()) continue;
                String[] campos=texto.split("\t",-1);
                Dados.exigir(campos.length==5,"Numero de campos invalido na linha "+linha);
                try {
                    var cadastro=new CadastroPassageiro(campos[0],LocalDate.parse(campos[1]),campos[2],campos[3],campos[4]);
                    Validacao.nascimento(cadastro.nascimento(),LocalDate.now(relogio));
                    Dados.exigir(!cadastro.cpf().isEmpty() && cpfs.add(cadastro.cpf()),"CPF obrigatorio ou repetido na importacao.");cadastros.add(cadastro);
                    Dados.exigir(cadastros.size()<=1000,"Limite de 1000 passageiros por arquivo.");
                } catch(RuntimeException erro) { throw new br.edu.aeroporto.excecao.RegraNegocioException("Dados invalidos na linha "+linha+"; importacao nao gravada."); }
            }
        }
        Dados.exigir(linha>0 && !cadastros.isEmpty(),"Arquivo sem passageiros.");
        return banco.transacao(c->{
            autorizacao.exigirAtendimento(c,s);
            for(var cadastro:cadastros) {
                long id=passageiros.cadastrar(c,cadastro).id();auditoria.registrar(c,s.usuarioId(),"IMPORTAR_PASSAGEIRO","passageiro",id);
            }
            return cadastros.size();
        });
    }

    public List<Path> listar(Sessao s) throws IOException {
        banco.consultar(c->{autorizacao.exigirAtendimento(c,s);return null;});
        Path pasta=Path.of("relatorios");Files.createDirectories(pasta);
        try(var arquivos=Files.list(pasta)) { return arquivos.filter(Files::isRegularFile).filter(p->p.toString().endsWith(".csv") || p.toString().endsWith(".txt")).sorted().toList(); }
    }

    public Path copiarRelatorio(Sessao s,Path arquivo) throws IOException {
        banco.consultar(c->{autorizacao.exigirAtendimento(c,s);return null;});
        Path raiz=Path.of("relatorios").toAbsolutePath().normalize().toRealPath(),origem=arquivo.toAbsolutePath().normalize().toRealPath();
        Dados.exigir(origem.startsWith(raiz) && Files.isRegularFile(origem) && !origem.startsWith(raiz.resolve("backup")),"Escolha um relatorio original da pasta relatorios.");
        Path pasta=raiz.resolve("backup");Files.createDirectories(pasta);Path temporario=Files.createTempFile(pasta,"copia-",".tmp"),destino=pasta.resolve(UUID.randomUUID()+"-"+origem.getFileName());
        try {
            try(InputStream in=new BufferedInputStream(Files.newInputStream(origem));OutputStream out=new BufferedOutputStream(Files.newOutputStream(temporario))) { in.transferTo(out); }
            Dados.exigir(MessageDigest.isEqual(hash(origem),hash(temporario)),"Copia falhou na verificacao SHA-256.");
            banco.transacao(c->{autorizacao.exigirAtendimento(c,s);auditoria.registrar(c,s.usuarioId(),"COPIAR_RELATORIO","arquivo",0);return null;});
            return Files.move(temporario,destino);
        } catch(RuntimeException | IOException erro) { try { Files.deleteIfExists(temporario); } catch(IOException limpeza) { erro.addSuppressed(limpeza); } throw erro; }
    }

    private byte[] hash(Path arquivo) throws IOException {
        try { MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new DigestInputStream(Files.newInputStream(arquivo),digest)) { in.transferTo(OutputStream.nullOutputStream()); } return digest.digest(); }
        catch(NoSuchAlgorithmException erro) { throw new IllegalStateException("SHA-256 indisponivel.",erro); }
    }
}
