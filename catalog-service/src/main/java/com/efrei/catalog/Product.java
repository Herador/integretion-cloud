package com.efrei.catalog;

/**
 * Un produit du catalogue.
 * Un "record" Java génère automatiquement le constructeur, les accesseurs,
 * equals, hashCode et toString.
 */
public record Product(Long id, String name, double price, int stock) {
}
