import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.toPlatformDepsGroupID
import sbt._

object Dependencies {

  val supportedCspVersion              = "5.0.11823"
  val supportedJcspVersion             = "5.0.40621-A"
  val supportedExperimentalCspVersion  = "5R2-RC7"
  val supportedExperimentalJcspVersion = "5.0.41993-A"

  lazy val serialization = Seq(
    "com.google.guava"  % "guava"                     % "28.1-jre",
    "com.typesafe.play" %% "play-json"                % "2.7.4",
    "org.julienrf"      %% "play-json-derived-codecs" % "6.0.0"
  )

  lazy val logging = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.26"
  )

  lazy val meta  = Seq("com.chuusai" %% "shapeless" % "2.3.3")
  lazy val monix = Def.setting(Seq("io.monix" %%% "monix" % "3.1.0"))

  lazy val scodec    = Def.setting(Seq("org.scodec" %%% "scodec-core" % "1.10.3"))
  lazy val fastparse = Def.setting(Seq("com.lihaoyi" %%% "fastparse" % "2.2.4"))
  lazy val ficus     = Seq("com.iheart" %% "ficus" % "1.4.7")
  lazy val scorex = Seq(
    ("org.scorexfoundation" %% "scrypto" % "2.1.6")
      .exclude("ch.qos.logback", "logback-classic")
      .exclude("com.typesafe.scala-logging", "scala-logging_2.12")
      .exclude("com.google.guava", "guava")
      .exclude("org.bouncycastle", "*"),
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
  )

  lazy val bouncyCastle = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.64",
    "org.bouncycastle" % "bcpkix-jdk15on" % "1.64"
  )

  lazy val commonsNet = Seq("commons-net" % "commons-net" % "3.6")
  lazy val commonsLang = Seq(
    "org.apache.commons" % "commons-lang3" % "3.8",
    "commons-codec"      % "commons-codec" % "1.11"
  )

  lazy val scalatest  = Seq("org.scalatest"  %% "scalatest"  % "3.0.8")
  lazy val scalacheck = Seq("org.scalacheck" %% "scalacheck" % "1.14.1")

  lazy val catsCore   = Seq("org.typelevel" %% "cats-core" % "2.0.0")
  lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % "2.0.0")
  lazy val catsMtl    = Seq("org.typelevel" %% "cats-mtl-core" % "0.7.0")
  lazy val fp         = catsCore ++ catsEffect ++ catsMtl

  lazy val kindProjector = "org.spire-math" %% "kind-projector" % "0.9.10"
  lazy val enumeratum    = Seq("com.beachape" %% "enumeratum-play-json" % "1.5.16")

  lazy val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.12.2"
  )

  lazy val protobuf = Seq("com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf")
}
