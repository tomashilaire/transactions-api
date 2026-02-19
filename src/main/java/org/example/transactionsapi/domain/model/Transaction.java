package org.example.transactionsapi.domain.model;

/**
 * Immutable domain entity representing a financial transaction.
 * A transaction can optionally reference a parent transaction,
 * forming a tree structure used for transitive sum calculations.
 */
public record Transaction(
        Long id,
        double amount,
        String type,
        Long parentId
) {}
