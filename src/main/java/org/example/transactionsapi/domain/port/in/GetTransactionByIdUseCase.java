package org.example.transactionsapi.domain.port.in;

import org.example.transactionsapi.domain.model.Transaction;

/**
 * Driving port: retrieves a single transaction by its id.
 */
public interface GetTransactionByIdUseCase {

    Transaction getTransactionById(Long id);
}
