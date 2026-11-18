package com.efrei.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

/**
 * Stockage en mémoire, suffisant pour la première partie.
 * Il sera remplacé par une vraie base de données à l'étape 16/20.
 * ConcurrentHashMap car plusieurs requêtes HTTP peuvent arriver en même temps.
 */
@Repository
public class ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ProductRepository() {
        save("Clavier", 49.9, 10);
        save("Souris", 19.9, 25);
        save("Ecran", 199.0, 5);
    }

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>(products.values());
        list.sort((a, b) -> Long.compare(a.id(), b.id()));
        return list;
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product save(String name, double price, int stock) {
        Product product = new Product(nextId.getAndIncrement(), name, price, stock);
        products.put(product.id(), product);
        return product;
    }
}
