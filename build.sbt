import com.lightbend.sbt.SbtProguard.autoImport._
import com.typesafe.sbt.SbtGit.GitKeys.gitUncommittedChanges
import com.typesafe.sbt.git.JGit
import com.typesafe.sbt.packager.archetypes.TemplateWriter
import com.wavesplatform.transaction.generator.{TxSchemePlugin, TxSchemeProtoPlugin, TxSchemeTypeScriptPlugin}
import org.eclipse.jgit.submodule.SubmoduleWalk.IgnoreSubmoduleMode
import sbt.Keys.{credentials, sourceGenerators, _}
import sbt.internal.inc.ReflectUtilities
import sbt.librarymanagement.ivy.IvyDependencyResolution
import sbt.{Compile, Credentials, Def, Path, _}
import sbtassembly.MergeStrategy
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import java.io.File
import scala.sys.process.{Process, ProcessLogger}

excludeDependencies ++= Seq(
  // workaround for https://github.com/sbt/sbt/issues/3618
  // include "jakarta.ws.rs" % "jakarta.ws.rs-api" instead
  ExclusionRule("javax.ws.rs", "javax.ws.rs-api")
)

libraryDependencies += "jakarta.ws.rs" % "jakarta.ws.rs-api" % "2.1.5"

enablePlugins(SystemdPlugin, GitVersioning)
scalafmtOnCompile in ThisBuild := true
Global / cancelable := true
fork in run := true
connectInput in run := true

name := "we-core"

/**
  * You have to put your credentials in a local file ~/.sbt/.credentials
  * File structure:
  *
  * realm=Sonatype Nexus Repository Manager
  * host=artifacts.wavesenterprise.com
  * username={YOUR_LDAP_USERNAME}
  * password={YOUR_LDAP_PASSWORD}
  */
credentials += {
  val envUsernameOpt = sys.env.get("nexusUser")
  val envPasswordOpt = sys.env.get("nexusPassword")

  (envUsernameOpt, envPasswordOpt) match {
    case (Some(username), Some(password)) =>
      println("Using credentials from environment for artifacts.wavesenterprise.com")
      Credentials("Sonatype Nexus Repository Manager", "artifacts.wavesenterprise.com", username, password)

    case _ =>
      val localCredentialsFile = Path.userHome / ".sbt" / ".credentials"
      println(s"Going to use ${localCredentialsFile.getAbsolutePath} as credentials for artifacts.wavesenterprise.com")
      Credentials(localCredentialsFile)
  }
}

lazy val coreJarName = settingKey[String]("Name for assembled we-core jar")
coreJarName := s"we-core-${version.value}.jar"

val coreVersionSource = Def.task {
  // WARNING!!!
  // Please, update the fallback version every major and minor releases.
  // This version is used then building from sources without Git repository
  // In case of not updating the version cores build from headless sources will fail to connect to newer versions
  val FallbackVersion = (1, 0, 0)

  val coreVersionFile: File = (sourceManaged in Compile).value / "com" / "wavesplatform" / "Version.scala"
  val versionExtractor      = """(\d+)\.(\d+)\.(\d+).*""".r
  val (major, minor, patch) = version.value match {
    case versionExtractor(ma, mi, pa) => (ma.toInt, mi.toInt, pa.toInt)
    case _                            => FallbackVersion
  }

  IO.write(
    coreVersionFile,
    s"""package com.wavesplatform
       |
       |object Version {
       |  val VersionString = "${version.value}"
       |  val VersionTuple = ($major, $minor, $patch)
       |}
       |""".stripMargin
  )

  Seq(coreVersionFile)
}

normalizedName := s"${name.value}"

gitUncommittedChanges in ThisBuild := JGit(baseDirectory.value).porcelain
  .status()
  .setIgnoreSubmodules(IgnoreSubmoduleMode.ALL)
  .call()
  .hasUncommittedChanges

version in ThisBuild := {
  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, Some("DIRTY"))
  val releaseVersion = git.releaseVersion(
    git.gitCurrentTags.value,
    git.gitTagToVersionNumber.value,
    suffix
  )
  lazy val describedExtended = git.gitDescribedVersion.value.map { described =>
    val commitHashLength = 7
    val (tagVersionWithoutCommitHash, commitHash) =
      described.splitAt(described.length - commitHashLength)
    val tagVersionWithCommitsAhead = tagVersionWithoutCommitHash.dropRight(2)
    s"$tagVersionWithCommitsAhead-$commitHash" + suffix
  }
  releaseVersion
    .orElse(describedExtended)
    .getOrElse(git.formattedDateVersion.value)
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

publishArtifact in (Compile, packageDoc) := false
publishArtifact in (Compile, packageSrc) := false
//mainClass in Compile := Some("com.wavesplatform.Application")
scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ywarn-unused:-implicits",
  "-Xlint",
  "-Yresolve-term-conflict:object",
  "-Ypartial-unification",
  "-language:postfixOps"
)
logBuffered := false

inThisBuild(
  Seq(
    scalaVersion := "2.12.10",
    organization := "com.wavesplatform",
    crossPaths := false,
    scalacOptions ++= Seq("-feature",
                          "-deprecation",
                          "-language:higherKinds",
                          "-language:implicitConversions",
                          "-Ywarn-unused:-implicits",
                          "-Xlint",
                          "-Ypartial-unification")
  ))

scalaModuleInfo ~= (_.map(_.withOverrideScalaVersion(true)))

