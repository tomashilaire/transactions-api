package org.example.transactionsapi.domain.port.in;

/**
 * Driving port: idempotent upsert — creates or fully replaces the transaction
 * identified by the caller-supplied id.
 */
public interface UpsertTransactionUseCase {

    void upsertTransaction(Long id, double amount, String type, Long parentId);
}
