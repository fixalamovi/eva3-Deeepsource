package com.deepsource.records.support;
import java.nio.file.*;
import javax.sql.DataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class DatabaseConfig {
    @Bean
    DataSource dataSource(@Value("${app.database}") String location) throws Exception {
        Path path = Path.of(location).toAbsolutePath();
        Files.createDirectories(path.getParent());
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(10000);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        SQLiteDataSource source = new SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + path);
        return source;
    }
}
