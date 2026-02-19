package org.example.transactionsapi.domain.port.in;

import java.util.List;

/**
 * Driving port: retrieves the ids of all transactions matching the given type.
 */
public interface GetTransactionsByTypeUseCase {

    List<Long> getTransactionIdsByType(String type);
}
