/* eslint camelcase: ["error", {allow: ["max_items", "all_last_transactions", "transaction_by_id", "transactions_by_iban", "get_account", "money_transfer", "stream_transactions", "min_amount", "max_amount", "descr_includes"]}] */
import { loadSchemaSync } from '@graphql-tools/load'
import { GraphQLFileLoader } from '@graphql-tools/graphql-file-loader'
import { join } from 'path'
import { addResolversToSchema } from '@graphql-tools/schema'
import express from 'express'
import graphqlHTTP from 'express-graphql'
import { findAllLastTransactions, findTransactionById, findTransactionsByIban } from './db'
import { Observable } from 'rxjs'
import { AccountCreationFeedback, MoneyTransferFeedback } from './kafka-types'
import { retrieveAccountFeedback, retrieveMoneyFeedback, streamTransactions } from './resolver-functions'

export function startGraphQL (
  transactionObservable: Observable<Transaction>,
  accountCreationFeedbackObservable: Observable<AccountCreationFeedback>,
  moneyTransferFeedbackObservable: Observable<MoneyTransferFeedback>) {
  const schema = loadSchemaSync(join(__dirname, '../resource/bank.graphql'), {
    loaders: [
      new GraphQLFileLoader()
    ]
  })

  const resolvers = {
    QueryRoot: {
      all_last_transactions () {
        return findAllLastTransactions()
      },
      transaction_by_id (obj, { id }) {
        return findTransactionById(id)
      },
      transactions_by_iban (obj, { iban, max_items }) {
        return findTransactionsByIban(iban, max_items)
      }
    },
    MutationRoot: {
      get_account (obj, { username, password }) {
        return retrieveAccountFeedback(username, password, accountCreationFeedbackObservable)
      },
      money_transfer (obj, { amount, descr, from, to, token, username, uuid }) {
        return retrieveMoneyFeedback(amount, descr, from, to, token, username, uuid, moneyTransferFeedbackObservable)
      }
    },
    SubscriptionRoot: {
      get_account (obj, { username, password }) {
        return retrieveAccountFeedback(username, password, accountCreationFeedbackObservable)
      },
      money_transfer (obj, { amount, descr, from, to, token, username, uuid }) {
        return retrieveMoneyFeedback(amount, descr, from, to, token, username, uuid, moneyTransferFeedbackObservable)
      },
      stream_transactions: {
        subscribe: (obj, { direction, iban, min_amount, max_amount, descr_includes }) => streamTransactions(direction, iban, min_amount, max_amount, descr_includes, transactionObservable),
        resolve: (root) => {
          console.info(`This was the root object: ${root}`)
          console.info(`Type: ${typeof root}`)
          return root
        }
      }
    }
  }

  // Add resolvers to the schema
  const schemaWithResolvers = addResolversToSchema({
    schema,
    resolvers
  })

  const app = express()

  app.use(
    graphqlHTTP({
      schema: schemaWithResolvers,
      graphiql: true
    })
  )

  app.listen(8000, () => {
    console.info('Server listening on http://localhost:8000')
  })
}
