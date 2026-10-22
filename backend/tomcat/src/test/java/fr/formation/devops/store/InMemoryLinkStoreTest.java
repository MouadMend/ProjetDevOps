package fr.formation.devops.store;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryLinkStoreTest {
    @Test void performsCrud() {
        var db = new InMemoryLinkStore();
        var created = db.create("GitHub", "https://github.com");
        assertEquals(1, db.findAll().size());
        assertTrue(db.update(created.id(), "Apache", "https://httpd.apache.org"));
        assertEquals("Apache", db.findAll().get(0).title());
        assertTrue(db.delete(created.id()));
        assertTrue(db.findAll().isEmpty());
    }
}
