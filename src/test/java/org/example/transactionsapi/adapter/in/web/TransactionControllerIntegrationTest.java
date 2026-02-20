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
    void putTransaction_shouldUpdateExistingTransaction() throws Exception {
        MvcResult created = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.0, "type": "payment"}
                                """))
                .andReturn();
        long id = Long.parseLong(created.getResponse().getContentAsString().replaceAll("[^0-9]", ""));

        mockMvc.perform(put("/transactions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 250.0, "type": "updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/transactions/" + id))
                .andExpect(jsonPath("$.amount").value(250.0))
                .andExpect(jsonPath("$.type").value("updated"));
    }

    @Test
    void putTransaction_shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(put("/transactions/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.0, "type": "payment"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- GET /transactions/{id} ---

    @Test
    void getById_shouldReturnTransactionDetails() throws Exception {
        long id = postTransaction(1500.0, "rent", null);

        mockMvc.perform(get("/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(1500.0))
                .andExpect(jsonPath("$.type").value("rent"))
                .andExpect(jsonPath("$.parent_id").doesNotExist());
    }

    @Test
    void getById_shouldIncludeParentIdWhenPresent() throws Exception {
        long parentId = postTransaction(1500.0, "rent", null);
        long childId  = postTransaction(500.0, "food", parentId);

        mockMvc.perform(get("/transactions/" + childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parent_id").value(parentId));
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
        long id1 = postTransaction(500.0, "cars", null);
        long id2 = postTransaction(300.0, "cars", null);
        postTransaction(100.0, "food", null);

        mockMvc.perform(get("/transactions/types/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@ == " + id1 + ")]").exists())
                .andExpect(jsonPath("$[?(@ == " + id2 + ")]").exists());
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
        long id = postTransaction(5000.0, "cars", null);

        mockMvc.perform(get("/transactions/sum/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(5000.0));
    }

    @Test
    void getSum_shouldAggregateTransitivelyFromRoot() throws Exception {
        long id1 = postTransaction(5000.0,  "cars",     null);
        long id2 = postTransaction(10000.0, "shopping", id1);
        postTransaction(5000.0,  "shopping", id2);

        mockMvc.perform(get("/transactions/sum/" + id1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(20000.0));
    }

    @Test
    void getSum_shouldAggregateTransitivelyFromChild() throws Exception {
        long id1 = postTransaction(5000.0,  "cars",     null);
        long id2 = postTransaction(10000.0, "shopping", id1);
        postTransaction(5000.0,  "shopping", id2);

        mockMvc.perform(get("/transactions/sum/" + id2))
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

    private long postTransaction(double amount, String type, Long parentId) throws Exception {
        String body = parentId == null
                ? """
                  {"amount": %s, "type": "%s"}
                  """.formatted(amount, type)
                : """
                  {"amount": %s, "type": "%s", "parent_id": %d}
                  """.formatted(amount, type, parentId);

        MvcResult result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn();
        return Long.parseLong(result.getResponse().getContentAsString().replaceAll("[^0-9]", ""));
    }
}
