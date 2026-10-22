package fr.formation.devops.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.formation.devops.model.Link;
import fr.formation.devops.store.InMemoryLinkStore;
import fr.formation.devops.store.LinkStore;
import fr.formation.devops.store.PostgresLinkStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(urlPatterns = "/api/links/*", loadOnStartup = 1)
public final class LinksServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private LinkStore store;

    public record LinkInput(String title, String url) {}
    public record ApiError(String error) {}

    @Override
    public void init() throws ServletException {
        String dbUrl = System.getenv("APP_DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            store = new InMemoryLinkStore();
            getServletContext().log("Liens API : mode mémoire, pas de persistance");
        } else {
            try {
                store = new PostgresLinkStore(dbUrl,
                        System.getenv().getOrDefault("APP_DB_USER", "liens"),
                        System.getenv().getOrDefault("APP_DB_PASSWORD", ""));
                getServletContext().log("Liens API : mode PostgreSQL activé");
            } catch (SQLException e) {
                throw new ServletException("Connexion ou initialisation PostgreSQL impossible", e);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getPathInfo() != null && !"/".equals(request.getPathInfo())) {
            sendError(response, 404, "Route inconnue.");
            return;
        }
        try {
            sendJson(response, 200, store.findAll());
        } catch (SQLException e) {
            dbError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getPathInfo() != null && !"/".equals(request.getPathInfo())) {
            sendError(response, 404, "Route inconnue.");
            return;
        }
        LinkInput input = parseInput(request, response);
        if (input == null) return;
        try {
            Link link = store.create(input.title().strip(), input.url().strip());
            sendJson(response, 201, link);
        } catch (SQLException e) {
            dbError(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = parseId(request, response);
        if (id == null) return;
        LinkInput input = parseInput(request, response);
        if (input == null) return;
        try {
            if (!store.update(id, input.title().strip(), input.url().strip())) {
                sendError(response, 404, "Lien introuvable.");
            } else {
                sendJson(response, 200, new Link(id, input.title().strip(), input.url().strip()));
            }
        } catch (SQLException e) {
            dbError(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = parseId(request, response);
        if (id == null) return;
        try {
            if (store.delete(id)) response.setStatus(204);
            else sendError(response, 404, "Lien introuvable.");
        } catch (SQLException e) {
            dbError(response, e);
        }
    }

    private Long parseId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String part = request.getPathInfo();
        if (part == null || !part.matches("/[1-9][0-9]*")) {
            sendError(response, 400, "Identifiant invalide.");
            return null;
        }
        try {
            return Long.parseLong(part.substring(1));
        } catch (NumberFormatException e) {
            sendError(response, 400, "Identifiant invalide.");
            return null;
        }
    }

    private LinkInput parseInput(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LinkInput input;
        try {
            input = JSON.readValue(request.getInputStream(), LinkInput.class);
        } catch (JsonProcessingException e) {
            sendError(response, 400, "Corps JSON invalide.");
            return null;
        }
        if (input == null) {
            sendError(response, 400, "Corps JSON manquant.");
            return null;
        }
        try {
            LinkValidator.validate(input.title(), input.url());
        } catch (IllegalArgumentException e) {
            sendError(response, 400, e.getMessage());
            return null;
        }
        return input;
    }

    private void sendJson(HttpServletResponse response, int code, Object body) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSON.writeValue(response.getOutputStream(), body);
    }

    private void sendError(HttpServletResponse response, int code, String message) throws IOException {
        sendJson(response, code, new ApiError(message));
    }

    private void dbError(HttpServletResponse response, SQLException e) throws IOException {
        getServletContext().log("Erreur base de données", e);
        sendError(response, 500, "Erreur serveur. Consulte les logs Tomcat.");
    }
}
