import { TRANSACTIONS } from '../src';
import { config } from "@vostokplatform/signature-generator";


describe('', () => {
  beforeEach(() => {
    config.set({networkByte: 84, crypto: 'waves'})
  });
  
  it('REGISTER_NODE', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      target: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      nodeName: "node-0",
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.REGISTER_NODE.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_ALIAS_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      alias: "John",
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CREATE_ALIAS.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_ALIAS_V3', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      alias: "John",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CREATE_ALIAS.V3(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('ISSUE_V2', async () => {
    const transaction = {
      chainId: 1,
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      name: "D56Gk8tvSAhNesghXgjAw67rSYDf4F2vo7HmsFTuGweC",
      description: "Some script",
      quantity: 10000000,
      decimals: 2,
      reissuable: true,
      fee: 1000000,
      timestamp: 1598008066632,
      script: "base64:3rbFDtbPwAvSp2vBvqGfGR9nRS1nBVnfuSCN3HxSZ7fVRpt3tuFG5JSmyTmvHPxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtE",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.ISSUE.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('REISSUE_V2', async () => {
    const transaction = {
      chainId: 1,
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      assetId: "WAVES",
      quantity: 10000000,
      reissuable: true,
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.REISSUE.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('BURN_V2', async () => {
    const transaction = {
      chainId: 1,
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      assetId: "WAVES",
      amount: "100000000",
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.BURN.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('LEASE_V2', async () => {
    const transaction = {
      assetId: "WAVES",
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      amount: "100000000",
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.LEASE.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('LEASE_CANCEL_V2', async () => {
    const transaction = {
      chainId: 1,
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      fee: 1000000,
      timestamp: 1598008066632,
      leaseId: "E9yZC4cVhCDfbjFJCc9CqkAtkoFy5KaCe64iaxHM2adG",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.LEASE_CANCEL.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('SPONSOR_FEE', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      assetId: "WAVES",
      isEnabled: true,
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.SPONSOR_FEE.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('SET_ASSET_SCRIPT', async () => {
    const transaction = {
      chainId: 1,
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      assetId: "WAVES",
      script: "base64:3rbFDtbPwAvSp2vBvqGfGR9nRS1nBVnfuSCN3HxSZ7fVRpt3tuFG5JSmyTmvHPxYf34SocMRkRKFgzTtXXnnv7upRHXJzZrLSQo8tUW6yMtE",
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.SET_ASSET_SCRIPT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DATA', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      author: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      data: "some_base64_encoded_string",
      timestamp: 1598008066632,
      fee: 1000000,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.DATA.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DATA_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      author: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      data: "some_base64_encoded_string",
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.DATA.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('TRANSFER_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      assetId: "WAVES",
      feeAssetId: "WAVES",
      timestamp: 1598008066632,
      amount: "100000000",
      fee: 1000000,
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      attachment: "",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.TRANSFER.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('MASS_TRANSFER', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      assetId: "WAVES",
      transfers: [
        {"recipient":"3NgSJRdMYu4ZbNpSbyRNZLJDX926W7e1EKQ","amount":"1000000000"},
        {"recipient":"3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn","amount":"1000000000"},
        {"recipient":"3NpkC1FSW9xNfmAMuhRSRArLgnfyGyEry7w","amount":"1000000000"},
        {"recipient":"3NkZd8Xd4KsuPiNVsuphRNCZE3SqJycqv8d","amount":"1000000000"}
        ],
      timestamp: 1598008066632,
      fee: 1000000,
      attachment: "",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.MASS_TRANSFER.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('MASS_TRANSFER_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      assetId: "WAVES",
      transfers: [
        {"recipient":"3NgSJRdMYu4ZbNpSbyRNZLJDX926W7e1EKQ","amount":"1000000000"},
        {"recipient":"3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn","amount":"1000000000"},
        {"recipient":"3NpkC1FSW9xNfmAMuhRSRArLgnfyGyEry7w","amount":"1000000000"},
        {"recipient":"3NkZd8Xd4KsuPiNVsuphRNCZE3SqJycqv8d","amount":"1000000000"}
      ],
      timestamp: 1598008066632,
      fee: 1000000,
      attachment: "",
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.MASS_TRANSFER.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('PERMIT', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      target: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      timestamp: 1598008066632,
      fee: 1000000,
      permissionOp: "{'opType': 'remove', 'role: miner', 'dueTimestamp': 1572600785208}",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.PERMIT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_POLICY', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      policyName: "SomeName",
      description: "Some script",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      timestamp: 1598008066632,
      fee: 1000000,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CREATE_POLICY.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_POLICY_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      policyName: "SomeName",
      description: "Some script",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CREATE_POLICY.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_POLICY', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_POLICY.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_POLICY_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      recipients: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      owners: ["3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn", "3votNaBcgb25FdsdgsdSvYZW4ftJ2ZwLXex"],
      opType: "add",
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_POLICY.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('POLICY_DATA_HASH', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      dataHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      timestamp: 1598008066632,
      fee: 1000000,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.POLICY_DATA_HASH.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('POLICY_DATA_HASH_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      dataHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      policyId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      timestamp: 1598008066632,
      fee: 1000000,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.POLICY_DATA_HASH.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_CONTRACT', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CREATE_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CREATE_CONTRACT_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CREATE_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CALL_CONTRACT', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CALL_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CALL_CONTRACT_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      contractVersion: 2,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CALL_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('CALL_CONTRACT_V3', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      contractVersion: 2,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.CALL_CONTRACT.V3(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('EXECUTED_CONTRACT', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      tx: "",
      results: [{"type":"integer", "key": "height", "value": 100}],
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.EXECUTED_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('EXECUTED_CONTRACT_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      tx: "",
      results: [{"type":"integer", "key": "height", "value": 100}],
      resultsHash: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      validationProofs: "",
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.EXECUTED_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DISABLE_CONTRACT', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.DISABLE_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('DISABLE_CONTRACT_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.DISABLE_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_CONTRACT', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632,
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_CONTRACT.V1(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })

  it('UPDATE_CONTRACT_V2', async () => {
    const transaction = {
      sender: "3NotQaBygbSvYZW4ftJ2ZwLXex4rTHY1Qzn",
      contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      proofs: ["5wMeGz4xzrs1AYJQR7DQU8jK8KJZ4g7HGxiGiZ1H8rfUHJKyKxTcZWFWHhojWuJMjst6kbDYL4EkcV2GyXKffyPU"]
    };
    const signatureGenerator = TRANSACTIONS.UPDATE_CONTRACT.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })
});