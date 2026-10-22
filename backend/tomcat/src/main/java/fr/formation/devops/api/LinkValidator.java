package fr.formation.devops.api;

import java.net.URI;
import java.net.URISyntaxException;

public final class LinkValidator {
    private LinkValidator() {}

    public static void validate(String title, String url) {
        if (title == null || title.isBlank() || title.strip().length() > 120) {
            throw new IllegalArgumentException("Le titre doit contenir entre 1 et 120 caractères.");
        }
        if (url == null || url.length() > 2048) {
            throw new IllegalArgumentException("L'URL est manquante ou trop longue.");
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("L'URL doit commencer par http:// ou https://.");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL invalide.");
        }
    }
}
