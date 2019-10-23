package nl.openweb.graphql_endpoint.repository;

import nl.openweb.graphql_endpoint.model.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, String> {
}
