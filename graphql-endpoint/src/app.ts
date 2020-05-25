import { acf, bc, connectProducer, mtf } from './kafka'
import { concatMap, share } from 'rxjs/operators'
import { AccountCreationFeedback, MoneyTransferFeedback } from './kafka-types'
import { Observable } from 'rxjs'
import { storeTransaction } from './db'
import { startGraphQL } from './graphql'
import { uuidBytesToString } from './util'

let transactionObservable: Observable<Transaction | null>
let accountCreationFeedbackObservable: Observable<AccountCreationFeedback>
let moneyTransferFeedbackObservable: Observable<MoneyTransferFeedback>

const init = async () => {
  await Promise.all([
    connectProducer(),
    transactionObservable = (await bc()).pipe(concatMap(balanceChanged => {
      return storeTransaction(balanceChanged)
    })).pipe(share()),
    accountCreationFeedbackObservable = (await acf()).pipe(share()),
    moneyTransferFeedbackObservable = (await (mtf())).pipe(share())
  ])
}
init().then(() => {
  transactionObservable
    .subscribe(transaction => console.log(`Transaction with id ${transaction.id} stored`))
  accountCreationFeedbackObservable
    .subscribe(feedback => console.log(`Account creation feedback with id ${uuidBytesToString(feedback.id)} received`))
  moneyTransferFeedbackObservable
    .subscribe(feedback => console.log(`Money transfer feedback with id ${uuidBytesToString(feedback.id)} received`))
  startGraphQL(transactionObservable, accountCreationFeedbackObservable, moneyTransferFeedbackObservable)
})
