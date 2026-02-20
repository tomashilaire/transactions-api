package org.example.transactionsapi.domain.port.in;

/**
 * Driving port: updates an existing transaction identified by the given id.
 * Throws TransactionNotFoundException if no transaction with that id exists.
 */
public interface UpdateTransactionUseCase {

    void updateTransaction(Long id, double amount, String type, Long parentId);
}
