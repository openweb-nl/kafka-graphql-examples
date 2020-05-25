import { v4 as uuidv4 } from 'uuid'
const uuidParse = require('uuid-parse')

export function uuidBytesToString (buffer: Buffer) {
  const v4options = {
    random: buffer
  }
  return uuidv4(v4options)
}

export function uuidStringToBytes (uuid: string) {
  return uuidParse.parse(uuid)
}
