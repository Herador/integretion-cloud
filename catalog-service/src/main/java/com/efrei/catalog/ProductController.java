package com.efrei.catalog;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class ProductController {

    private final ProductRepository repository;
    private final String version;

    // Spring injecte automatiquement le repository (injection par constructeur).
    // La version vient de la variable d'environnement SERVICE_VERSION (1 par défaut).
    public ProductController(ProductRepository repository,
                             @Value("${SERVICE_VERSION:1}") String version) {
        this.repository = repository;
        this.version = version;
    }

    /**
     * Infos du service. Dans Kubernetes, le nom d'hôte est le nom du Pod :
     * on voit ainsi quel Pod a répondu (utile pour montrer le load balancing).
     */
    @GetMapping("/")
    public Map<String, String> info() {
        return Map.of(
                "service", "catalog-service",
                "version", version,
                "pod", System.getenv().getOrDefault("HOSTNAME", "local"));
    }

    @GetMapping("/products")
    public List<Product> findAll() {
        return repository.findAll();
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product product = repository.save(request.name(), request.price(), request.stock());
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
}
