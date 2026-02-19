package org.example.transactionsapi.adapter.out.persistence;

import org.example.transactionsapi.domain.model.Transaction;
import org.example.transactionsapi.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link TransactionRepository}.
 *
 * Swap this class with any other implementation (MongoDB, Redis, etc.) without
 * touching any domain or port code — the only requirement is implementing the
 * {@link TransactionRepository} interface.
 */
@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        storage.put(transaction.id(), transaction);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Transaction> findByType(String type) {
        return storage.values().stream()
                .filter(t -> type.equals(t.type()))
                .toList();
    }

    @Override
    public List<Transaction> findByParentId(Long parentId) {
        return storage.values().stream()
                .filter(t -> parentId.equals(t.parentId()))
                .toList();
    }

    /** Resets all stored transactions — used by integration tests to isolate test cases. */
    public void clear() {
        storage.clear();
    }
}
