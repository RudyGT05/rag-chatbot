package gt.edu.umg.desarrolloweb.chatbot.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Este servicio habla con DOS bases de datos:
 *
 *  1. Postgres + pgvector (PRIMARIA): la BD propia del chatbot. Aquí Spring AI
 *     guarda los fragmentos y sus embeddings. Lectura y escritura.
 *
 *  2. MySQL del monolito (SECUNDARIA, opcional): solo lectura. De aquí salen
 *     los datos públicos (tarifas) que se convierten en texto para el RAG.
 *
 * Al declarar dos DataSource, Spring Boot ya no puede adivinar cuál usar, así
 * que marcamos la de pgvector con @Primary. Spring AI toma el JdbcTemplate
 * primario para el vector store.
 */
@Configuration
public class DataSourceConfig {

    // ---------- 1. Postgres + pgvector (primaria) ----------

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource vectorDataSource(
            @Qualifier("vectorDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ---------- 2. MySQL del monolito (solo lectura, opcional) ----------

    @Bean
    @ConfigurationProperties("monolito.datasource")
    @ConditionalOnProperty(name = "monolito.enabled", havingValue = "true")
    public DataSourceProperties monolitoDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConditionalOnProperty(name = "monolito.enabled", havingValue = "true")
    public DataSource monolitoDataSource(
            @Qualifier("monolitoDataSourceProperties") DataSourceProperties props) {
        HikariDataSource ds = props.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        // Doble candado: además del usuario con solo GRANT SELECT en MySQL,
        // la conexión misma se abre en modo read-only.
        ds.setReadOnly(true);
        ds.setPoolName("monolito-solo-lectura");
        ds.setMaximumPoolSize(2);
        return ds;
    }

    @Bean
    @ConditionalOnProperty(name = "monolito.enabled", havingValue = "true")
    public JdbcTemplate monolitoJdbcTemplate(@Qualifier("monolitoDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
