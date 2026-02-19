package org.example.transactionsapi.adapter.in.web;

import org.example.transactionsapi.adapter.out.persistence.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests covering the full request/response cycle:
 * HTTP layer → use case → in-memory repository.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryTransactionRepository repository;

    @BeforeEach
    void clearStorage() {
        repository.clear();
    }

    // --- POST /transactions ---

    @Test
    void postTransaction_shouldReturn201WithGeneratedId() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.0, "type": "payment"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void postTransaction_shouldGenerateDistinctIdsForEachCall() throws Exception {
        MvcResult first = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.0, "type": "food"}
                                """))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 200.0, "type": "food"}
                                """))
                .andReturn();

        String firstId  = first.getResponse().getContentAsString().replaceAll("[^0-9]", "");
        String secondId = second.getResponse().getContentAsString().replaceAll("[^0-9]", "");
        assertThat(firstId).isNotEqualTo(secondId);
    }

    @Test
    void postTransaction_shouldAcceptOptionalParentId() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 200.0, "type": "shopping", "parent_id": 999}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void postTransaction_shouldBeQueryableWithReturnedId() throws Exception {
        MvcResult result = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 750.0, "type": "rent"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the auto-generated id from the response
        String body = result.getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll("[^0-9]", ""));

        mockMvc.perform(get("/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(750.0))
                .andExpect(jsonPath("$.type").value("rent"));
    }

    // --- PUT /transactions/{id} ---

    @Test
    void putTransaction_shouldReturnStatusOk() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.0, "type": "payment"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void putTransaction_shouldAcceptOptionalParentId() throws Exception {
        mockMvc.perform(put("/transactions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 200.0, "type": "shopping", "parent_id": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // --- GET /transactions/{id} ---

    @Test
    void getById_shouldReturnTransactionDetails() throws Exception {
        putTransaction(30L, 1500.0, "rent", null);

        mockMvc.perform(get("/transactions/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.amount").value(1500.0))
                .andExpect(jsonPath("$.type").value("rent"))
                .andExpect(jsonPath("$.parent_id").doesNotExist());
    }

    @Test
    void getById_shouldIncludeParentIdWhenPresent() throws Exception {
        putTransaction(31L, 500.0, "food", 30L);

        mockMvc.perform(get("/transactions/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parent_id").value(30));
    }

    @Test
    void getById_shouldReturn404ForUnknownId() throws Exception {
        mockMvc.perform(get("/transactions/998"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- GET /transactions/types/{type} ---

    @Test
    void getByType_shouldReturnMatchingIds() throws Exception {
        putTransaction(10L, 500.0, "cars", null);
        putTransaction(11L, 300.0, "cars", null);
        putTransaction(12L, 100.0, "food", null);

        mockMvc.perform(get("/transactions/types/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@ == 10)]").exists())
                .andExpect(jsonPath("$[?(@ == 11)]").exists());
    }

    @Test
    void getByType_shouldReturnEmptyArrayForUnknownType() throws Exception {
        mockMvc.perform(get("/transactions/types/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- GET /transactions/sum/{id} ---

    @Test
    void getSum_shouldReturnAmountForLeafTransaction() throws Exception {
        putTransaction(20L, 5000.0, "cars", null);

        mockMvc.perform(get("/transactions/sum/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(5000.0));
    }

    @Test
    void getSum_shouldAggregateTransitivelyFromRoot() throws Exception {
        // 10 (5000) <- 11 (10000) <- 12 (5000)  =>  sum(10) = 20000
        putTransaction(10L, 5000.0, "cars", null);
        putTransaction(11L, 10000.0, "shopping", 10L);
        putTransaction(12L, 5000.0, "shopping", 11L);

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(20000.0));
    }

    @Test
    void getSum_shouldAggregateTransitivelyFromChild() throws Exception {
        // sum(11) = 10000 + 5000 = 15000 (does NOT include ancestor 10)
        putTransaction(10L, 5000.0, "cars", null);
        putTransaction(11L, 10000.0, "shopping", 10L);
        putTransaction(12L, 5000.0, "shopping", 11L);

        mockMvc.perform(get("/transactions/sum/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(15000.0));
    }

    @Test
    void getSum_shouldReturn404ForUnknownTransaction() throws Exception {
        mockMvc.perform(get("/transactions/sum/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- helper ---

    private void putTransaction(long id, double amount, String type, Long parentId) throws Exception {
        String body = parentId == null
                ? """
                  {"amount": %s, "type": "%s"}
                  """.formatted(amount, type)
                : """
                  {"amount": %s, "type": "%s", "parent_id": %d}
                  """.formatted(amount, type, parentId);

        mockMvc.perform(put("/transactions/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
