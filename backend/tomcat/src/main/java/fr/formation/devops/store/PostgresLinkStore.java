package fr.formation.devops.store;

import fr.formation.devops.model.Link;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Mode DEV : base PostgreSQL, paramètres fournis par l'environnement. */
public final class PostgresLinkStore implements LinkStore {
    private final String url;
    private final String user;
    private final String password;

    public PostgresLinkStore(String url, String user, String password) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Pilote PostgreSQL introuvable", e);
        }
        try (Connection c = connect(); Statement statement = c.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS links (
                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    title VARCHAR(120) NOT NULL,
                    url VARCHAR(2048) NOT NULL
                )
                """);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<Link> findAll() throws SQLException {
        List<Link> result = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement stmt = c.prepareStatement("SELECT id, title, url FROM links ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) result.add(new Link(rs.getLong("id"), rs.getString("title"), rs.getString("url")));
        }
        return result;
    }

    @Override
    public Link create(String title, String url) throws SQLException {
        try (Connection c = connect();
             PreparedStatement stmt = c.prepareStatement("INSERT INTO links (title, url) VALUES (?, ?) RETURNING id")) {
            stmt.setString(1, title);
            stmt.setString(2, url);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new SQLException("Pas de nouvel identifiant");
                return new Link(rs.getLong(1), title, url);
            }
        }
    }

    @Override
    public boolean update(long id, String title, String url) throws SQLException {
        try (Connection c = connect();
             PreparedStatement stmt = c.prepareStatement("UPDATE links SET title = ?, url = ? WHERE id = ?")) {
            stmt.setString(1, title);
            stmt.setString(2, url);
            stmt.setLong(3, id);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        try (Connection c = connect();
             PreparedStatement stmt = c.prepareStatement("DELETE FROM links WHERE id = ?")) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
}
