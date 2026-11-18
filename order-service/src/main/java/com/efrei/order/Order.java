package com.efrei.order;

/**
 * Une commande enregistrée.
 * Le nom et le prix sont copiés depuis catalog-service au moment de la commande.
 */
public record Order(Long id, Long productId, String productName, int quantity, double totalPrice) {
}
