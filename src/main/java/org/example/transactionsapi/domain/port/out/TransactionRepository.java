package org.example.transactionsapi.domain.port.out;

import org.example.transactionsapi.domain.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Driven port: storage abstraction for transactions.
 * Any persistence mechanism (in-memory, MongoDB, etc.) must implement this interface.
 */
public interface TransactionRepository {

    void save(Transaction transaction);

    Optional<Transaction> findById(Long id);

    List<Transaction> findByType(String type);

    List<Transaction> findByParentId(Long parentId);
}
