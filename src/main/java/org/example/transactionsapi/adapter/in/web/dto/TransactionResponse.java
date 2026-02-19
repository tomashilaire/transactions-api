package org.example.transactionsapi.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound DTO for GET /transactions/{id} responses.
 * parent_id is omitted from the JSON when null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        Long id,
        double amount,
        String type,
        @JsonProperty("parent_id") Long parentId
) {}