// for sbt plugins sources resolving
updateSbtClassifiers / dependencyResolution := IvyDependencyResolution((updateSbtClassifiers / ivyConfiguration).value)
resolvers ++= Seq(
  "WE Nexus" at "https://artifacts.wavesenterprise.com/repository/we-releases",
  Resolver.bintrayRepo("ethereum", "maven"),
  Resolver.bintrayRepo("dnvriend", "maven"),
  Resolver.sbtPluginRepo("releases")
)

javaOptions in run ++= Seq(
  "-XX:+IgnoreUnrecognizedVMOptions"
)

//Test / fork := true
//Test / javaOptions ++= Seq(
//  "-XX:+IgnoreUnrecognizedVMOptions",
//  "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
//  "-Dnode.waves-crypto=true"
//)
//Test / parallelExecution := true

val aopMerge: MergeStrategy = new MergeStrategy {
  val name = "aopMerge"

  import scala.xml._
  import scala.xml.dtd._

  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
    val dt              = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
    val file            = MergeStrategy.createMergeTarget(tempDir, path)
    val xmls: Seq[Elem] = files.map(XML.loadFile)
    val aspectsChildren: Seq[Node] =
      xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
    val weaverChildren: Seq[Node] =
      xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
    val options: String = xmls
      .map(x => (x \\ "aspectj" \ "weaver" \ "@options").text)
      .mkString(" ")
      .trim
    val weaverAttr =
      if (options.isEmpty) Null
      else new UnprefixedAttribute("options", options, Null)
    val aspects =
      new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
    val weaver =
      new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
    val aspectj =
      new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
    XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
    IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
    Right(Seq(file -> path))
  }
}

lazy val excludedCryptoProJars =
  taskKey[Keys.Classpath]("CryptoPro libs excluded from assembly task")
excludedCryptoProJars in ThisBuild := (unmanagedJars in (moduleCrypto, Compile)).value

inTask(assembly)(
  Seq(
    test := {},
    assemblyJarName := coreJarName.value,
    assemblyExcludedJars := excludedCryptoProJars.value,
    assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") =>
        MergeStrategy.concat
      case PathList("META-INF", "aop.xml") => aopMerge
      case PathList("com", "google", "thirdparty", xs @ _*) =>
        MergeStrategy.first
      case PathList("com", "kenai", xs @ _*)             => MergeStrategy.first
      case PathList("javax", "ws", xs @ _*)              => MergeStrategy.first
      case PathList("jersey", "repackaged", xs @ _*)     => MergeStrategy.first
      case PathList("jnr", xs @ _*)                      => MergeStrategy.first
      case PathList("org", "aopalliance", xs @ _*)       => MergeStrategy.first
      case PathList("org", "jvnet", xs @ _*)             => MergeStrategy.first
      case PathList("com", "sun", "activation", xs @ _*) => MergeStrategy.last
      case PathList("javax", "activation", xs @ _*)      => MergeStrategy.last
      case PathList("jakarta", "activation", xs @ _*)    => MergeStrategy.last
      case path if path.endsWith("module-info.class")    => MergeStrategy.discard
      case "META-INF/maven/com.kohlschutter.junixsocket/junixsocket-native-common/pom.properties" =>
        MergeStrategy.first
      case PathList("com", "google", "protobuf", xs @ _*) => MergeStrategy.first
      case other                                          => (assemblyMergeStrategy in assembly).value(other)
    }
  ))

inConfig(Compile)(
  Seq(
    mainClass := Some("com.wavesplatform.Application"),
    publishArtifact in packageDoc := false,
    publishArtifact in packageSrc := false,
    sourceGenerators += coreVersionSource
  ))

//inConfig(Test)(
//  Seq(
//    logBuffered := false,
//    parallelExecution := true,
//    testListeners := Seq.empty,
//    testOptions += Tests.Argument("-oIDOF", "-u", "target/test-reports"),
//    testOptions += Tests.Setup({ _ =>
//      sys.props("sbt-testing") = "true"
//    })
//  ))

// The bash scripts classpath only needs the fat jar
scriptClasspath := Seq((assemblyJarName in assembly).value)

commands += Command.command("packageAll") { state =>
  "clean" ::
    "assembly" ::
//    "debian:packageBin" ::
    state
}

inConfig(Linux)(
  Seq(
    maintainer := "wavesenterprise.com",
    packageSummary := "WE core",
    packageDescription := "WE core"
  ))

//bashScriptExtraDefines += s"""addJava "-Dnode.directory=/var/lib/${normalizedName.value}""""
//
//val linuxScriptPattern = "bin/(.+)".r
//val batScriptPattern   = "bin/([^.]+)\\.bat".r

//inConfig(Universal)(
//  Seq(
//    mappings += (baseDirectory.value / s"node-example.conf" -> "doc/we.conf.sample"),
//    mappings := {
//      val fatJar = (assembly in Compile).value
//      val universalMappings = mappings.value.map {
//        case (file, batScriptPattern(script)) =>
//          (file, s"bin/$script.bat")
//        case (file, linuxScriptPattern(script)) =>
//          (file, s"bin/$script")
//        case other => other
//      }
//
//      // Filter all jar dependencies because they already exists in the fat jar
//      val filtered = universalMappings filter {
//        case (_, name) => !name.endsWith(".jar")
//      }
//
//      filtered :+ (fatJar -> ("lib/" + fatJar.getName))
//    },
//    javaOptions ++= Seq(
//      // -J prefix is required by the bash script
//      "-J-server",
//      // JVM memory tuning for 2g ram
//      "-J-Xms128m",
//      "-J-Xmx2g",
//      "-J-XX:+ExitOnOutOfMemoryError",
//      // Java 9 support
//      "-J-XX:+IgnoreUnrecognizedVMOptions",
//      // from https://groups.google.com/d/msg/akka-user/9s4Yl7aEz3E/zfxmdc0cGQAJ
//      "-J-XX:+UseG1GC",
//      "-J-XX:+UseNUMA",
//      "-J-XX:+AlwaysPreTouch",
//      // probably can't use these with jstack and others tools
//      "-J-XX:+PerfDisableSharedMem",
//      "-J-XX:+ParallelRefProcEnabled",
//      "-J-XX:+UseStringDeduplication"
//    )
//  ))

