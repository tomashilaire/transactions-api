package org.example.transactionsapi.domain.model;

/**
 * Domain exception thrown when a transaction with the given id does not exist.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long id) {
        super("Transaction not found with id: " + id);
    }
}
