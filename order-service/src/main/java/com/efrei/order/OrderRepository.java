package com.efrei.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

/**
 * Stockage en mémoire des commandes (remplacé par une base de données plus tard).
 */
@Repository
public class OrderRepository {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Order> findAll() {
        List<Order> list = new ArrayList<>(orders.values());
        list.sort((a, b) -> Long.compare(a.id(), b.id()));
        return list;
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order save(Long productId, String productName, int quantity, double totalPrice) {
        Order order = new Order(nextId.getAndIncrement(), productId, productName, quantity, totalPrice);
        orders.put(order.id(), order);
        return order;
    }
}
