package nl.openweb.graphql_endpoint.resolver

import graphql.GraphQL
import graphql.execution.SubscriptionExecutionStrategy
import graphql.schema.idl.*
import graphql.schema.idl.TypeRuntimeWiring.newTypeWiring
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.core.io.ResourceResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Singleton

@FlowPreview
@ExperimentalCoroutinesApi
@Factory
class GraphQLFactory {
    @Bean
    @Singleton
    fun graphQL(
        resourceResolver: ResourceResolver,
        allLastTransactions: AllLastTransactions,
        transactionById: TransactionById,
        transactionsByIban: TransactionsByIban,
        getAccount: GetAccount,
        moneyTransfer: MoneyTransfer,
        streamTransactions: StreamTransactions
    ): GraphQL { // <2>

        val schemaParser = SchemaParser()
        val schemaGenerator = SchemaGenerator()

        // Parse the schema.
        val typeRegistry = schemaParser.parse(BufferedReader(InputStreamReader(
                resourceResolver.getResourceAsStream("classpath:bank.graphql").get())))

        // Create the runtime wiring.
        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("QueryRoot")
                        .dataFetcher("all_last_transactions", allLastTransactions)
                        .dataFetcher("transactions_by_id", transactionById)
                        .dataFetcher("transactions_by_iban", transactionsByIban))
                .type(newTypeWiring("SubscriptionRoot")
                        .dataFetcher("get_account", getAccount)
                        .dataFetcher("money_transfer", moneyTransfer)
                        .dataFetcher("stream_transactions", streamTransactions))
                .build()

        // Create the executable schema.
        val graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring)

        // Return the GraphQL bean.
        return GraphQL
                .newGraphQL(graphQLSchema)
                .build()
    }
}