### TRANSACTIONS-FACTORY
Модуль предназначен для создания тела транзакции с логикой подсчёта байт для подписи.

#### Генерируемые файлы:
- src/Transactions.ts - содержит логику подсчёта байт, служит основой для генерации типа транзакции
- src/constants.ts - содержит констаты: тип транзакции и версии
- tests/Tests.ts - содержит тестовые примеры всех типов транзакций  
При добавлении новых транзакций, данные файлы генерируются командой ноды, после чего вручную необходимо их обновить в репозитории.

### Пример использования:

```typescript
const {TRANSACTIONS} = require('@vostokplatform/transactions-factory')

const tx = {
    senderPublicKey: "34qsNWsKKQaysTzpsf4aTyRS6Q1BoUuBntgGVj6SHZg3",
    contractId: "DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg",
    params: [{"type":"integer", "key": "height", "value": 100}],
    fee: 1000000,
    timestamp: 1598008066632,
    feeAssetId: "WAVES"
}

const callTx = TRANSACTIONS.CALL_CONTRACT.V3(tx)
callTx.contractVersion = 2
...

// get bytes
const bytes = callTx.getBytes()

// get validation errors
const errors = callTx.getErrors()

// get signature
const signature = callTx.getSignature(privateKey)

```
