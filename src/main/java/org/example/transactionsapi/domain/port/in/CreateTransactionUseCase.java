package org.example.transactionsapi.domain.port.in;

/**
 * Driving port: creates a new transaction with an auto-generated id.
 * Returns the generated id so the caller can reference the transaction.
 */
public interface CreateTransactionUseCase {

    Long createTransaction(double amount, String type, Long parentId);
}
