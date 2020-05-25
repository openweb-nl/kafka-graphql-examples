import { BalanceChanged } from './kafka-types'
import { v4 as uuidv4 } from 'uuid'

const { Pool } = require('pg')
const bcrypt = require('bcrypt')

const pool = new Pool({
  host: 'localhost',
  port: 25431,
  database: 'transactiondb',
  user: 'clojure_ge',
  password: 'kafka-graphql-pw',
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000
})

export async function removeAccount (uuid: string): Promise<boolean> {
  try {
    const result = await pool.query('DELETE FROM account WHERE uuid = $1', [uuid])
    return result.rowCount === 1
  } catch (e) {
    setImmediate(() => {
      throw e
    })
  }
}

export async function checkPassword (username: string, password: string): Promise<null | string> {
  try {
    const result = await pool.query('SELECT * FROM account WHERE username = $1', [username])
    if (await result.rowCount === 1) {
      const isValid = await bcrypt.compare(password, result.rows[0].password)
      return isValid === true ? result.rows[0].uuid : null
    } else {
      const encrypted = await bcrypt.hash(password, 11)
      const uuid = await uuidv4()
      const insertResult = await pool.query({
        text: 'INSERT INTO account(username, password, uuid) VALUES($1, $2, $3)',
        values: [username, encrypted, uuid]
      })
      return insertResult.rowCount === 1 ? uuid : null
    }
  } catch (e) {
    setImmediate(() => {
      throw e
    })
  }
}

export async function findTransactionById (id: number): Promise<null | Transaction> {
  try {
    const result = await pool.query('SELECT * FROM transaction WHERE id = $1', [id])
    return result.rowCount === 1 ? result.rows[0] : null
  } catch (e) {
    setImmediate(() => {
      throw e
    })
  }
}

export async function findTransactionsByIban (iban: string, maxItems: number): Promise<null | Array<Transaction>> {
  try {
    const result = await pool.query('SELECT * FROM transaction WHERE iban = $1 ORDER BY id DESC LIMIT $2', [iban, maxItems])
    return result.rowCount !== 0 ? result.rows : null
  } catch (e) {
    setImmediate(() => {
      throw e
    })
  }
}

export async function findAllLastTransactions (): Promise<null | Array<Transaction>> {
  try {
    const result = await pool.query('SELECT * FROM transaction WHERE id IN (SELECT MAX(id) FROM transaction GROUP BY iban) ORDER BY iban')
    return result.rowCount !== 0 ? result.rows : null
  } catch (e) {
    setImmediate(() => {
      throw e
    })
  }
}

export async function storeTransaction (balanceChanged: BalanceChanged): Promise<Transaction> {
  const insertResult = await pool.query({
    text: 'INSERT INTO transaction(iban, new_balance, changed_by, from_to, direction, descr) VALUES($1, $2, $3, $4, $5, $6) RETURNING *',
    values: [
      balanceChanged.iban,
      new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(balanceChanged.new_balance / 100),
      new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Math.abs(balanceChanged.changed_by / 100)),
      balanceChanged.from_to,
      balanceChanged.changed_by < 0 ? 'DEBIT' : 'CREDIT',
      balanceChanged.description
    ]
  })
  if (insertResult.rowCount !== 1) {
    throw new Error('new transaction was not properly stored')
  }
  return insertResult.rows[0]
}
