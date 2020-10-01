/**
* Generated by TxSchemeTypeScriptPlugin. DO NOT EDIT.
*/

import { TRANSACTIONS } from '../src/';
import { config } from "@wavesenterprise/signature-generator";


describe('', () => {
  beforeEach(() => {
    config.set({networkByte: 84, crypto: 'waves'})
  });

  it('REGISTER_NODE', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      targetPubKey: "FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z",
      nodeName: "node-0",
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000
    };
    const signatureGenerator = TRANSACTIONS.REGISTER_NODE.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_ALIAS_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      alias: "John",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.CREATE_ALIAS.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_ALIAS_V3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      alias: "John",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.CREATE_ALIAS.V3(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('ISSUE_V2', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      name: "D56Gk8tvSAhNesghXgjAw67rSYDf4F2vo7HmsFTuGweC",
      description: "Some script",
      quantity: 10000000,
      decimals: 2,
      reissuable: true,
      fee: 1000000,
      timestamp: 1598008066632,
      script: "base64:3rbFDtbPwAvSp2vBvqGfGR9nRS1nBVnfuSCN3HxSZ7fVRpt3tuFG5JSmyTmvHPxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtE"
    };
    const signatureGenerator = TRANSACTIONS.ISSUE.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('REISSUE_V2', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      quantity: 10000000,
      reissuable: true,
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.REISSUE.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('BURN_V2', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      amount: "100000000",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.BURN.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('LEASE_V2', async () => {
    const transaction = {
      assetId: "WAVES",
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      amount: "100000000",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.LEASE.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('LEASE_CANCEL_V2', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      fee: 1000000,
      timestamp: 1598008066632,
      leaseId: "E9yZC4cVhCDfbjFJCc9CqkAtkoFy5KaCe64iaxHM2adG"
    };
    const signatureGenerator = TRANSACTIONS.LEASE_CANCEL.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('SPONSOR_FEE', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      isEnabled: true,
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.SPONSOR_FEE.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('SET_ASSET_SCRIPT', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      script: "base64:3rbFDtbPwAvSp2vBvqGfGR9nRS1nBVnfuSCN3HxSZ7fVRpt3tuFG5JSmyTmvHPxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtE",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.SET_ASSET_SCRIPT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DATA', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      authorPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      data: [{"type":"integer", "key": "height", "value": 100}],
      timestamp: 1598008066632,
      fee: 1000000
    };
    const signatureGenerator = TRANSACTIONS.DATA.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DATA_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      authorPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      data: [{"type":"integer", "key": "height", "value": 100}],
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.DATA.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('TRANSFER_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      feeAssetId: "WAVES",
      timestamp: 1598008066632,
      amount: "100000000",
      fee: 1000000,
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      attachment: "base64:3rbFDtbPwAvSp2vBvqGfGR9PxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtEiZ"
    };
    const signatureGenerator = TRANSACTIONS.TRANSFER.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('MASS_TRANSFER', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      transfers: [
        {"recipient":"3NgSJRdMYu4ZbNpSbyRNZLJDX926W7e1EKQ","amount":"1000000000"},
        {"recipient":"3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn","amount":"1000000000"},
        {"recipient":"3NpkC1FSW9xNfmAMuhRSRArLgnfyGyEry7w","amount":"1000000000"},
        {"recipient":"3NkZd8Xd4KsuPiNVsuphRNCZE3SqJycqv8d","amount":"1000000000"}
      ],
      timestamp: 1598008066632,
      fee: 1000000,
      attachment: "base64:3rbFDtbPwAvSp2vBvqGfGR9PxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtEiZ"
    };
    const signatureGenerator = TRANSACTIONS.MASS_TRANSFER.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('MASS_TRANSFER_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      transfers: [
        {"recipient":"3NgSJRdMYu4ZbNpSbyRNZLJDX926W7e1EKQ","amount":"1000000000"},
        {"recipient":"3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn","amount":"1000000000"},
        {"recipient":"3NpkC1FSW9xNfmAMuhRSRArLgnfyGyEry7w","amount":"1000000000"},
        {"recipient":"3NkZd8Xd4KsuPiNVsuphRNCZE3SqJycqv8d","amount":"1000000000"}
      ],
      timestamp: 1598008066632,
      fee: 1000000,
      attachment: "base64:3rbFDtbPwAvSp2vBvqGfGR9PxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtEiZ",
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.MASS_TRANSFER.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('PERMIT', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      target: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      timestamp: 1598008066632,
      fee: 1000000,
      opType: "add",
      role: "miner",
      duplicate_timestamp: 1598008066632,
      dueTimestamp: 1572600785208
    };
    const signatureGenerator = TRANSACTIONS.PERMIT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_POLICY', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyName: "SomeName",
      description: "Some script",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      timestamp: 1598008066632,
      fee: 1000000
    };
    const signatureGenerator = TRANSACTIONS.CREATE_POLICY.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_POLICY_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyName: "SomeName",
      description: "Some script",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.CREATE_POLICY.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_POLICY', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_POLICY.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_POLICY_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_POLICY.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_CONTRACT', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.CREATE_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_CONTRACT_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.CREATE_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CALL_CONTRACT', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.CALL_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CALL_CONTRACT_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      contractVersion: 2
    };
    const signatureGenerator = TRANSACTIONS.CALL_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CALL_CONTRACT_V3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      contractVersion: 2,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.CALL_CONTRACT.V3(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DISABLE_CONTRACT', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.DISABLE_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DISABLE_CONTRACT_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.DISABLE_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_CONTRACT', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_CONTRACT_V2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES"
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('SET_SCRIPT', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      script: "base64:3rbFDtbPwAvSp2vBvqGfGR9nRS1nBVnfuSCN3HxSZ7fVRpt3tuFG5JSmyTmvHPxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtE",
      name: "D56Gk8tvSAhNesghXgjAw67rSYDf4F2vo7HmsFTuGweC",
      description: "Some script",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.SET_SCRIPT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })
});