//val packageSource = Def.setting {
//  sourceDirectory.value / "package"
//}

//val upstartScript = Def.task {
//  val src = packageSource.value / "upstart.conf"
//  val dest = (target in Debian).value / "upstart" / s"${packageName.value}.conf"
//  val result = TemplateWriter.generateScript(src.toURI.toURL,
//    linuxScriptReplacements.value)
//  IO.write(dest, result)
//  dest
//}

//linuxPackageMappings ++= Seq(
//  (upstartScript.value, s"/etc/init/${packageName.value}.conf")
//).map(packageMapping(_).withConfig().withPerms("644"))
//
//linuxScriptReplacements += "detect-loader" ->
//  """is_systemd() {
//    |    which systemctl >/dev/null 2>&1 && \
//    |    systemctl | grep -- -\.mount >/dev/null 2>&1
//    |}
//    |is_upstart() {
//    |    /sbin/init --version | grep upstart >/dev/null 2>&1
//    |}
//    |""".stripMargin

//inConfig(Debian)(
//  Seq(
//    linuxStartScriptTemplate := (packageSource.value / "systemd.service").toURI.toURL,
//    debianPackageDependencies += "java11-runtime-headless",
//    serviceAutostart := false,
//    maintainerScripts := maintainerScriptsFromDirectory(
//      packageSource.value / "debian",
//      Seq("preinst", "postinst", "postrm", "prerm"))
//  ))

// https://stackoverflow.com/a/48592704/4050580
def allProjects: List[ProjectReference] =
  ReflectUtilities.allVals[Project](this).values.toList map { p =>
    p: ProjectReference
  }

addCommandAlias(
  "checkPR",
  """;
    |set scalacOptions in ThisBuild ++= Seq("-Xfatal-warnings");
    |Global / checkPRRaw;
    |set scalacOptions in ThisBuild -= "-Xfatal-warnings";
  """.stripMargin
)
lazy val checkPRRaw = taskKey[Unit]("Build a project and run unit tests")
checkPRRaw in Global := {
  try {
    clean
      .all(ScopeFilter(inProjects(allProjects: _*), inConfigurations(Compile)))
      .value
  } finally {
    test
      .all(ScopeFilter(inProjects(langJVM, core), inConfigurations(Test)))
      .value
    (langJS / Compile / fastOptJS).value
//    compile
//      .all(ScopeFilter(inProjects(generator), inConfigurations(Test)))
//      .value
  }
}

lazy val lang =
  crossProject(JSPlatform, JVMPlatform)
    .withoutSuffixFor(JVMPlatform)
    .settings(
      version := "1.0.0",
      // the following line forces scala version across all dependencies
      scalaModuleInfo ~= (_.map(_.withOverrideScalaVersion(true))),
      test in assembly := {},
      addCompilerPlugin(Dependencies.kindProjector),
      libraryDependencies ++=
        Dependencies.catsCore ++
          Dependencies.fp ++
          Dependencies.scalacheck ++
          Dependencies.scorex ++
          Dependencies.scalatest ++
          Dependencies.scalactic ++
          Dependencies.monix.value ++
          Dependencies.scodec.value ++
          Dependencies.fastparse.value,
      resolvers += Resolver.bintrayIvyRepo("portable-scala", "sbt-plugins"),
      resolvers += Resolver.sbtPluginRepo("releases")
    )
    .jsSettings(
      scalaJSLinkerConfig ~= {
        _.withModuleKind(ModuleKind.CommonJSModule)
      }
    )
    .jvmSettings(
      publishMavenStyle := true,
      credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
      name := "RIDE Compiler",
      normalizedName := "lang",
      description := "The RIDE smart contract language compiler",
      homepage := Some(url("https://docs.wavesplatform.com/en/technical-details/waves-contracts-language-description/maven-compiler-package.html")),
      licenses := Seq(("MIT", url("https://github.com/wavesplatform/Waves/blob/master/LICENSE"))),
      organization := "com.wavesplatform",
      organizationName := "Waves Platform",
      organizationHomepage := Some(url("https://wavesplatform.com")),
      scmInfo := Some(ScmInfo(url("https://github.com/wavesplatform/Waves"), "git@github.com:wavesplatform/Waves.git", None)),
      developers := List(Developer("petermz", "Peter Zhelezniakov", "peterz@rambler.ru", url("https://wavesplatform.com"))),
      libraryDependencies ++= Seq(
        "org.scala-js"                      %% "scalajs-stubs" % "1.0.0-RC1" % "provided",
        "com.github.spullara.mustache.java" % "compiler"       % "0.9.5"
      ) ++ Dependencies.logging
        .map(_ % "test") // scrypto logs an error if a signature verification was failed
    )

lazy val langJS  = lang.js
lazy val langJVM = lang.jvm.dependsOn(moduleCrypto).dependsOn(utils)

