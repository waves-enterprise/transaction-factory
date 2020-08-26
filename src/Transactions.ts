import {
  Alias,
  AssetId,
  Attachment,
  Base58,
  Base58WithLength,
  Base64,
  Bool,
  Byte,
  ByteProcessor,
  DataEntries,
  DockerCreateParamsEntries,
  Long,
  TxType,
  TxVersion,
  Integer,
  MandatoryAssetId,
  OrderType,
  Recipient,
  StringWithLength,
  ArrayOfStringsWithLength,
  Transfers,
  PermissionTarget,
  PermissionOpType,
  PermissionRole,
  PermissionDueTimestamp,
  BigNumber
} from '@vostokplatform/signature-generator'
import { TRANSACTION_TYPES, TRANSACTION_VERSIONS } from './constants'
import { getTransactionsFactory, Processor } from './TransactionsFactory'


/**
 *
 * TRANSACTIONS
 *
 */

const TRANSFER_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.TRANSFER),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  senderPublicKey: new Base58(true),
  assetId: new AssetId(true),
  feeAssetId: new AssetId(false),
  timestamp: new Long(true),
  amount: new Long(true),
  fee: new Long(true),
  recipient: new Recipient(true),
  attachment: new Attachment(true)
}


/**
 *
 * EXPORT
 *
 */

export const TRANSACTIONS = {
  TRANSFER: {
    V2: getTransactionsFactory(TRANSFER_V2),
  }
}


