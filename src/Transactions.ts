import {
  Alias,
  ArrayOfStringsWithLength,
  AssetId,
  Base58,
  Base58WithLength,
  Base64,
  Bool,
  Byte,
  ByteArrayWithSize,
  Integer,
  Long,
  PermissionOpType,
  PermissionOptions,
  Recipient,
  StringWithLength,
  Transfers,
  TxType,
  TxVersion
} from '@vostokplatform/signature-generator'
import { TRANSACTION_TYPES, TRANSACTION_VERSIONS } from './constants'
import { getTransactionsFactory, Processor } from './TransactionsFactory'


const REGISTER_NODE = {
  tx_type: new TxType(true, TRANSACTION_TYPES.REGISTER_NODE),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  target: new Base58(true),
  nodeName: new StringWithLength(true),
  opType: new PermissionOpType(true),
  timestamp: new Long(true),
  fee: new Long(true)
}

const CREATE_ALIAS_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CREATE_ALIAS),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  alias: new Alias(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const CREATE_ALIAS_V3 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CREATE_ALIAS),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V3),
  sender: new Base58(true),
  alias: new Alias(true),
  fee: new Long(true),
  timestamp: new Long(true),
  feeAssetId: new AssetId(false)
}

const ISSUE_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.ISSUE),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  chainId: new Byte(true),
  sender: new Base58(true),
  name: new ByteArrayWithSize(true),
  description: new ByteArrayWithSize(true),
  quantity: new Long(true),
  decimals: new Byte(true),
  reissuable: new Bool(true),
  fee: new Long(true),
  timestamp: new Long(true),
  script: new Base64(false)
}

const REISSUE_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.REISSUE),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  chainId: new Byte(true),
  sender: new Base58(true),
  assetId: new AssetId(true),
  quantity: new Long(true),
  reissuable: new Bool(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const BURN_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.BURN),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  chainId: new Byte(true),
  sender: new Base58(true),
  assetId: new AssetId(true),
  amount: new Long(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const LEASE_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.LEASE),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  assetId: new AssetId(false),
  sender: new Base58(true),
  recipient: new Recipient(true),
  amount: new Long(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const LEASE_CANCEL_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.LEASE_CANCEL),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  chainId: new Byte(true),
  sender: new Base58(true),
  fee: new Long(true),
  timestamp: new Long(true),
  leaseId: new AssetId(true)
}

const SPONSOR_FEE = {
  tx_type: new TxType(true, TRANSACTION_TYPES.SPONSOR_FEE),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  assetId: new AssetId(true),
  isEnabled: new Bool(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const SET_ASSET_SCRIPT = {
  tx_type: new TxType(true, TRANSACTION_TYPES.SET_ASSET_SCRIPT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  chainId: new Byte(true),
  sender: new Base58(true),
  assetId: new AssetId(true),
  script: new Base64(false),
  fee: new Long(true),
  timestamp: new Long(true)
}

const DATA = {
  tx_type: new TxType(true, TRANSACTION_TYPES.DATA),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  author: new Base58(true),
  timestamp: new Long(true),
  fee: new Long(true)
}

const DATA_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.DATA),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  author: new Base58(true),
  timestamp: new Long(true),
  fee: new Long(true),
  feeAssetId: new AssetId(false)
}

const TRANSFER_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.TRANSFER),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  assetId: new AssetId(false),
  feeAssetId: new AssetId(false),
  timestamp: new Long(true),
  amount: new Long(true),
  fee: new Long(true),
  recipient: new Recipient(true),
  attachment: new ByteArrayWithSize(true)
}

const MASS_TRANSFER = {
  tx_type: new TxType(true, TRANSACTION_TYPES.MASS_TRANSFER),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  assetId: new AssetId(false),
  transfers: new Transfers(true),
  timestamp: new Long(true),
  fee: new Long(true),
  attachment: new ByteArrayWithSize(true)
}

