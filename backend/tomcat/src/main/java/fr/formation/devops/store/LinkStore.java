package fr.formation.devops.store;

import fr.formation.devops.model.Link;
import java.sql.SQLException;
import java.util.List;

public interface LinkStore {
    List<Link> findAll() throws SQLException;
    Link create(String title, String url) throws SQLException;
    boolean update(long id, String title, String url) throws SQLException;
    boolean delete(long id) throws SQLException;
}
