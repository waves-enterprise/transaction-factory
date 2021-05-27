package com.wavesenterprise.transaction.generator

import java.io.File
import com.wavesenterprise.transaction.TxScheme
import com.wavesenterprise.transaction.generator.scala.{AtomicInnerTxAdapterGenerator, TxScalaGenerator}
import sbt.Keys.{baseDirectory, sourceGenerators, sourceManaged}
import sbt._

object TxSchemePlugin extends AutoPlugin {

  override lazy val projectSettings = Seq(
    (sourceGenerators in Compile) += codeGenerator
  )

  lazy val codeGenerator = Def.task {
    val path = (sourceManaged in Compile).value
    generate(path)
  }

  private def generate(outputDir: File): Seq[File] =
    generateInnerTxAdapter(outputDir) +:
      generateTransactions(outputDir)

  private def generateTransactions(outputDir: File): Seq[File] = {
    TxScheme.values.map { scheme =>
      val packageDir = new File(outputDir, scheme.packageName.replace('.', '/'))
      packageDir.mkdirs()
      val code = TxScalaGenerator.buildScala(scheme)
      val file = new File(packageDir, s"${scheme.entryName}.scala")
      writeTextFile(file, code)
    }
  }

  private def generateInnerTxAdapter(outputDir: File): File = {
    val packageDir = new File(outputDir, AtomicInnerTxAdapterGenerator.PackageName.replace('.', '/'))
    packageDir.mkdirs()
    val file = new File(packageDir, s"${AtomicInnerTxAdapterGenerator.ObjectName}.scala")
    val code = AtomicInnerTxAdapterGenerator.buildWriter(TxScheme.values, skipContainers = true).build()
    writeTextFile(file, code)
    file
  }
}
