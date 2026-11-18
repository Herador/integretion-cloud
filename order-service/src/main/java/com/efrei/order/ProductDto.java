package com.efrei.order;

/**
 * Produit tel que renvoyé par l'API REST de catalog-service.
 * Les deux services ne partagent pas de code : chacun a sa propre classe,
 * seul le format JSON les relie.
 */
public record ProductDto(Long id, String name, double price, int stock) {
}
