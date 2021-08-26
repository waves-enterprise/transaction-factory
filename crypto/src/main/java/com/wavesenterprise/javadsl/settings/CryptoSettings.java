package com.wavesenterprise.javadsl.settings;


public interface CryptoSettings {
    com.wavesenterprise.settings.CryptoSettings GOST_CRYPTO_SETTINGS = com.wavesenterprise.settings.CryptoSettings.GostCryptoSettings$.MODULE$;
    com.wavesenterprise.settings.CryptoSettings WAVES_CRYPTO_SETTINGS = com.wavesenterprise.settings.CryptoSettings.WavesCryptoSettings$.MODULE$;
}
