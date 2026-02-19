package org.example.transactionsapi.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inbound DTO for PUT /transactions/{id} requests.
 */
public record TransactionRequest(
        double amount,
        String type,
        @JsonProperty("parent_id") Long parentId
) {}