lazy val utils = (project in file("utils"))
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.pureConfig,
      Dependencies.serialization,
      Dependencies.monix.value,
      Dependencies.logging,
      Dependencies.catsCore,
      Dependencies.scorex
    ).flatten
  )

lazy val models = (project in file("models"))
  .enablePlugins(TxSchemePlugin)
  .dependsOn(moduleCrypto)
  .dependsOn(langJVM)
  .dependsOn(grpcProtobuf)
  .dependsOn(transactionProtobuf)
  .settings(
    Compile / unmanagedSourceDirectories += sourceManaged.value / "main" / "com" / "wavesplatform" / "models",
    libraryDependencies ++= Seq(
      Dependencies.pureConfig,
      Dependencies.catsCore,
      Dependencies.monix.value,
      Dependencies.protobuf,
      Dependencies.scodec.value,
      Dependencies.serialization
    ).flatten
  )

lazy val moduleCrypto: Project = project
  .dependsOn(utils)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.scorex,
      Dependencies.catsCore,
      Dependencies.logging,
      Dependencies.enumeratum,
      Dependencies.bouncyCastle,
      Dependencies.serialization
    ).flatten
  )

lazy val grpcProtobuf = (project in file("grpc-protobuf"))
  .enablePlugins(AkkaGrpcPlugin)
  .dependsOn(transactionProtobuf)
  .settings(
    scalacOptions += "-Yresolve-term-conflict:object",
    libraryDependencies ++= Dependencies.protobuf
  )

lazy val transactionProtobuf = (project in file("transaction-protobuf"))
  .enablePlugins(TxSchemeProtoPlugin)
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    scalacOptions += "-Yresolve-term-conflict:object",
    libraryDependencies ++= Dependencies.protobuf
  )

lazy val typeScriptZipTask = taskKey[File]("archive-typescript")

lazy val typeScriptZipSetting: Def.Setting[Task[File]] = typeScriptZipTask := {
  val tsDirectory: File = new sbt.File("./transactions-factory")
  val zipName           = s"we-transaction_typescript_${version.value}.zip"
  IO.delete(tsDirectory * "we-transaction_typescript_*.zip" get)

  val filesToZip: Set[(File, String)] = Set(
    tsDirectory \ "src" \ "Transactions.ts",
    tsDirectory \ "src" \ "constants.ts",
    tsDirectory \ "tests" \ "Tests.ts"
  ).flatMap(_.get())
    .map { file =>
      (file, tsDirectory.toURI.relativize(file.toURI).getPath)
    }

  val outputZip = new sbt.File(tsDirectory, zipName)
  println(s"Typescript archive has been created: '${outputZip.getAbsolutePath}'")
  IO.zip(filesToZip, outputZip)

  outputZip
}

lazy val protobufZipTask = taskKey[File]("archive-protobuf")

lazy val protobufZipSetting: Def.Setting[Task[File]] = protobufZipTask := {
  (transactionProtobuf / Compile / compile).value
  (grpcProtobuf / Compile / compile).value

  val txProtoDir   = (sourceDirectory in transactionProtobuf).value / "main" / "protobuf"
  val grpcProtoDir = (sourceDirectory in grpcProtobuf).value / "main" / "protobuf"
  val zipName      = s"we_protobuf_${version.value}.zip"

  IO.delete(new sbt.File("./target/") * "we_protobuf_*.zip" get)

  val filesToZip = for {
    pathFinder    <- Set(txProtoDir, grpcProtoDir)
    directory     <- pathFinder.get()
    fileWithPaths <- Path.selectSubpaths(directory, "*.proto" | "*.md")
  } yield fileWithPaths

  val outputZip = new sbt.File("./target/", zipName)

  IO.zip(filesToZip, outputZip)
  println(s"Protobuf archive has been created: '${outputZip.getAbsolutePath}'")
  outputZip
}

lazy val transactionTypeScript = (project in file("transactions-factory"))
  .enablePlugins(TxSchemeTypeScriptPlugin)
  .settings(
    scalacOptions += "-Yresolve-term-conflict:object"
  )

lazy val protobufArchives = (project in file("we-transaction-protobuf"))
  .dependsOn(grpcProtobuf)
  .settings(
    name := "we-transaction-protobuf",
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishTo := weReleasesRepo,
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    protobufZipSetting,
    artifact in (Compile, protobufZipTask) ~= ((art: Artifact) => art.withType("zip").withExtension("zip")),
    addArtifact(artifact in (Compile, protobufZipTask), protobufZipTask)
  )

lazy val typescriptArchives = (project in file("we-transaction-typescript"))
  .dependsOn(transactionTypeScript)
  .settings(
    name := "we-transaction-typescript",
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishTo := weReleasesRepo,
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishMavenStyle := false,
    typeScriptZipSetting,
    artifact ~= ((art: Artifact) => art.withType("zip").withExtension("zip")),
    addArtifact(artifact, typeScriptZipTask)
  )

addCommandAlias(
  "compileAll",
  "; cleanAll; checkJCSP; compile"
)

val weReleasesRepo = Some("Sonatype Nexus Repository Manager" at "https://artifacts.wavesenterprise.com/repository/we-releases")

//val debianPackage = taskKey[File]("debian-package")
//debianPackage := (baseDirectory in Compile).value / "target" / s"${name.value}_${version.value}_all.deb"

