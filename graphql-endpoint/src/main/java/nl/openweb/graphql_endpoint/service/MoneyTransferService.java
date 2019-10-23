package nl.openweb.graphql_endpoint.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.openweb.data.ConfirmMoneyTransfer;
import nl.openweb.data.MoneyTransferConfirmed;
import nl.openweb.data.MoneyTransferFailed;
import nl.openweb.graphql_endpoint.kafka.CommandPublisher;
import nl.openweb.graphql_endpoint.mapper.MoneyTransferResultMapper;
import nl.openweb.graphql_endpoint.mapper.UuidMapper;
import nl.openweb.graphql_endpoint.model.MoneyTransferResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.reactivestreams.Publisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
@AllArgsConstructor
public class MoneyTransferService {

    private final CommandPublisher publisher;
    private final EmitterProcessor<Tuple2<String, MoneyTransferResult>> emitterProcessor = EmitterProcessor.create(false);
    private final Flux<Tuple2<String, MoneyTransferResult>> flux = Flux.from(emitterProcessor).share();

    @KafkaListener(id = "graphql-endpoint-money-transfer-consumer", topics = "${kafka.mtf-topic}")
    public void onMoneyTransfer(ConsumerRecord<String, Object> record) {
        log.info("received money transfer feedback from partition: {} at offset: {}",
                 record.partition(), record.offset());
        handleRecord(record);
    }


    private void handleRecord(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof MoneyTransferConfirmed) {
            handleMoneyTransferConfirmed((MoneyTransferConfirmed) record.value());
        } else if (record.value() instanceof MoneyTransferFailed) {
            handleMoneyTransferFailed((MoneyTransferFailed) record.value());
        } else {
            log.warn(
                    "Unexpected item {} with class {}, expected either AccountCreationConfirmed or " +
                            "AccountCreationFailed",
                    record.value(), record.value().getClass().getName());
        }
    }

    private void handleMoneyTransferConfirmed(MoneyTransferConfirmed confirmed) {
        MoneyTransferResult result = MoneyTransferResultMapper.fromMoneyTransferConfirmed(confirmed);
        var item = Tuples.of(UuidMapper.fromAvroUuid(confirmed.getId()).toString(), result);
        emitterProcessor.onNext(item);
    }

    private void handleMoneyTransferFailed(MoneyTransferFailed failed) {
        MoneyTransferResult result = MoneyTransferResultMapper.fromMoneyTransferFailed(failed);
        var item = Tuples.of(UuidMapper.fromAvroUuid(failed.getId()).toString(), result);
        emitterProcessor.onNext(item);
    }

    public Publisher<MoneyTransferResult> transfer(long amount, String descr, String from, String to, String
            token,
            String username, String uuid) {
        ConfirmMoneyTransfer transfer = new ConfirmMoneyTransfer();
        transfer.setId(UuidMapper.fromString(uuid));
        transfer.setAmount(amount);
        transfer.setDescription(descr);
        transfer.setFrom(from);
        transfer.setTo(to);
        transfer.setToken(token);
        publisher.publish(username, transfer);
        return Flux.from(flux)
                .filter(x -> x.getT1().equals(uuid))
                .map(Tuple2::getT2);
    }
}
