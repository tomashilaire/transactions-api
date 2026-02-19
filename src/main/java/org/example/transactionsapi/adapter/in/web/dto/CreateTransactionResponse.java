package org.example.transactionsapi.adapter.in.web.dto;

/**
 * Outbound DTO for POST /transactions responses.
 * Returns the auto-generated transaction id so the caller can reference the new resource.
 */
public record CreateTransactionResponse(Long id) {}
