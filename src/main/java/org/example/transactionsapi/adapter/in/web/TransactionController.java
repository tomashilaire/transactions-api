package org.example.transactionsapi.adapter.in.web;

import org.example.transactionsapi.adapter.in.web.dto.CreateTransactionRequest;
import org.example.transactionsapi.adapter.in.web.dto.CreateTransactionResponse;
import org.example.transactionsapi.adapter.in.web.dto.StatusResponse;
import org.example.transactionsapi.adapter.in.web.dto.SumResponse;
import org.example.transactionsapi.adapter.in.web.dto.TransactionRequest;
import org.example.transactionsapi.adapter.in.web.dto.TransactionResponse;
import org.example.transactionsapi.domain.model.Transaction;
import org.example.transactionsapi.domain.port.in.CreateTransactionUseCase;
import org.example.transactionsapi.domain.port.in.GetTransactionByIdUseCase;
import org.example.transactionsapi.domain.port.in.GetTransactionsByTypeUseCase;
import org.example.transactionsapi.domain.port.in.GetTransactionSumUseCase;
import org.example.transactionsapi.domain.port.in.UpsertTransactionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST adapter (driving side) — translates HTTP requests into use case calls.
 * Depends only on port interfaces, never on the concrete service.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransaction;
    private final UpsertTransactionUseCase upsertTransaction;
    private final GetTransactionByIdUseCase getById;
    private final GetTransactionsByTypeUseCase getByType;
    private final GetTransactionSumUseCase getSum;

    public TransactionController(
            CreateTransactionUseCase createTransaction,
            UpsertTransactionUseCase upsertTransaction,
            GetTransactionByIdUseCase getById,
            GetTransactionsByTypeUseCase getByType,
            GetTransactionSumUseCase getSum) {
        this.createTransaction = createTransaction;
        this.upsertTransaction = upsertTransaction;
        this.getById = getById;
        this.getByType = getByType;
        this.getSum = getSum;
    }

    /**
     * POST /transactions
     * Creates a new transaction with an auto-generated id.
     * Returns 201 Created with the generated id in the response body.
     */
    @PostMapping
    public ResponseEntity<CreateTransactionResponse> create(@RequestBody CreateTransactionRequest request) {
        Long id = createTransaction.createTransaction(request.amount(), request.type(), request.parentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateTransactionResponse(id));
    }

    /**
     * PUT /transactions/{transactionId}
     * Idempotent upsert: creates or fully replaces the transaction with the given id.
     */
    @PutMapping("/{transactionId}")
    public ResponseEntity<StatusResponse> upsert(
            @PathVariable Long transactionId,
            @RequestBody TransactionRequest request) {
        upsertTransaction.upsertTransaction(transactionId, request.amount(), request.type(), request.parentId());
        return ResponseEntity.ok(new StatusResponse("ok"));
    }

    /**
     * GET /transactions/{transactionId}
     * Returns the full details of a single transaction.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Long transactionId) {
        Transaction tx = getById.getTransactionById(transactionId);
        return ResponseEntity.ok(new TransactionResponse(tx.id(), tx.amount(), tx.type(), tx.parentId()));
    }

    /**
     * GET /transactions/types/{type}
     * Returns a list of all transaction ids for the given type.
     */
    @GetMapping("/types/{type}")
    public ResponseEntity<List<Long>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(getByType.getTransactionIdsByType(type));
    }

    /**
     * GET /transactions/sum/{transactionId}
     * Returns the transitive sum of amounts for the given transaction and all its descendants.
     */
    @GetMapping("/sum/{transactionId}")
    public ResponseEntity<SumResponse> getSum(@PathVariable Long transactionId) {
        return ResponseEntity.ok(new SumResponse(getSum.getTransactionSum(transactionId)));
    }
}
