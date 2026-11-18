package com.efrei.order;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import jakarta.validation.Valid;

@RestController
public class OrderController {

    private final OrderRepository repository;
    private final CatalogClient catalogClient;
    private final String version;

    public OrderController(OrderRepository repository,
                           CatalogClient catalogClient,
                           @Value("${SERVICE_VERSION:1}") String version) {
        this.repository = repository;
        this.catalogClient = catalogClient;
        this.version = version;
    }

    @GetMapping("/")
    public Map<String, String> info() {
        return Map.of(
                "service", "order-service",
                "version", version,
                "pod", System.getenv().getOrDefault("HOSTNAME", "local"));
    }

    @GetMapping("/orders")
    public List<Order> findAll() {
        return repository.findAll();
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée une commande après avoir interrogé catalog-service en REST.
     * 201 : commande créée
     * 404 : produit inconnu du catalogue
     * 409 : stock insuffisant
     * 503 : catalog-service injoignable
     */
    @PostMapping("/orders")
    public ResponseEntity<?> create(@Valid @RequestBody OrderRequest request) {
        Optional<ProductDto> product;
        try {
            product = catalogClient.findProduct(request.productId());
        } catch (RestClientException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "catalog-service est injoignable");
        }

        if (product.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Produit inconnu : " + request.productId());
        }
        ProductDto p = product.get();
        if (p.stock() < request.quantity()) {
            return error(HttpStatus.CONFLICT, "Stock insuffisant pour " + p.name());
        }

        Order order = repository.save(p.id(), p.name(), request.quantity(), p.price() * request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
