package org.example.transactionsapi;

import org.example.transactionsapi.domain.port.out.TransactionRepository;
import org.example.transactionsapi.domain.service.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring: bridges the domain service (framework-free) with the
 * Spring application context, injecting the repository port implementation.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public TransactionService transactionService(TransactionRepository repository) {
        return new TransactionService(repository);
    }
}
