import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.toPlatformDepsGroupID
import sbt._

object Dependencies {

  val supportedCspVersion              = "5.0.11823"
  val supportedJcspVersion             = "5.0.40621-A"
  val supportedExperimentalCspVersion  = "5R2-RC7"
  val supportedExperimentalJcspVersion = "5.0.41993-A"

  def akkaModule(module: String): ModuleID = "com.typesafe.akka" %% s"akka-$module" % "2.5.26"

  def akkaHttpModule(module: String = ""): ModuleID = "com.typesafe.akka" %% s"akka-http${if (module.isEmpty) "" else s"-$module"}" % "10.1.11"

//  def nettyModule(module: String): ModuleID = "io.netty" % s"netty-$module" % "4.1.45.Final"

//  def kamonModule(module: String, v: String): ModuleID = "io.kamon" %% s"kamon-$module" % v

  val AkkaHTTP = akkaHttpModule()

//  val asyncHttpClient = "org.asynchttpclient" % "async-http-client" % "2.10.5"

//  lazy val netty = Seq("handler", "buffer", "codec", "codec-http2").map(nettyModule)

//  lazy val network = netty ++ Seq(
//    "org.bitlet" % "weupnp" % "0.1.4",
//     Solves an issue with kamon-influxdb
//    asyncHttpClient
//  )

//  lazy val testKit = scalatest ++ scalacheck ++ Seq(
//    akkaModule("testkit"),
//    "org.mockito"   % "mockito-all"                  % "1.10.19",
//    "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0",
//    akkaHttpModule("testkit"),
//    "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.3"
//  )

//  val dockerJavaVersion = "3.2.5"
//  lazy val docker = Seq(
//    "com.github.docker-java" % "docker-java-api" % dockerJavaVersion,
//    ("com.github.docker-java" % "docker-java-transport-jersey" % dockerJavaVersion)
//      .exclude("com.google.guava", "guava")
//  )
//
//  lazy val itDocker = Seq(
//    ("com.spotify" % "docker-client" % "8.16.0")
//      .exclude("com.google.guava", "guava")
//  )
//
//  lazy val itKit = scalatest ++ itDocker ++ Seq(
//    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-properties" % "2.11.1",
//    asyncHttpClient,
//    "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.3"
//  )

  lazy val serialization = Seq(
    "com.google.guava"  % "guava"                     % "28.1-jre",
    "com.typesafe.play" %% "play-json"                % "2.7.4",
    "org.julienrf"      %% "play-json-derived-codecs" % "6.0.0"
  )

//  lazy val db = Seq("org.rocksdb" % "rocksdbjni" % "6.13.3")

  lazy val logging = Seq(
    "ch.qos.logback"       % "logback-classic"          % "1.2.3",
    "org.slf4j"            % "slf4j-api"                % "1.7.26",
    "org.slf4j"            % "jul-to-slf4j"             % "1.7.26",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
  )

  lazy val http = Seq(
    "com.fasterxml.jackson.core"   % "jackson-databind"      % "2.11.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.1",
    "com.pauldijou"                %% "jwt-play-json"        % "3.2.0",
    AkkaHTTP,
    akkaModule("slf4j"),
    akkaModule("stream")
  )

//  lazy val metrics = Seq(
//    kamonModule("core", "1.1.6"),
//    kamonModule("system-metrics", "1.0.1").exclude("io.kamon", "kamon-core_2.12"),
//    kamonModule("akka-2.5", "1.1.4").exclude("io.kamon", "kamon-core_2.12"),
//    kamonModule("influxdb", "1.0.3"),
//    "org.influxdb" % "influxdb-java" % "2.16"
//  ).map(_.exclude("org.asynchttpclient", "async-http-client"))
//
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

  lazy val commonsNet  = Seq("commons-net"        % "commons-net"   % "3.6")
  lazy val commonsLang = Seq("org.apache.commons" % "commons-lang3" % "3.8")

  lazy val scalatest  = Seq("org.scalatest"  %% "scalatest"  % "3.0.8")
//  lazy val scalactic  = Seq("org.scalactic"  %% "scalactic"  % "3.0.8")
  lazy val scalacheck = Seq("org.scalacheck" %% "scalacheck" % "1.14.1")

  lazy val catsCore   = Seq("org.typelevel" %% "cats-core" % "2.0.0")
  lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % "2.0.0")
  lazy val catsMtl    = Seq("org.typelevel" %% "cats-mtl-core" % "0.7.0")
  lazy val fp         = catsCore ++ catsEffect ++ catsMtl

  lazy val kindProjector = "org.spire-math" %% "kind-projector" % "0.9.10"
  lazy val enumeratum    = Seq("com.beachape" %% "enumeratum-play-json" % "1.5.16")

//  private lazy val postgresql    = "org.postgresql"     % "postgresql"      % "42.2.8"
//  private lazy val slick         = "com.typesafe.slick" %% "slick"          % "3.3.2"
//  private lazy val slickHikariCp = "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2"
//  private lazy val flyway        = "org.flywaydb"       % "flyway-core"     % "5.2.4"

//  private lazy val testcontainers = Seq(
//    "org.testcontainers" % "testcontainers"                   % "1.15.0-rc1" % Test,
//    "org.testcontainers" % "postgresql"                       % "1.15.0-rc1" % Test,
//    "com.dimafeng"       %% "testcontainers-scala-scalatest"  % "0.38.0"     % Test,
//    "com.dimafeng"       %% "testcontainers-scala-postgresql" % "0.38.0"     % Test
//  )
//
//  lazy val dbDependencies = Seq(postgresql, slick, slickHikariCp, flyway) ++ testcontainers
//
//  lazy val awsBom = "software.amazon.awssdk" % "bom" % "2.13.2"
//  lazy val awsS3 = ("software.amazon.awssdk" % "s3" % "2.13.2")
//    .exclude("io.netty", "*")
//    .exclude("commons-logging", "commons-logging")
//    .exclude("org.apache.httpcomponents", "*")
//
//  lazy val awsDependencies = Seq(awsBom, awsS3)
//
//  lazy val console = Seq("com.github.scopt" %% "scopt" % "4.0.0-RC2")

  lazy val janino = "org.codehaus.janino" % "janino" % "3.0.12"

  lazy val javaplot = Seq("com.panayotis" % "javaplot" % "0.5.0" % Test)

  lazy val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig"            % "0.12.2",
    "com.github.pureconfig" %% "pureconfig-squants"    % "0.12.2",
    "com.github.pureconfig" %% "pureconfig-enumeratum" % "0.12.2"
  )

  lazy val licenseDependencies = Seq("com.wavesenterprise" %% "license-models" % "1.2.0")

  lazy val protobuf = Seq("com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf")
}
