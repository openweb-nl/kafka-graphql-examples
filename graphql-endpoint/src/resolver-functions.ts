import { Observable, of } from 'rxjs'
import {
  AccountCreationConfirmed,
  AccountCreationFailed,
  AccountCreationFeedback, Identifiable,
  isSuccessAccountCreation, isSuccessMoneyTransfer, MoneyTransferFailed, MoneyTransferFeedback
} from './kafka-types'
import { checkPassword, removeAccount } from './db'
import { uuidStringToBytes } from './util'
import { sendCommand } from './kafka'
import { catchError, filter, first, timeout } from 'rxjs/operators'

function firstMatch<T extends Identifiable> (id: Buffer, observable: Observable<T>) {
  return observable
    .pipe(filter(feedback => feedback.id.equals(id)))
    .pipe(first())
    .pipe(timeout(5000), catchError(() => of('kafka timed out')))
    .toPromise()
}

export async function retrieveAccountFeedback (
  username: string,
  password: string,
  accountCreationFeedbackObservable: Observable<AccountCreationFeedback>
) {
  const uuidString = await checkPassword(username, password)
  if (uuidString === null) {
    return { reason: 'password invalid' }
  }
  const uuidBytes = await uuidStringToBytes(uuidString)
  const uuidBuffer = Buffer.from(uuidBytes)
  await sendCommand(username, {
    id: uuidBuffer,
    username: username
  })
  const kafkaResponse = await firstMatch(uuidBuffer, accountCreationFeedbackObservable)
  if (typeof kafkaResponse === 'string') {
    return { reason: kafkaResponse }
  } else if (isSuccessAccountCreation(kafkaResponse)) {
    const confirmed = kafkaResponse as AccountCreationConfirmed
    return {
      iban: confirmed.iban,
      token: confirmed.token
    }
  } else {
    await removeAccount(uuidString)
    return {
      reason: (kafkaResponse as AccountCreationFailed).reason
    }
  }
}

export async function retrieveMoneyFeedback (
  amount: number,
  descr: string,
  from: string,
  to: string,
  token: string,
  username: string,
  uuid: string,
  moneyTransferFeedbackObservable: Observable<MoneyTransferFeedback>) {
  const buffer = Buffer.from(uuidStringToBytes(uuid))
  await sendCommand(username, {
    id: buffer,
    token: token,
    amount: amount,
    from: from,
    to: to,
    description: descr
  })
  const kafkaResponse = await firstMatch(buffer, moneyTransferFeedbackObservable)
  if (typeof kafkaResponse === 'string') {
    return { reason: kafkaResponse, success: false, uuid: uuid }
  } else if (isSuccessMoneyTransfer(kafkaResponse)) {
    return { success: true, uuid: uuid }
  } else {
    return {
      reason: (kafkaResponse as MoneyTransferFailed).reason,
      success: false,
      uuid: uuid
    }
  }
}

function delay (ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export function streamTransactions (
  direction: string,
  iban: string,
  minAmount: number,
  maxAmount: number,
  descrIncludes: string,
  transactionObservable: Observable<Transaction>) {
  const asyncCounterIterator = () => {
    let iter = 1
    let exhausted = false

    return {
      async next () {
        if (iter > 10 || exhausted) {
          return { done: true }
        }
        await delay(5000)
        const iteratorResult = {
          value: {
            iban: 'iban' + iter,
            new_balance: '$10,10',
            changed_by: 'someone else' + iter,
            from_to: '$11,10',
            direction: 'DEBIT',
            descr: 'some description ' + iter,
            id: iter
          },
          done: false
        }
        iter++
        return iteratorResult
      },
      async throw (e) {
        console.log('oops something is wrong')
        throw e
      },
      async return () {
        exhausted = true
        console.log('I have been released !!!')
        return { done: true }
      }
    }
  }
  return () => ({
    [Symbol.asyncIterator] () {
      return asyncCounterIterator()
    }
  })
}
