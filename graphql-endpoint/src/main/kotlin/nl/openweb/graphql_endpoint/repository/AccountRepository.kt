package nl.openweb.graphql_endpoint.repository

import nl.openweb.graphql_endpoint.model.Account
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : CrudRepository<Account, String>