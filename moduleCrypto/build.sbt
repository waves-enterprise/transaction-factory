import java.io.File

inConfig(Compile)(Seq(sourceGenerators += gostCryptoVersionSource))

(compile in Compile) := ((compile in Compile) dependsOn checkJCSP).value

scalacOptions += "-Yresolve-term-conflict:object"

lazy val gostCryptoVersionSource = Def.task {
  val gostCryptoVersionFile: File = (sourceManaged in Compile).value / "com" / "wavesplatform" / "CryptoVersion.scala"

  IO.write(
    gostCryptoVersionFile,
    s"""package com.wavesplatform
       |
       |object CryptoVersion {
       |  val supportedCspVersion              = "${Dependencies.supportedCspVersion}"
       |  val supportedJcspVersion             = "${Dependencies.supportedJcspVersion}"
       |  val supportedExperimentalCspVersion  = "${Dependencies.supportedExperimentalCspVersion}"
       |  val supportedExperimentalJcspVersion = "${Dependencies.supportedExperimentalJcspVersion}"
       |}
       |""".stripMargin
  )

  Seq(gostCryptoVersionFile)
}

lazy val checkJCSP = taskKey[Unit]("check JCSP installed in JRE")

checkJCSP := Def
  .sequential(
    Def.task {
      CheckJCSP.run()
    },
    printMessageTask("JCSP checked successfully")
  )
  .value

def printMessageTask(msg: String) = Def.task {
  printMessage(msg)
}

def printMessage(msg: String): Unit = {
  println(" " + ">" * 3 + " " + msg)
}
