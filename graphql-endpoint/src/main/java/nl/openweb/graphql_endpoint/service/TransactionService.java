package nl.openweb.graphql_endpoint.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.openweb.data.BalanceChanged;
import nl.openweb.graphql_endpoint.mapper.TransactionMapper;
import nl.openweb.graphql_endpoint.model.DType;
import nl.openweb.graphql_endpoint.model.Transaction;
import nl.openweb.graphql_endpoint.repository.TransactionRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Component
@AllArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final EmitterProcessor<Transaction> emitterProcessor = EmitterProcessor.create(false);
    private final Flux<Transaction> flux = Flux.from(emitterProcessor).share();

    @KafkaListener(id = "graphql-endpoint-balance-changed-consumer", topics = "${kafka.bc-topic}")
    public void onBalanceChanged(ConsumerRecord<String, Object> record) {
        log.info("received balance changed from partition: {} at offset: {}"
                , record.partition(), record.offset());
        handleRecord(record);
    }

    private void handleRecord(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof BalanceChanged) {
            handleBalanceChanged((BalanceChanged) record.value());
        } else {
            log.warn("Unexpected item {} with class {}, expected BalanceChanged",
                     record.value(), record.value().getClass().getName());
        }
    }

    public List<Transaction> allLastTransactions() {
        return transactionRepository.allLastTransactions();
    }

    public Transaction transactionById(int id) {
        return transactionRepository.findById(id).orElse(null);
    }

    public List<Transaction> transactionsByIban(String iban, int maxItems) {
        return transactionRepository.findAllByIbanOrderByIdDesc(iban, PageRequest.of(0, maxItems));
    }

    private Predicate<Transaction> filterFunction(DType direction, String iban, Long minAmount,
            Long maxAmount, String descrIncluded) {
        List<Predicate<Transaction>> predicates = new ArrayList<>();
        Optional.ofNullable(direction).ifPresent(d -> predicates.add(t -> t.getDirection() == d));
        Optional.ofNullable(iban).ifPresent(i -> predicates.add(t -> t.getIban().equals(i)));
        Optional.ofNullable(minAmount).ifPresent(a -> predicates.add(t -> t.getAmount() >= a));
        Optional.ofNullable(maxAmount).ifPresent(a -> predicates.add(t -> t.getAmount() <= a));
        Optional.ofNullable(descrIncluded)
                .ifPresent(i -> predicates.add(t -> StringUtils.containsIgnoreCase(t.getDescr(), i)));
        return transaction -> predicates
                .stream()
                .map(predicate -> predicate.test(transaction))
                .filter(result -> !result)
                .findFirst()
                .orElse(Boolean.TRUE);
    }

    public Publisher<Transaction> stream(DType direction, String iban, Long minAmount, Long maxAmount,
            String descrIncluded) {
        return Flux.from(flux)
                .filter(filterFunction(direction, iban, minAmount, maxAmount, descrIncluded));
    }

    private void handleBalanceChanged(BalanceChanged changed) {
        Transaction transaction = TransactionMapper.fromBalanceChanged(changed);
        transaction = transactionRepository.save(transaction);
        emitterProcessor.onNext(transaction);
    }
}
