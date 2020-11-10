/**
* Generated by TxSchemeTypeScriptPlugin. DO NOT EDIT.
*/

import { TRANSACTIONS } from '../src/';
import { config } from "@wavesenterprise/signature-generator";
import * as expect from 'expect';

const decoder = new TextDecoder('utf-8');

describe('', () => {
  beforeEach(() => {
    config.set({networkByte: 84, crypto: 'waves'})
  });

  it('CREATE_CONTRACT_V3', async () => {
    const transaction = {
      senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
      image: "localhost:5000/smart-kv",
      imageHash: "b48d1de58c39d2160a4b8a5a9cae90818da1212742ec1f11fba1209bed0a212c",
      contractName: "SomeName",
      params: [{"type":"integer", "key": "height", "value": 100}],
      fee: 1000000,
      timestamp: 1598008066632,
      feeAssetId: "WAVES",
      atomicBadge: {}
    };
    const signatureGenerator = TRANSACTIONS.CREATE_CONTRACT.V3(transaction);
    const bytes = await signatureGenerator.getBytes();

    expect(decoder.decode(bytes))
      .toEqual(
        decoder.decode(Int8Array.from(
          [103,3,30,-77,95,61,75,82,107,-77,-99,-102,-43,-96,-127,-49,-51,75,-103,
            37,53,-128,108,-12,-111,-120,-122,-111,43,17,46,65,-56,8,0,23,108,111,99,97,108,
            104,111,115,116,58,53,48,48,48,47,115,109,97,114,116,45,107,118,0,64,98,52,56,
            100,49,100,101,53,56,99,51,57,100,50,49,54,48,97,52,98,56,97,53,97,57,99,97,101,
            57,48,56,49,56,100,97,49,50,49,50,55,52,50,101,99,49,102,49,49,102,98,97,49,50,
            48,57,98,101,100,48,97,50,49,50,99,0,8,83,111,109,101,78,97,109,101,0,1,0,6,104,
            101,105,103,104,116,0,0,0,0,0,0,0,0,100,0,0,0,0,0,15,66,64,0,0,1,116,16,-76,2,72,
            1,1,0]
        )).toString()
      )
  })
});