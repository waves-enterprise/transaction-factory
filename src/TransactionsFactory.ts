import {
  ByteProcessor,
  TxType,
  TxVersion,
  config,
  utils
} from '@vostokplatform/signature-generator'


const {concatUint8Arrays, cryptoGost, crypto} = utils

export interface Processor {
  getBytes() : Promise<Uint8Array>
  isValid() : boolean
  getErrors() : string[]
  getSignature (privateKey: string): Promise<string>
}
type TransactionFields = {tx_type: TxType<any>, version: TxVersion<any>} & Record<string, ByteProcessor<any>>
type getTxType <T> = { [key in keyof T]?: T[key] extends ByteProcessor<infer P> ? P : never }
type TransactionType<T> = getTxType<T> & Processor

class Transaction<T extends TransactionFields> {
  public version: number
  public tx_type: number
  public val: T

  constructor(val: T, tx?: Partial<getTxType<T>>) {
    this.val = val
    this.version = val.version.version
    this.tx_type =  val.tx_type.type
    if (tx) {
      Object.keys(tx).forEach(key => {
        this[key] = tx[key]
      })
    }
  }

  getBytes = async () => {
    const errors = this.getErrors()
    if (errors.length) {
      throw new Error(errors.join('\n'))
    }
    const multipleDataBytes = await Promise.all(Object.keys(this.val).map(async key => {
      let value: Uint8Array
      try {
        if (!this.val[key].required) {
          if (!this[key]) {
            value = Uint8Array.from([0])
          } else {
            const bytes = await this.val[key].getBytes(this[key])
            value = concatUint8Arrays(Uint8Array.from([1]), bytes)
          }
        } else {
          value = await this.val[key].getBytes(this[key])
        }
      } catch (err) {
        throw new Error(`${key}: ${err.message || err}`)
      }
      return value
    }))
    if (multipleDataBytes.length === 1) {
      return multipleDataBytes[0]
    } else {
      return concatUint8Arrays(...multipleDataBytes)
    }
  }

  getSignature = async (privateKey: string) => {
    const dataBytes = await this.getBytes()
    return config.isCryptoGost()
      ? cryptoGost.buildTransactionSignature(dataBytes, privateKey)
      : crypto.buildTransactionSignature(dataBytes, privateKey)
  }

  isValid = () => {
    return !this.getErrors().length
  }

  getErrors = (): string[] | null => {
    const that = this
    return [].concat(...Object.keys(that.val).map(key => {
      const error = that.val[key].getError(that[key])
      return error ? `${key}: ${error}` : null
    }).filter(Boolean))
  }
}

export const getTransactionsFactory = <T extends TransactionFields> (val: T) =>
  (tx?: Partial<getTxType<T>>): TransactionType<T> =>
    (new Transaction(val, tx)) as unknown as TransactionType<T>
