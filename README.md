## 1. Introduction

`we-core` library provides ability to work with Waves Enterprise blockchain transactions and REST/gRPC APIs.

## 2. Using we-core

* Gradle

      dependencies {
          implementation 'com.wavesenterprise:we-core:1.7.0'
      }
* SBT

      libraryDependencies += "com.wavesenterprise" % "we-core" % "1.7.0"

### GOST cryptography

If GOST cryptography is going to be used, follow the `com.wavesenterprise.CryptoVersion` class to find supported CryptoPro CSP and JCSP versions. Install [CryptoPro CSP](https://www.cryptopro.ru/products/csp) and include [JCSP JARs](https://www.cryptopro.ru/products/csp/jcsp) into the classpath (e.g., put all CryptoPro JARs into `crypto/lib` directory).

* Gradle

      dependencies {
          compile fileTree(dir: 'crypto/lib', include: ['*.jar'])
      }

* SBT

      Compile / unmanagedJars ++= (baseDirectory.value / "crypto" / "lib" ** "*.jar").classpath

To test GOST cryptography run the following code snippet. `true` should be printed into a console.

    import com.wavesenterprise.account.AddressScheme;
    import com.wavesenterprise.crypto.CryptoInitializer;
    import com.wavesenterprise.javadsl.crypto.CryptoState;
    import com.wavesenterprise.javadsl.settings.CryptoSettings;

    public class GostTest {
        public static void main(String[] args) {
            CryptoInitializer.init(CryptoSettings.GOST_CRYPTO_SETTINGS);
            System.out.println(CryptoState.isGost());
        }
    }

## 3. Code walkthrough

In order to work with transactions from blockchain, cryptography must be initialised. Address scheme byte must be also provided according to a target blockchain network.

Classes from `com.wavesenterprise.javadsl` package should be considered for better compatibility with Java.

* Waves cryptography

      import com.wavesenterprise.crypto.CryptoInitializer;
      import com.wavesenterprise.settings.javadsl.CryptoSettings;

      CryptoInitializer.init(CryptoSettings.WAVES_CRYPTO_SETTINGS);
      AddressScheme.setAddressSchemaByte('T');

* GOST cryptography

      import com.wavesenterprise.account.AddressScheme;
      import com.wavesenterprise.crypto.CryptoInitializer;
      import com.wavesenterprise.javadsl.settings.CryptoSettings;

      CryptoInitializer.init(CryptoSettings.GOST_CRYPTO_SETTINGS);
      AddressScheme.setAddressSchemaByte('I');

Retrieving key pair from a key store. Note that CryptoPro key store has HDIMAGE type and no key store path must be provided.

    import com.wavesenterprise.javadsl.crypto.internals.KeyStore;
    import com.wavesenterprise.javadsl.crypto.CryptoState;

    com.wavesenterprise.crypto.internals.KeyStore<KeyPair> keyStore = CryptoState.keyStore(Optional.of(new File("keystore_path")), "keystore_password".toCharArray());
    keyStore.getKeyPair("public_key_alias", Optional.of("private_key_password".toCharArray())).flatMap(keyPair -> {
        // any logic to work with the keyPair
    });

Create and sign a transaction:

    import com.wavesenterprise.account.PrivateKeyAccount;
    import com.wavesenterprise.javadsl.acl.OpType;
    import com.wavesenterprise.javadsl.utils.NumberUtils;
    import com.wavesenterprise.transaction.ValidationError;
    import com.wavesenterprise.transaction.RegisterNodeTransactionV1;
    import scala.Option;

    PrivateKeyAccount account = PrivateKeyAccount.apply(keyPair);
    long fee = NumberUtils.doubleToWest(1.0);
    com.wavesenterprise.acl.OpType add = OpType.ADD;
    Either<ValidationError, RegisterNodeTransactionV1> txOrError = RegisterNodeTransactionV1.selfSigned(account, account, Option.apply("node01"), add, System.currentTimeMillis(), fee);

## 4. gRPC API

gRPC services could be found in `com.wavesenterprise.protobuf` package.

## 5. REST API

todo: REST request/response objects should be added in future releases
