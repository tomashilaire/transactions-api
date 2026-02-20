package org.example.transactionsapi.domain.service;

import org.example.transactionsapi.domain.model.Transaction;
import org.example.transactionsapi.domain.model.TransactionNotFoundException;
import org.example.transactionsapi.domain.port.in.CreateTransactionUseCase;
import org.example.transactionsapi.domain.port.in.GetTransactionByIdUseCase;
import org.example.transactionsapi.domain.port.in.GetTransactionsByTypeUseCase;
import org.example.transactionsapi.domain.port.in.GetTransactionSumUseCase;
import org.example.transactionsapi.domain.port.in.UpdateTransactionUseCase;
import org.example.transactionsapi.domain.port.out.TransactionRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core domain service implementing all transaction use cases.
 * Free of framework dependencies — wired into Spring via BeanConfiguration.
 */
public class TransactionService
        implements CreateTransactionUseCase, UpdateTransactionUseCase, GetTransactionByIdUseCase,
                   GetTransactionsByTypeUseCase, GetTransactionSumUseCase {

    private final TransactionRepository repository;
    private final AtomicLong idSequence = new AtomicLong(1);

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Long createTransaction(double amount, String type, Long parentId) {
        Long id = idSequence.getAndIncrement();
        repository.save(new Transaction(id, amount, type, parentId));
        return id;
    }

    @Override
    public void updateTransaction(Long id, double amount, String type, Long parentId) {
        if (repository.findById(id).isEmpty()) {
            throw new TransactionNotFoundException(id);
        }
        repository.save(new Transaction(id, amount, type, parentId));
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Override
    public List<Long> getTransactionIdsByType(String type) {
        return repository.findByType(type).stream()
                .map(Transaction::id)
                .toList();
    }

    /**
     * Returns the sum of the requested transaction's amount plus the amounts of
     * all transactions transitively linked to it as children (depth-first traversal).
     *
     * Example: 10(5000) <- 11(10000) <- 12(5000)
     *   sum(10) = 5000 + 10000 + 5000 = 20000
     *   sum(11) = 10000 + 5000        = 15000
     */
    @Override
    public double getTransactionSum(Long transactionId) {
        Transaction root = repository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return sumRecursive(root);
    }

    private double sumRecursive(Transaction transaction) {
        double sum = transaction.amount();
        for (Transaction child : repository.findByParentId(transaction.id())) {
            sum += sumRecursive(child);
        }
        return sum;
    }
}
