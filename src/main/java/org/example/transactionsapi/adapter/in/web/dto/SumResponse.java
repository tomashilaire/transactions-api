package org.example.transactionsapi.adapter.in.web.dto;

/**
 * Outbound DTO for GET /transactions/sum/{id} responses.
 */
public record SumResponse(double sum) {}
