package nl.openweb.graphql_endpoint.service;

import fj.data.Either;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.openweb.data.AccountCreationConfirmed;
import nl.openweb.data.AccountCreationFailed;
import nl.openweb.data.ConfirmAccountCreation;
import nl.openweb.graphql_endpoint.kafka.CommandPublisher;
import nl.openweb.graphql_endpoint.mapper.AccountResultMapper;
import nl.openweb.graphql_endpoint.mapper.UuidMapper;
import nl.openweb.graphql_endpoint.model.Account;
import nl.openweb.graphql_endpoint.model.AccountResult;
import nl.openweb.graphql_endpoint.repository.AccountRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.reactivestreams.Publisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class AccountCreationService {

    private final AccountRepository accountRepository;
    private final CommandPublisher publisher;
    private final EmitterProcessor<Tuple2<UUID, AccountResult>> emitterProcessor = EmitterProcessor.create(false);
    private final Flux<Tuple2<UUID, AccountResult>> flux = Flux.from(emitterProcessor).share();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    @KafkaListener(id = "graphql-endpoint-account-creation-consumer", topics = "${kafka.acf-topic}")
    public void onAccountCreation(ConsumerRecord<String, Object> record) {
        log.info("received account creation from partition: {} at offset: {}"
                , record.partition(), record.offset());
        handleRecord(record);
    }

    private void handleRecord(ConsumerRecord<String, Object> record){
        if (record.value() instanceof AccountCreationConfirmed) {
            handleAccountCreationConfirmed((AccountCreationConfirmed) record.value());
        } else if (record.value() instanceof AccountCreationFailed) {
            handleAccountCreationFailed((AccountCreationFailed) record.value());
        } else {
            log.warn(
                    "Unexpected item {} with class {}, expected either AccountCreationConfirmed or " +
                            "AccountCreationFailed",
                    record.value(), record.value().getClass().getName());
        }
    }

    public Publisher<AccountResult> getAccount(String username, String password) {
        String encoded = passwordEncoder.encode(password);
        Either<UUID, String> check = accountRepository
                .findById(username)
                .map(account -> checkPassword(account, password))
                .orElseGet(() -> createAccount(username, encoded));
        return check.either(
                uuid -> handleSuccess(uuid, username),
                this::handleError
        );
    }

    private void handleAccountCreationConfirmed(AccountCreationConfirmed confirmed) {
        AccountResult result = AccountResultMapper.fromAccountCreationConfirmed(confirmed);
        var item = Tuples.of(UuidMapper.fromAvroUuid(confirmed.getId()), result);
        emitterProcessor.onNext(item);
    }

    private void handleAccountCreationFailed(AccountCreationFailed failed) {
        AccountResult result = AccountResultMapper.fromAccountCreationFailed(failed);
        var item = Tuples.of(UuidMapper.fromAvroUuid(failed.getId()), result);
        emitterProcessor.onNext(item);
    }

    private Publisher<AccountResult> handleError(String reason) {
        AccountResult result = new AccountResult();
        result.setReason(reason);
        return Mono.just(result);
    }

    private Publisher<AccountResult> handleSuccess(UUID uuid, String username) {
        ConfirmAccountCreation creation = new ConfirmAccountCreation(UuidMapper.fromJavaUuid(uuid), username);
        publisher.publish(username, creation);
        return Flux.from(flux)
                .filter(x -> x.getT1().equals(uuid))
                .map(Tuple2::getT2);
    }

    private Either<UUID, String> checkPassword(Account account, String password) {
        if (passwordEncoder.matches(password, account.getPassword())) {
            return Either.left(account.getUuid());
        } else {
            log.error("Expected {}, but  was {}", password, account.getPassword());
            return Either.right("incorrect password");
        }
    }

    private Either<UUID, String> createAccount(String username, String password) {
        UUID uuid = UUID.randomUUID();
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(password);
        account.setUuid(uuid);
        accountRepository.save(account);
        return Either.left(uuid);
    }
}
