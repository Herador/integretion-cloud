package com.efrei.order;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Client REST vers catalog-service.
 * L'adresse vient de la propriété catalog.url (variable d'environnement CATALOG_URL).
 * Dans Kubernetes, ce sera http://catalog-service:8080 : le nom du Service
 * est résolu par le DNS interne du cluster.
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder builder, @Value("${catalog.url}") String catalogUrl) {
        this.restClient = builder.baseUrl(catalogUrl).build();
    }

    /**
     * Renvoie le produit, ou un Optional vide si catalog-service répond 404.
     * Si catalog-service est injoignable, une RestClientException est levée.
     */
    public Optional<ProductDto> findProduct(Long productId) {
        try {
            ProductDto product = restClient.get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
