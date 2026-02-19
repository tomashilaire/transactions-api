package org.example.transactionsapi.domain.port.in;

/**
 * Driving port: returns the total amount for a transaction and all its
 * transitively linked descendants (via parent_id).
 */
public interface GetTransactionSumUseCase {

    double getTransactionSum(Long transactionId);
}
