import {
  ByteProcessor,
  TxType,
  TxVersion,
} from '@wavesenterprise/signature-generator'

export interface Processor<T> {
  isValid() : boolean
  getErrors() : string[]
  getBody(): {
    version: number,
    type: number
  } & Omit<T, 'tx_type'>
}
type TransactionFields = {tx_type: TxType<any>, version: TxVersion<any>} & Record<string, ByteProcessor<any>>
type getTxType <T> = { [key in keyof T]?: T[key] extends ByteProcessor<infer P> ? P : never }
export type TransactionType<T> = getTxType<T> & Processor<getTxType<T>>
export type TransactionFactory<T> = (tx?: Partial<getTxType<T>>) => TransactionType<T>

export class TransactionClass<T extends TransactionFields> {
  public version: number
  public tx_type: number
  public senderPublicKey?: string
  public val: T
  public id?: string
  public proofs?: string[]

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

  isValid = () => {
    return !this.getErrors().length
  }

  getErrors = (): string[] | null => {
    const that = this
    return [].concat(...Object.keys(that.val).filter(key => !key.startsWith('duplicate')).map(key => {
      const error = that.val[key].getError(that[key])
      return error ? `${key}: ${error}` : null
    }).filter(Boolean))
  }

  getBody = () => {
    const data = {} as any
    Object.keys(this).forEach(key => {
      if (typeof this[key] !== 'function' && key !== 'val') {
        data[key] = this[key]
      }
    })
    delete data.tx_type
    return {
      ...data,
      version: this.version,
      type: this.tx_type
    }
  }
}

export const createTransactionsFactory = <T extends TransactionFields> (val: T) =>
  (tx?: Partial<getTxType<T>>): TransactionType<T> =>
    (new TransactionClass(val, tx)) as unknown as TransactionType<T>
