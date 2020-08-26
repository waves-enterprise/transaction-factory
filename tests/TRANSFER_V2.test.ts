import { TRANSACTIONS } from "../src/Transactions";
import { config } from "@vostokplatform/signature-generator";


describe('', () => {
  beforeEach(() => {
    config.set({networkByte: 84, crypto: 'waves'})
  });
  it('TRANSFER_V2', async () => {
    const transaction = {
      amount: "100000000",
      assetId: "WAVES",
      attachment: "",
      fee: 1000000,
      feeAssetId: "WAVES",
      recipient: "3NiVPB1t32jC3SJpLomY3Zv6kwvfaJpRkqS",
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      timestamp: 1598008066632
    };
    const signatureGenerator = TRANSACTIONS.TRANSFER.V2(transaction);
    const Uint8Bytes = await signatureGenerator.getBytes();
    const Int8Bytes = Int8Array.from(Uint8Bytes)
    console.log(JSON.stringify(Array.from(Int8Bytes)));
  })
});
