package com.efrei.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Corps de la requête POST /products.
 * Les annotations sont vérifiées automatiquement grâce à @Valid dans le contrôleur.
 */
public record ProductRequest(
        @NotBlank String name,
        @PositiveOrZero double price,
        @PositiveOrZero int stock) {
}
