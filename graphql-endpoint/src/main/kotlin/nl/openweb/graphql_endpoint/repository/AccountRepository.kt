package nl.openweb.graphql_endpoint.repository

import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import nl.openweb.graphql_endpoint.model.Account

@Repository
interface AccountRepository : CrudRepository<Account, String>