const MASS_TRANSFER_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.MASS_TRANSFER),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  assetId: new AssetId(false),
  transfers: new Transfers(true),
  timestamp: new Long(true),
  fee: new Long(true),
  attachment: new ByteArrayWithSize(true),
  feeAssetId: new AssetId(false)
}

const PERMIT = {
  tx_type: new TxType(true, TRANSACTION_TYPES.PERMIT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  target: new Recipient(true),
  timestamp: new Long(true),
  fee: new Long(true),
  permissionOp: new PermissionOptions(true)
}

const CREATE_POLICY = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CREATE_POLICY),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  policyName: new StringWithLength(true),
  description: new StringWithLength(true),
  recipients: new ArrayOfStringsWithLength(true),
  owners: new ArrayOfStringsWithLength(true),
  timestamp: new Long(true),
  fee: new Long(true)
}

const CREATE_POLICY_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CREATE_POLICY),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  policyName: new StringWithLength(true),
  description: new StringWithLength(true),
  recipients: new ArrayOfStringsWithLength(true),
  owners: new ArrayOfStringsWithLength(true),
  timestamp: new Long(true),
  fee: new Long(true),
  feeAssetId: new AssetId(false)
}

const UPDATE_POLICY = {
  tx_type: new TxType(true, TRANSACTION_TYPES.UPDATE_POLICY),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  policyId: new Base58WithLength(true),
  recipients: new ArrayOfStringsWithLength(true),
  owners: new ArrayOfStringsWithLength(true),
  opType: new PermissionOpType(true),
  timestamp: new Long(true),
  fee: new Long(true)
}

const UPDATE_POLICY_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.UPDATE_POLICY),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  policyId: new Base58WithLength(true),
  recipients: new ArrayOfStringsWithLength(true),
  owners: new ArrayOfStringsWithLength(true),
  opType: new PermissionOpType(true),
  timestamp: new Long(true),
  fee: new Long(true),
  feeAssetId: new AssetId(false)
}

const POLICY_DATA_HASH = {
  tx_type: new TxType(true, TRANSACTION_TYPES.POLICY_DATA_HASH),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  policyId: new Base58WithLength(true),
  timestamp: new Long(true),
  fee: new Long(true)
}

const POLICY_DATA_HASH_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.POLICY_DATA_HASH),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  policyId: new Base58WithLength(true),
  timestamp: new Long(true),
  fee: new Long(true),
  feeAssetId: new AssetId(false)
}

const CREATE_CONTRACT = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CREATE_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  image: new StringWithLength(true),
  imageHash: new StringWithLength(true),
  contractName: new StringWithLength(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const CREATE_CONTRACT_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CREATE_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  image: new StringWithLength(true),
  imageHash: new StringWithLength(true),
  contractName: new StringWithLength(true),
  fee: new Long(true),
  timestamp: new Long(true),
  feeAssetId: new AssetId(false)
}

const CALL_CONTRACT = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CALL_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  contractId: new Base58WithLength(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const CALL_CONTRACT_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CALL_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  contractId: new Base58WithLength(true),
  fee: new Long(true),
  timestamp: new Long(true),
  contractVersion: new Integer(true)
}

const CALL_CONTRACT_V3 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.CALL_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V3),
  sender: new Base58(true),
  contractId: new Base58WithLength(true),
  fee: new Long(true),
  timestamp: new Long(true),
  contractVersion: new Integer(true),
  feeAssetId: new AssetId(false)
}

const EXECUTED_CONTRACT = {
  tx_type: new TxType(true, TRANSACTION_TYPES.EXECUTED_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  timestamp: new Long(true)
}

const EXECUTED_CONTRACT_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.EXECUTED_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  resultsHash: new Base58WithLength(true),
  timestamp: new Long(true)
}

