/* eslint camelcase: ["error", {allow: ["new_balance", "changed_by", "from_to"]}] */
export type BalanceChanged = {
    iban: string,
    new_balance: number,
    changed_by: number,
    from_to: string,
    description: string
}

export interface Identifiable {
    id: Buffer
}

export type AccountCreationConfirmed = {
    id: Buffer
    iban: string,
    token: string
}

export type AccountCreationFailed = {
    id: Buffer
    reason: string,
}

export type AccountCreationFeedback = AccountCreationConfirmed | AccountCreationFailed

export function isSuccessAccountCreation (item: AccountCreationFeedback) {
  return (item as AccountCreationConfirmed).iban !== undefined
}

export type MoneyTransferConfirmed = {
    id: Buffer
}

export type MoneyTransferFailed = {
    id: Buffer,
    reason: string
}

export type MoneyTransferFeedback = AccountCreationConfirmed | AccountCreationFailed

export function isSuccessMoneyTransfer (item: MoneyTransferFeedback) {
  return (item as MoneyTransferFailed).reason === undefined
}

export type ConfirmAccountCreation = {
    id: Buffer,
    username: string
}

export type ConfirmMoneyTransfer = {
    id: Buffer,
    token: string,
    amount: number,
    from: string,
    to: string,
    description: string
}

export type Command = ConfirmAccountCreation | ConfirmMoneyTransfer

export function getName (command: Command) {
  if ((command as ConfirmAccountCreation).username !== undefined) {
    return 'ConfirmAccountCreation'
  } else {
    return 'ConfirmMoneyTransfer'
  }
}
