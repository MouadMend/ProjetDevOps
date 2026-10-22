package fr.formation.devops.store;

import fr.formation.devops.model.Link;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Mode démo local : les données disparaissent au redémarrage de Tomcat. */
public final class InMemoryLinkStore implements LinkStore {
    private final Map<Long, Link> items = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public List<Link> findAll() {
        return items.values().stream().sorted(Comparator.comparingLong(Link::id)).toList();
    }

    @Override
    public Link create(String title, String url) {
        long id = sequence.incrementAndGet();
        Link link = new Link(id, title, url);
        items.put(id, link);
        return link;
    }

    @Override
    public boolean update(long id, String title, String url) {
        return items.computeIfPresent(id, (key, previous) -> new Link(id, title, url)) != null;
    }

    @Override
    public boolean delete(long id) {
        return items.remove(id) != null;
    }
}
