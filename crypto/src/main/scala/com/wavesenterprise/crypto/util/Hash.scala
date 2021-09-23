package com.wavesenterprise.crypto.util

trait Hash {

  def update(data: Array[Byte]): Unit

  def result(): Array[Byte]

  def reset(): Unit

}
