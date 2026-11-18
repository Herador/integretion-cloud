package com.efrei.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corps de la requête POST /orders.
 */
public record OrderRequest(
        @NotNull Long productId,
        @Positive int quantity) {
}
