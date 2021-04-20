package com.wavesplatform.crypto.internals.gost

import com.wavesplatform.crypto.internals.PrivateKey

case class GostPrivateKey(internal: java.security.PrivateKey) extends PrivateKey
