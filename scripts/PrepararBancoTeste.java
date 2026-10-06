import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

public class PrepararBancoTeste {
    public static void main(String[] args) throws Exception {
        // Esta porta pertence exclusivamente ao cluster descartavel de artifacts.
        try(var c=DriverManager.getConnection("jdbc:postgresql://127.0.0.1:55439/postgres","aeroporto_teste","");var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT EXISTS (SELECT 1 FROM pg_database WHERE datname='sistema_aeroporto')")) {
                r.next();if(!r.getBoolean(1)) s.executeUpdate("CREATE DATABASE sistema_aeroporto");
            }
        }
        try(var c=DriverManager.getConnection("jdbc:postgresql://127.0.0.1:55439/sistema_aeroporto","aeroporto_teste","");var s=c.createStatement()) {
            boolean existe;
            try(var r=s.executeQuery("SELECT to_regclass('aeroporto.pessoa') IS NOT NULL")) { r.next();existe=r.getBoolean(1); }
            if(!existe) s.execute(Files.readString(Path.of("sql/criar_banco.sql")));
            s.execute(Files.readString(Path.of("sql/migracoes/001_cadastros_ativos.sql")));
            try(var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='aeroporto' AND table_type='BASE TABLE'")) {
                r.next();System.out.println("Tabelas no PostgreSQL de teste: "+r.getInt(1));
            }
        }
    }
}
