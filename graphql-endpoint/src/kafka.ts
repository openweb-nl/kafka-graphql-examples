import { CompressionCodecs, CompressionTypes, Kafka } from 'kafkajs'
import { SchemaRegistry } from '@kafkajs/confluent-schema-registry/dist'
import LZ4 from 'kafkajs-lz4'
import {
  AccountCreationFeedback,
  BalanceChanged,
  Command,
  getName,
  MoneyTransferFeedback
} from './kafka-types'
import { Observable, Subscriber } from 'rxjs'

CompressionCodecs[CompressionTypes.LZ4] = new LZ4().codec
const config = {
  clientId: 'my-app', brokers: ['localhost:9092', 'localhost:9094', 'localhost:9096']
}
const kafka = new Kafka(config)
const registry = new SchemaRegistry({ host: 'http://localhost:8081/' })
const producer = kafka.producer()
const schemaIdCache = new Map()

async function consumeObservable<T> (groupId: string, topic: string): Promise<Observable<T>> {
  const consumer = kafka.consumer({ groupId: groupId })
  await consumer.connect()
  await consumer.subscribe({ topic: topic, fromBeginning: true })
  return new Observable((subject: Subscriber<T>) => {
    consumer.run({
      autoCommit: true,
      eachMessage: async ({ message }) => {
        const event: T = await registry.decode(message.value)
        subject.next(event)
      }
    })
  })
}

export async function bc (): Promise<Observable<BalanceChanged>> {
  return consumeObservable('graphql-endpoint-bc', 'balance_changed')
}

export async function acf (): Promise<Observable<AccountCreationFeedback>> {
  return consumeObservable('graphql-endpoint-acf', 'account_creation_feedback')
}

export async function mtf (): Promise<Observable<MoneyTransferFeedback>> {
  return consumeObservable('graphql-endpoint-mtf', 'money_transfer_feedback')
}

function getSubjectFromCommand (command: Command) {
  return 'commands-nl.openweb.data.' + getName(command)
}

async function getSchemaId (subject: string) {
  const idFromCache = await schemaIdCache.get(subject)
  if (await idFromCache) {
    return idFromCache
  }
  const idFromRegistry = await registry.getLatestSchemaId(subject)
  schemaIdCache.set(subject, idFromRegistry)
  return idFromRegistry
}

export async function connectProducer () {
  await Promise.all([
    producer.connect(),
    getSchemaId('commands-nl.openweb.data.ConfirmAccountCreation'),
    getSchemaId('commands-nl.openweb.data.ConfirmMoneyTransfer')
  ])
}

export async function sendCommand (user: string, command: Command) {
  console.info(`sending command: ${JSON.stringify(command)}`)
  const schemaId = await getSchemaId(getSubjectFromCommand(command))
  const value = await registry.encode(schemaId, command)
  await producer.send({
    topic: 'commands',
    messages: [{ key: user, value: value }],
    compression: CompressionTypes.LZ4
  })
}
