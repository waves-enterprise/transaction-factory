import * as expect from 'expect';

import {TRANSACTIONS,} from "../src";
import {toBase58} from "../src/__toBase58";

const tx_bytes = "2MBoJcatRLKfcLmUiQLx9mTsJNzWReRK9qXeeo1Gzab2Bh9Dp6jabPP5ZCHhucnQyqWk3rsuqy8KQR2huY9ozCqvA35BsBWUVc2pvHRwh2PYAxAGE8UcKGuNoyERtADTd1CrsSLGy4xyn5A54pmgFR8g4bJRLY2sXMrEiUTU6H2B3Rp1E5GmuYBvYVN93WwdSucBYYZx3o271gDyWCNvwhsUuCdWMRe2147WFaQtfbwbnx7D81EW2Kz1CDQAqWst9NKAc5CtFKoFGruBsXtBygzugXMJvapnXS8tkFnLmv3RT7Pj3c5gySZmZkxHzM2kTX5"
const v4_bytes = '51mNYmztZTMkm1PuNfh7Z8Sxrwvy7kiR4MRpU5QFdqjRKqwDi6gSoFzQUruzSBzZV7NFmYTTuLHprmNv7xLWMApY4WKSeqCtHrRGRQva7eFj5d3zgaqVsuFANpYzfb9cqfCoqj5bqU7Tc6ct1YcyxsJZGPqCpYYA4WaJC37yhGsBJztAq8h7gkdqUTJfixLJMWni34P3cfJVfPbyHQpaBMpdnY1qgpLHLDAcHi1JAW2j2z9NZhP9v35yYWFeUCco5MRtJFEmguYkzAdtk5ApWsbns88auqePuCBoL9MB9NzWdEPBzE9RbFZYuu2zhqX5'

describe('', () => {
    it('should create signature for call contract v4', async () => {
        const tx = TRANSACTIONS.CallContract.V4({
            timestamp: 1663510702148,
            params: [
                {
                    type: 'string',
                    key: 'action',
                    value: 'transfer',
                },
                {
                    type: "string",
                    key: 'asset',
                    value: "Cb1xsvZYjHgjZAZsdMMtbTwKhJY1kYLfpq1K65uJXJVM"
                },
                {
                    type: "string",
                    key: 'recipient',
                    value: "3NukhxdTpDxXPYky45EPTg1tinzZDUWcbcB"
                },
                {
                    type: "integer",
                    key: 'qty',
                    value: 150
                }
            ],
            contractId: '9TvMKZuebH6NS33it9TeEH8CxmhxKqfunTYaJJc1AMyW',
            senderPublicKey: 'AhQHhsmx9EWrJy6juLovM5DZdYCt7cTeoyX2yaTqDdNP',
            fee: 100000000,
            contractVersion: 1,
        });

        const bytes = await tx.getBytes();

        expect(toBase58(bytes)).toEqual(v4_bytes)
    })

    it('should create signature for call contract v5', async () => {
        const tx = TRANSACTIONS.CallContract.V5({
            timestamp: 1663510702148,
            params: [
                {
                    type: 'string',
                    key: 'action',
                    value: 'transfer',
                },
                {
                    type: "string",
                    key: 'asset',
                    value: "Cb1xsvZYjHgjZAZsdMMtbTwKhJY1kYLfpq1K65uJXJVM"
                },
                {
                    type: "string",
                    key: 'recipient',
                    value: "3NukhxdTpDxXPYky45EPTg1tinzZDUWcbcB"
                },
                {
                    type: "integer",
                    key: 'qty',
                    value: 150
                }
            ],
            contractId: '9TvMKZuebH6NS33it9TeEH8CxmhxKqfunTYaJJc1AMyW',
            senderPublicKey: 'AhQHhsmx9EWrJy6juLovM5DZdYCt7cTeoyX2yaTqDdNP',
            fee: 100000000,
            contractVersion: 1,
            payments: [{
                assetId: 'WEST',
                amount: 1000
            }]
        });

        const bytes = await tx.getBytes();

        expect(toBase58(bytes)).toEqual(tx_bytes)
    })
})
