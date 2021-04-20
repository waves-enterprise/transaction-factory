package com.wavesenterprise.crypto.internals.gost

import com.wavesenterprise.crypto.internals.PrivateKey

case class GostPrivateKey(internal: java.security.PrivateKey) extends PrivateKey