const DISABLE_CONTRACT = {
  tx_type: new TxType(true, TRANSACTION_TYPES.DISABLE_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  contractId: new Base58WithLength(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const DISABLE_CONTRACT_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.DISABLE_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  contractId: new Base58WithLength(true),
  fee: new Long(true),
  timestamp: new Long(true),
  feeAssetId: new AssetId(false)
}

const UPDATE_CONTRACT = {
  tx_type: new TxType(true, TRANSACTION_TYPES.UPDATE_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V1),
  sender: new Base58(true),
  contractId: new Base58WithLength(true),
  image: new StringWithLength(true),
  imageHash: new StringWithLength(true),
  fee: new Long(true),
  timestamp: new Long(true)
}

const UPDATE_CONTRACT_V2 = {
  tx_type: new TxType(true, TRANSACTION_TYPES.UPDATE_CONTRACT),
  version: new TxVersion(true, TRANSACTION_VERSIONS.V2),
  sender: new Base58(true),
  contractId: new Base58WithLength(true),
  image: new StringWithLength(true),
  imageHash: new StringWithLength(true),
  fee: new Long(true),
  timestamp: new Long(true),
  feeAssetId: new AssetId(false)
}

export const TRANSACTIONS = {
  REGISTER_NODE: {
    V1: getTransactionsFactory(REGISTER_NODE)
  },
  CREATE_ALIAS: {
    V2: getTransactionsFactory(CREATE_ALIAS_V2),
    V3: getTransactionsFactory(CREATE_ALIAS_V3)
  },
  ISSUE: {
    V2: getTransactionsFactory(ISSUE_V2)
  },
  REISSUE: {
    V2: getTransactionsFactory(REISSUE_V2)
  },
  BURN: {
    V2: getTransactionsFactory(BURN_V2)
  },
  LEASE: {
    V2: getTransactionsFactory(LEASE_V2)
  },
  LEASE_CANCEL: {
    V2: getTransactionsFactory(LEASE_CANCEL_V2)
  },
  SPONSOR_FEE: {
    V1: getTransactionsFactory(SPONSOR_FEE)
  },
  SET_ASSET_SCRIPT: {
    V1: getTransactionsFactory(SET_ASSET_SCRIPT)
  },
  DATA: {
    V1: getTransactionsFactory(DATA),
    V2: getTransactionsFactory(DATA_V2)
  },
  TRANSFER: {
    V2: getTransactionsFactory(TRANSFER_V2)
  },
  MASS_TRANSFER: {
    V1: getTransactionsFactory(MASS_TRANSFER),
    V2: getTransactionsFactory(MASS_TRANSFER_V2)
  },
  PERMIT: {
    V1: getTransactionsFactory(PERMIT)
  },
  CREATE_POLICY: {
    V1: getTransactionsFactory(CREATE_POLICY),
    V2: getTransactionsFactory(CREATE_POLICY_V2)
  },
  UPDATE_POLICY: {
    V1: getTransactionsFactory(UPDATE_POLICY),
    V2: getTransactionsFactory(UPDATE_POLICY_V2)
  },
  POLICY_DATA_HASH: {
    V1: getTransactionsFactory(POLICY_DATA_HASH),
    V2: getTransactionsFactory(POLICY_DATA_HASH_V2)
  },
  CREATE_CONTRACT: {
    V1: getTransactionsFactory(CREATE_CONTRACT),
    V2: getTransactionsFactory(CREATE_CONTRACT_V2)
  },
  CALL_CONTRACT: {
    V1: getTransactionsFactory(CALL_CONTRACT),
    V2: getTransactionsFactory(CALL_CONTRACT_V2),
    V3: getTransactionsFactory(CALL_CONTRACT_V3)
  },
  EXECUTED_CONTRACT: {
    V1: getTransactionsFactory(EXECUTED_CONTRACT),
    V2: getTransactionsFactory(EXECUTED_CONTRACT_V2)
  },
  DISABLE_CONTRACT: {
    V1: getTransactionsFactory(DISABLE_CONTRACT),
    V2: getTransactionsFactory(DISABLE_CONTRACT_V2)
  },
  UPDATE_CONTRACT: {
    V1: getTransactionsFactory(UPDATE_CONTRACT),
    V2: getTransactionsFactory(UPDATE_CONTRACT_V2)
  }
}