lazy val core = project
  .in(file("."))
  .dependsOn(models)
  .dependsOn(langJVM)
  .dependsOn(transactionTypeScript)
  .settings(
    addCompilerPlugin(Dependencies.kindProjector),
    libraryDependencies ++=
      Dependencies.network ++
        Dependencies.db ++
        Dependencies.http ++
        Dependencies.serialization ++
        Dependencies.testKit.map(_ % "test") ++
        Dependencies.logging ++
        Dependencies.metrics ++
        Dependencies.fp ++
        Dependencies.meta ++
        Dependencies.ficus ++
        Dependencies.scorex ++
        Dependencies.commonsNet ++
        Dependencies.commonsLang ++
        Dependencies.monix.value ++
        Dependencies.docker ++
        Dependencies.enumeratum ++
        Dependencies.dbDependencies ++
        Dependencies.awsDependencies ++
        Dependencies.javaplot ++
        Dependencies.pureConfig ++
        Dependencies.licenseDependencies,
    dependencyOverrides ++= Seq(
      Dependencies.AkkaHTTP
    ) ++ Dependencies.fastparse.value
  )
  .settings(
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishTo := weReleasesRepo,
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    addArtifact(artifact in (Compile, assembly), assembly)
//    artifact in(Compile, debianPackage) ~= ((art: Artifact) =>
//      art.withType("deb").withExtension("deb")),
//    addArtifact(artifact in(Compile, debianPackage), debianPackage),
//    publish := (publish dependsOn (packageBin in Debian) dependsOn (assembly in Compile)).value
  )

//lazy val it = project
//  .dependsOn(core % "test->test; compile->compile")
//  .disablePlugins(plugins.JUnitXmlReportPlugin)
//  .settings(
//    dependencyOverrides ++= Dependencies.fastparse.value
//  )

lazy val javaHomeProguardOption = Def.task[String] {
  (for {
    versionStr <- sys.props
      .get("java.version")
      .toRight("failed to get system property java.version")
    javaHome <- sys.props
      .get("java.home")
      .toRight("failed to get system property java.home")
    version <- "^(\\d+).*".r
      .findFirstMatchIn(versionStr)
      .map(_.group(1))
      .toRight(s"java.version system property has wrong format: '$versionStr'")
  } yield {
    if (version.toInt > 8)
      s"-libraryjars $javaHome/jmods/java.base.jmod"
    else ""
  }) match {
    case Right(path) => path
    case Left(error) => sys.error(error)
  }
}

lazy val extLibrariesProguardExclusions = Def.task[Seq[String]] {
  libraryDependencies.value
    .map(_.organization.toLowerCase)
    .distinct
    .filterNot(org => org.startsWith("com.wavesenterprise") && org.startsWith("com.wavesplatform"))
    .flatMap { org =>
      Seq(
        s"-keep class $org.** { *; }",
        s"-keep interface $org.** { *; }",
        s"-keep enum $org.** { *; }"
      )
    }
}

//lazy val generator = project
//  .dependsOn(core)
//  .dependsOn(core % "test->test; compile->compile")
//  .enablePlugins(SbtProguard)
//  .settings(
//    Global / cancelable := true,
//    fork in run := true,
//    fork in Test := true,
//    connectInput in run := true,
//    libraryDependencies += "com.github.scopt" %% "scopt" % "3.6.0",
//    assemblyJarName in assembly := s"generators-${version.value}.jar",
//    mainClass in assembly := Some(
//      "com.wavesplatform.generator.GeneratorLauncher"),
//    testOptions in Test += Tests.Argument("-oIDOF",
//      "-u",
//      "target/generator/test-reports"),
//    javaOptions in Test ++= Seq(
//      "-XX:+IgnoreUnrecognizedVMOptions",
//      "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
//      "-Dnode.waves-crypto=true"
//    ),
//    assemblyExcludedJars in assembly := excludedCryptoProJars.value,
//    assemblyMergeStrategy in assembly := {
//      case PathList("META-INF", "io.netty.versions.properties") =>
//        MergeStrategy.concat
//      case PathList("META-INF", "aop.xml") => MergeStrategy.discard
//      case sign if sign.endsWith(".CP") => MergeStrategy.discard
//      case PathList("com", "google", "thirdparty", xs@_*) =>
//        MergeStrategy.discard
//      case PathList("com", "kenai", xs@_*) => MergeStrategy.discard
//      case PathList("javax", "ws", xs@_*) => MergeStrategy.discard
//      case PathList("jersey", "repackaged", xs@_*) => MergeStrategy.discard
//      case PathList("jnr", xs@_*) => MergeStrategy.discard
//      case PathList("org", "aopalliance", xs@_*) => MergeStrategy.discard
//      case PathList("org", "jvnet", xs@_*) => MergeStrategy.discard
//      case PathList("com", "sun", "activation", xs@_*) =>
//        MergeStrategy.discard
//      case PathList("javax", "activation", xs@_*) => MergeStrategy.discard
//      case PathList("jakarta", "activation", xs@_*) => MergeStrategy.discard
//      case "application.conf" => MergeStrategy.concat
//      case path if path.endsWith("module-info.class") => MergeStrategy.discard
//      case PathList("com", "google", "protobuf", xs@_*) => MergeStrategy.first
//      case other => (assemblyMergeStrategy in assembly).value(other)
//    },
//    proguardInputs in Proguard := Seq((assemblyOutputPath in assembly).value),
//    proguardOutputs in Proguard := Seq(
//      (proguardDirectory in Proguard).value / (assemblyJarName in assembly).value),
//    proguardOptions in Proguard ++= Seq(
//      "-dontnote",
//      "-dontwarn",
//      "-dontoptimize",
//      "-dontusemixedcaseclassnames",
//      javaHomeProguardOption.value,
//      // to keep logback
//      "-keep public class org.slf4j.** { *; }",
//      "-keep public class ch.** { *; }",
//      "-keep class org.bouncycastle.** { *; }",
//      "-keep class scala.** { *; }",
//      "-keep interface scala.** { *; }",
//      "-keep enum scala.** { *; }",
//      "-keep class akka.actor.** { *; }",
//      "-keep interface akka.actor.** { *; }",
//      "-keep enum akka.actor.** { *; }",
//      """-keepnames public class * {
//        |public static ** classTag();
//        |}""".stripMargin,
//      """-keepclasseswithmembers public class * {
//        | public static void main(java.lang.String[]);
//        |}""".stripMargin,
//      """-keepclassmembers class * {
//        |** MODULE$;
//        |}""".stripMargin,
//      """-keepclasseswithmembernames,includedescriptorclasses class * {
//        |native <methods>;
//        |}""".stripMargin,
//      """-keepclassmembers,allowoptimization enum * {
//        |public static **[] values();
//        |public static ** valueOf(java.lang.String);
//        |}""".stripMargin,
//      "-keepattributes SourceFile,LineNumberTable"
//    ) ++ extLibrariesProguardExclusions.value,
//    javaOptions in(Proguard, proguard) := Seq("-Xmx2G"),
//    proguardOptions in Proguard += ProguardOptions.keepMain(
//      "com.wavesplatform.generator.GeneratorLauncher"),
//    proguardInputFilter in Proguard := { file =>
//      None
//    },
//    proguardVersion in Proguard := "6.2.2",
//    (proguard in Proguard) := ((proguard in Proguard) dependsOn assembly).value
//  )
//  .settings(
//    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
//    publishTo := weReleasesRepo,
//    publishArtifact in(Compile, packageSrc) := false,
//    publishArtifact in(Compile, packageBin) := false,
//    publishArtifact in(Compile, packageDoc) := false,
//    publishArtifact in(Proguard, proguard) := false,
//    addArtifact(artifact in(Compile, assembly), assembly)
//  )

