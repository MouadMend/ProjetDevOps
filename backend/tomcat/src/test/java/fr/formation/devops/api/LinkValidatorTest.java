package fr.formation.devops.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkValidatorTest {
    @Test void acceptsHttpAndHttps() {
        assertDoesNotThrow(() -> LinkValidator.validate("Documentation", "https://example.org/docs"));
    }
    @Test void rejectsJavaScriptAndEmptyTitle() {
        assertThrows(IllegalArgumentException.class, () -> LinkValidator.validate("", "https://example.org"));
        assertThrows(IllegalArgumentException.class, () -> LinkValidator.validate("Test", "javascript:alert(1)"));
    }
}
