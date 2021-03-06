/**
* Generated by TxSchemeTypeScriptPlugin. DO NOT EDIT.
*/

import { TRANSACTIONS } from '../src/';
import * as expect from 'expect';

describe('', () => {
  it('RegisterNode', async () => {
    const txBody = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      targetPubKey: "FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z",
      nodeName: "node-0",
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000
    };
    const Tx = TRANSACTIONS.RegisterNode.V1(txBody);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreateAliasV2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      alias: "John",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.CreateAlias.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreateAliasV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      alias: "John",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES"
    };
    const Tx = TRANSACTIONS.CreateAlias.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('IssueV2', async () => {
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
    const Tx = TRANSACTIONS.Issue.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('ReissueV2', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      quantity: 10000000,
      reissuable: true,
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.Reissue.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('BurnV2', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      amount: "100000000",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.Burn.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('LeaseV2', async () => {
    const transaction = {
      assetId: "WAVES",
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      amount: "100000000",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.Lease.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('LeaseCancelV2', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      fee: 1000000,
      timestamp: 1598008066632,
      leaseId: "E9yZC4cVhCDfbjFJCc9CqkAtkoFy5KaCe64iaxHM2adG"
    };
    const Tx = TRANSACTIONS.LeaseCancel.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('SponsorFee', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      isEnabled: true,
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.SponsorFee.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('SetAssetScript', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      script: "base64:3rbFDtbPwAvSp2vBvqGfGR9nRS1nBVnfuSCN3HxSZ7fVRpt3tuFG5JSmyTmvHPxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtE",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.SetAssetScript.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('Data', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      authorPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      data: [{"type":"integer", "key": "height", "value": 100}],
      timestamp: 1598008066632,
      fee: 1000000
    };
    const Tx = TRANSACTIONS.Data.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('DataV2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      authorPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      data: [{"type":"integer", "key": "height", "value": 100}],
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES"
    };
    const Tx = TRANSACTIONS.Data.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('TransferV2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      feeAssetId: "WAVES",
      timestamp: 1598008066632,
      amount: "100000000",
      fee: 1000000,
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      attachment: "3rbFDtbPwAvSp2vBvqGfGR9PxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtEiZ"
    };
    const Tx = TRANSACTIONS.Transfer.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('TransferV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      assetId: "WAVES",
      feeAssetId: "WAVES",
      timestamp: 1598008066632,
      amount: "100000000",
      fee: 1000000,
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      attachment: "3rbFDtbPwAvSp2vBvqGfGR9PxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtEiZ",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.Transfer.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('MassTransfer', async () => {
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
      attachment: "3rbFDtbPwAvSp2vBvqGfGR9PxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtEiZ"
    };
    const Tx = TRANSACTIONS.MassTransfer.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('MassTransferV2', async () => {
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
      attachment: "3rbFDtbPwAvSp2vBvqGfGR9PxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtEiZ",
      feeAssetId: "WAVES"
    };
    const Tx = TRANSACTIONS.MassTransfer.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('Permit', async () => {
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
    const Tx = TRANSACTIONS.Permit.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('PermitV2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      target: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      timestamp: 1598008066632,
      fee: 1000000,
      opType: "add",
      role: "miner",
      duplicate_timestamp: 1598008066632,
      dueTimestamp: 1572600785208,
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.Permit.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreatePolicy', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyName: "SomeName",
      description: "Some script",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      timestamp: 1598008066632,
      fee: 1000000
    };
    const Tx = TRANSACTIONS.CreatePolicy.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreatePolicyV2', async () => {
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
    const Tx = TRANSACTIONS.CreatePolicy.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreatePolicyV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyName: "SomeName",
      description: "Some script",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.CreatePolicy.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('UpdatePolicy', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000
    };
    const Tx = TRANSACTIONS.UpdatePolicy.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('UpdatePolicyV2', async () => {
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
    const Tx = TRANSACTIONS.UpdatePolicy.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('UpdatePolicyV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.UpdatePolicy.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('PolicyDataHashV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      dataHash: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.PolicyDataHash.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreateContract', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.CreateContract.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreateContractV2', async () => {
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
    const Tx = TRANSACTIONS.CreateContract.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreateContractV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.CreateContract.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CreateContractV4', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      },
      validationPolicy: {
        type: 0
      },
      apiVersion: "1.1"
    };
    const Tx = TRANSACTIONS.CreateContract.V4(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CallContract', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.CallContract.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CallContractV2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      contractVersion: 2
    };
    const Tx = TRANSACTIONS.CallContract.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CallContractV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      contractVersion: 2,
      feeAssetId: "WAVES"
    };
    const Tx = TRANSACTIONS.CallContract.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('CallContractV4', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      contractVersion: 2,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.CallContract.V4(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('DisableContract', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.DisableContract.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('DisableContractV2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES"
    };
    const Tx = TRANSACTIONS.DisableContract.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('DisableContractV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.DisableContract.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('UpdateContract', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.UpdateContract.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('UpdateContractV2', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES"
    };
    const Tx = TRANSACTIONS.UpdateContract.V2(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('UpdateContractV3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      }
    };
    const Tx = TRANSACTIONS.UpdateContract.V3(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('UpdateContractV4', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      atomicBadge: {
        trustedSender: ""
      },
      validationPolicy: {
        type: 0
      },
      apiVersion: "1.1"
    };
    const Tx = TRANSACTIONS.UpdateContract.V4(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('SetScript', async () => {
    const transaction = {
      chainId: 1,
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      script: "base64:3rbFDtbPwAvSp2vBvqGfGR9nRS1nBVnfuSCN3HxSZ7fVRpt3tuFG5JSmyTmvHPxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtE",
      name: "D56Gk8tvSAhNesghXgjAw67rSYDf4F2vo7HmsFTuGweC",
      description: "Some script",
      fee: 1000000,
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.SetScript.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })

  it('Atomic', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      transactions: [],
      timestamp: 1598008066632
    };
    const Tx = TRANSACTIONS.Atomic.V1(transaction);

    expect(Tx.isValid()).toEqual(true);
  })
});