//lazy val apiTests = (project in file("api-testing"))
//  .dependsOn(core)

//lazy val transactionsSigner = (project in file("transactions-signer"))
//  .dependsOn(core)
//  .settings(
//    name := "transactions-signer",
//    mainClass := Some("com.wavesplatform.TxSignerApplication"),
//    libraryDependencies ++= Dependencies.console,
//    testOptions in Test += Tests
//      .Argument("-oIDOF", "-u", "target/transactions-signer/test-reports")
//  )
//  .settings(
//    assemblyExcludedJars in assembly := excludedCryptoProJars.value,
//    assemblyMergeStrategy in assembly := {
//      case PathList("META-INF", "io.netty.versions.properties") =>
//        MergeStrategy.concat
//      case PathList("META-INF", "aop.xml") => MergeStrategy.discard
//      case PathList("META-INF", "mailcap.default") => MergeStrategy.discard
//      case PathList("META-INF", "mimetypes.default") => MergeStrategy.discard
//      case sign if sign.endsWith(".CP") => MergeStrategy.discard
//      case PathList("com", "google", "thirdparty", xs@_*) =>
//        MergeStrategy.discard
//      case PathList("com", "kenai", xs@_*) => MergeStrategy.discard
//      case PathList("javax", "ws", xs@_*) => MergeStrategy.discard
//      case PathList("jersey", "repackaged", xs@_*) => MergeStrategy.discard
//      case PathList("jnr", xs@_*) => MergeStrategy.discard
//      case PathList("org", "aopalliance", xs@_*) => MergeStrategy.discard
//      case PathList("org", "jvnet", xs@_*) => MergeStrategy.discard
//      case PathList("javax", "activation", xs@_*) => MergeStrategy.discard
//      case PathList("com", "sun", "activation", xs@_*) =>
//        MergeStrategy.discard
//      case "application.conf" => MergeStrategy.concat
//      case path if path.endsWith("module-info.class") => MergeStrategy.discard
//      case "META-INF/maven/com.kohlschutter.junixsocket/junixsocket-native-common/pom.properties" =>
//        MergeStrategy.first
//      case PathList("com", "google", "protobuf", xs@_*) => MergeStrategy.first
//      case other => (assemblyMergeStrategy in assembly).value(other)
//    }
//  )
//  .settings(
//    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
//    publishTo := weReleasesRepo,
//    publishArtifact in(Compile, packageSrc) := false,
//    publishArtifact in(Compile, packageBin) := false,
//    publishArtifact in(Compile, packageDoc) := false,
//    addArtifact(artifact in(Compile, assembly), assembly)
//  )

/* ********************************************************* */

/**
  * Some stuff to handle moving licensees' Public Keys before the build or the tests
  */
//lazy val targetPubkeysDir =
//  Def.setting((resourceDirectory in(core, Compile)).value / "licensees")
//lazy val targetTestPubkeysDir =
//  Def.setting((resourceDirectory in(core, Test)).value / "licensees")
//val pubKeysSourceDir =
//  Def.setting((baseDirectory in(core, Compile)).value / "licenseesPubKeys")

// matches RSA public key files
//val pubFileNameFilter = NameFilter.fnToNameFilter(_.endsWith(".pub"))
//
//val prodKeysDir = Def.setting(pubKeysSourceDir.value / "prod")
//val testKeysDir = Def.setting(pubKeysSourceDir.value / "test")
//val testingKeys =
//  Def.taskDyn(Def.task(IO.listFiles(testKeysDir.value, pubFileNameFilter)))
//val productionKeys =
//  Def.task(IO.listFiles(prodKeysDir.value, pubFileNameFilter))
//
//lazy val cleanLicensePubKeysDir = Def.task[Unit] {
//  printMessage(s"Clearing ${targetPubkeysDir.value}")
//  IO.delete(IO.listFiles(targetPubkeysDir.value, pubFileNameFilter))
//  printMessage(s"Clearing ${targetTestPubkeysDir.value}")
//  IO.delete(IO.listFiles(targetTestPubkeysDir.value, pubFileNameFilter))
//}

/**
  * Copies RSA public keys that are used for license checks
  * For regular builds both production and testing keys are used
  * For release versions (for a clean "v1.2.3"-type version) only the production key is copied
  */
//val releaseVersionPattern = """\d+\.\d+\.\d+"""
//
//lazy val moveLicensePubKeys = Def
//  .task[Unit] {
//    val log = streams.value.log
//
//    cleanLicensePubKeysDir.value
//
//    log.info(s"Current version is v${version.value}")
//
//    val usedTestingKeys: Array[File] = Def.taskDyn {
//      val isNotProduction = !version.value.matches(releaseVersionPattern)
//      if (isNotProduction)
//        testingKeys
//      else
//        Def.task(Array.empty[File])
//    }.value
//
//    val pubKeyFilesToCopy: Map[String, Array[File]] =
//      Map("production" -> productionKeys.value, "testing" -> usedTestingKeys)
//
//    val showFiles = pubKeyFilesToCopy
//      .map {
//        case (keysType, keyFiles) if keyFiles.nonEmpty =>
//          s"$keysType:\n" + keyFiles.map(f => s"\t${f.getName}").mkString("\n")
//        case _ => ""
//      }
//      .mkString("\n")
//    log.info("Copying RSA public keys for license checks:")
//    log.info(showFiles)
//
//    val targetDir = targetPubkeysDir.value
//
//    pubKeyFilesToCopy.values.flatten.foreach { keyFile =>
//      IO.copyFile(keyFile, targetDir / keyFile.getName)
//    }
//  }
//
//lazy val moveTestingPubKeys = Def.task[Unit] {
//  val log = streams.value.log
//  log.info("Moving testing public keys for unit-tests")
//
//  cleanLicensePubKeysDir.value
//
//  val targetDir = targetTestPubkeysDir.value
//
//  // copy testing keys to tests resources dir
//  IO.listFiles(testKeysDir.value, pubFileNameFilter).foreach { keyFile =>
//    log.info("\t" + keyFile.getName)
//    IO.copyFile(keyFile, targetDir / keyFile.getName)
//  }
//}

/* ********************************************************* */

// makes moveLicensePubKeys task execute before assembly
//(assembly in core) := ((assembly in core) dependsOn moveLicensePubKeys).value

// it's a hack, suggested in [[https://stackoverflow.com/questions/32906818/how-can-i-make-test-and-testonly-tasks-both-depend-on-a-new-task]]
// `testLoader` appears to be a common Task for `test` and `testOnly`
//(testLoader in(core, Test)) := ((testLoader in(core, Test)) dependsOn moveTestingPubKeys).value

/* ********************************************************* */

def printMessage(msg: String): Unit = {
  println(" " + ">" * 3 + " " + msg)
}

def printMessageTask(msg: String) = Def.task {
  printMessage(msg)
}

/* ********************************************************* */

/**
  * Some convenient assembly machinery:
  * task "prepareJars": compiles and assembles node and generator, puts resulting fat jars to cleaned 'jars' dir
  * task "cleanJars": deletes 'jars' directory
  */
lazy val jarsDir = settingKey[File]("Jars output directory")

jarsDir := (baseDirectory in (core, Compile)).value / "jars"

lazy val movePrepareJarsArtifacts = Def.task[Unit] {
  val nodeJarName = (assemblyJarName in (core, assembly)).value
  val nodeJar     = (target in (core, Compile)).value / nodeJarName

//  val generatorJarName = (assemblyJarName in(generator, assembly)).value
//  val generatorJar = (target in(generator, Compile)).value / generatorJarName

//  val generatorConfigs = (resourceDirectory in(generator, Compile)).value
//    .listFiles()
//    .filter(_.getName.endsWith(".conf"))

  IO.delete(jarsDir.value)
  IO.createDirectory(jarsDir.value)

//  generatorConfigs.foreach { configFile =>
//    val fileName = configFile.getName.split(File.separator).last
//    IO.copyFile(configFile, jarsDir.value / (fileName + ".sample"))
//  }

  IO.copyFile(nodeJar, jarsDir.value / nodeJarName)
//  IO.copyFile(generatorJar, jarsDir.value / generatorJarName)
}

/* ********************************************************* */

lazy val cleanJars = taskKey[Unit]("Delete jars directory")

cleanJars := IO.delete(jarsDir.value)

/* ********************************************************* */

lazy val prepareJars = taskKey[Unit]("Package everything")

prepareJars := Def
  .sequential(
    printMessageTask("Pre-stage: check JCSP installed in JRE"),
    checkJCSP,
    printMessageTask("Stage 1: compile node"),
    compile in (core, Compile),
//    printMessageTask("Stage 2: compile generator"),
//    compile in(generator, Compile),
    printMessageTask("Stage 3: assembly node jar"),
    assembly in (core, assembly),
//    printMessageTask("Stage 4: assembly generator jar"),
//    assembly in(generator, assembly),
    printMessageTask("Stage 5: create jars dir and move assembled jars there"),
    movePrepareJarsArtifacts,
    printMessageTask("All done!")
  )
  .value

/* ********************************************************* */

lazy val buildAllDir =
  settingKey[File]("Directory for artifacts, generated with 'buildAll' task")
buildAllDir := (baseDirectory in (core, Compile)).value / "artifacts"

lazy val moveNodeFatJar = Def.task[Unit] {
  val nodeJar = (target in (core, Compile)).value / coreJarName.value
  IO.copyFile(nodeJar, buildAllDir.value / coreJarName.value)
}

//lazy val moveNodeDeb = Def.task[Unit] {
//  val nodeDeb = (target in(core, Compile)).value / nodeDebName.value
//  IO.copyFile(nodeDeb, buildAllDir.value / nodeDebName.value)
//}

//lazy val moveGeneratorFatJar = Def.task[Unit] {
//  val generatorJar = (target in(generator, Compile)).value / generatorJarName.value
//  IO.copyFile(generatorJar, buildAllDir.value / generatorJarName.value)
//}

//lazy val moveGeneratorObfuscatedJar = Def.task[Unit] {
//  val obfuscatedJar = (proguardOutputs in(generator, Proguard)).value.head
//  IO.copyFile(obfuscatedJar, buildAllDir.value / generatorJarName.value)
//}

lazy val recreateBuildAllDir = Def.task[Unit] {
  IO.delete(buildAllDir.value)
  IO.createDirectory(buildAllDir.value)
}

/* ********************************************************* */

lazy val cleanProtobufManagedDir = Def.task[Unit] {
  IO.delete((sourceDirectory in transactionProtobuf).value / "main" / "protobuf" / "managed")
}

lazy val cleanTypeScript = Def.task[Unit] {
  IO.delete((baseDirectory in transactionTypeScript).value / "node_modules")
  IO.delete((baseDirectory in transactionTypeScript).value * "we-transaction_typescript_*.zip" get)
}

/**
  * More machinery:
  * * cleanAll - cleans all sub-projects;
  * * buildAll - builds node (to fat jar and deb-package) and generator (just fat jar)
  */
lazy val cleanAll = taskKey[Unit]("Clean all sub-projects")

cleanAll := Def
  .sequential(
//    cleanLicensePubKeysDir,
    printMessageTask("Clean grpcProtobuf"),
    clean in grpcProtobuf,
    printMessageTask("Clean transactionProtobuf"),
    clean in transactionProtobuf,
    cleanProtobufManagedDir,
    printMessageTask("Clean transactionTypeScript"),
    clean in transactionTypeScript,
    cleanTypeScript,
    printMessageTask("Clean node"),
    clean in core,
//    printMessageTask("Clean generator"),
//    clean in generator,
    printMessageTask("Clean moduleCrypto"),
    clean in moduleCrypto,
    printMessageTask("Clean models"),
    clean in models,
    printMessageTask("Clean utils"),
    clean in utils
//    printMessageTask("Clean transactionsSigner"),
//    clean in transactionsSigner,
//    printMessageTask("Clean it"),
//    clean in it
  )
  .value

lazy val buildAll = taskKey[Unit]("Build node (jar + deb) and generator (jar)")

buildAll := Def
  .sequential(
    printMessageTask("Check JCSP installation"),
    checkJCSP,
    printMessageTask("Recreate /artifacts dir"),
    recreateBuildAllDir,
    printMessageTask("Assembly node (fat jar)"),
    assembly,
    printMessageTask("Move node fat jar to /artifacts"),
    moveNodeFatJar
//    printMessageTask("Package node (deb)"),
//    packageBin in Debian,
//    printMessageTask("Move node deb to /artifacts"),
//    moveNodeDeb,
//    printMessageTask("Assembly generator (fat jar)"),
//    assembly in generator,
//    printMessageTask("Move generator fat jar to /artifacts"),
//    moveGeneratorFatJar
  )
  .value

/* ********************************************************* */

//lazy val errLogger = ProcessLogger(_ => (), err => sys.error(err))
//
//lazy val dockerBuild = taskKey[Unit]("Runs Docker build")
//dockerBuild := Def.task(
//  Process(s"docker build -t wavesenterprise/node:v${version.value} .")
//    .!(errLogger))
//
//lazy val dockerLifeCheck = taskKey[Unit]("Checks if Docker is running")
//dockerLifeCheck := Def.task {
//  Process("docker info").!(errLogger)
//}
//
lazy val release = taskKey[Unit]("Prepares artifacts for release. Since 1.1.1, it assembles and obfuscates generator jar")

release := Def
  .sequential(
//    printMessageTask("Checking is Docker running"),
//    dockerLifeCheck,
    printMessageTask("Recreating /artifacts dir"),
    recreateBuildAllDir,
    printMessageTask("Assembling fat jar"),
    assembly,
//    printMessageTask("Building Docker image"),
//    dockerBuild,
//    printMessageTask("Assembling generator"),
//    assembly in generator,
//    printMessageTask("Obfuscating assembled generator"),
//    proguard in(generator, Proguard),
//    printMessageTask("Moving obfuscated generator jar to /artifacts"),
//    moveGeneratorObfuscatedJar,
    printMessageTask("Done")
  )
  .value
