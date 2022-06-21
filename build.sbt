import com.typesafe.sbt.SbtGit.GitKeys.gitUncommittedChanges
import com.typesafe.sbt.git.JGit
import com.wavesenterprise.grpc.GrpcApiVersionGenerator
import com.wavesenterprise.transaction.generator.{TxSchemePlugin, TxSchemeProtoPlugin, TxSchemeTypeScriptPlugin}
import org.eclipse.jgit.submodule.SubmoduleWalk.IgnoreSubmoduleMode
import sbt.Keys.{credentials, sourceGenerators, _}
import sbt.internal.inc.ReflectUtilities
import sbt.librarymanagement.ivy.IvyDependencyResolution
import sbt.{Compile, Credentials, Def, Path, _}
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import java.io.File

enablePlugins(GitVersioning)
scalafmtOnCompile in ThisBuild := true

Global / cancelable := true
Global / onChangedBuildSource := ReloadOnSourceChanges

fork in run := true

name := "we-core"

inThisBuild(
  Seq(
    scalaVersion := "2.12.10",
    organization := "com.wavesenterprise",
    organizationName := "wavesenterprise",
    organizationHomepage := Some(url("https://wavesenterprise.com")),
    description := "Library for Waves Enterprise blockchain platform",
    homepage := Some(url("https://github.com/waves-enterprise/we-core")),
    pomIncludeRepository := { _ =>
      false
    },
    publishMavenStyle := true,
    licenses ++= Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/waves-enterprise/we-core"),
        "scm:git@github.com:waves-enterprise/we-core.git"
      )
    ),
    developers ++= List(
      Developer("vaan", "Vadim Anufriev", "vanufriev@web3tech.ru", url("https://vaan.io/")),
      Developer("squadgazzz", "Ilia Zhavoronkov", "izhavoronkov89@list.ru", url("https://www.linkedin.com/in/ilya-zhavoronkov/")),
      Developer("sathembite", "Anton Mazur", "sathembite@gmail.com", url("https://github.com/AntonMazur")),
      Developer("kantefier", "Kirill Nebogin", "kinebogin@gmail.com", url("https://github.com/kantefier")),
      Developer("gamzaliev", "Ruslan Gamzaliev", "gamzaliev.ruslan.94@gmail.com", url("https://github.com/gamzaliev")),
      Developer("1estart", "Artemiy Pospelov", "artemiywaves@gmail.com", url("https://github.com/1estart")),
    ),
    crossPaths := false,
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Ywarn-unused:-implicits",
      "-Xlint",
      "-Ypartial-unification"
    )
  )
)

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

    case _ if isSnapshotVersion.value =>
      val localCredentialsFile = Path.userHome / ".sbt" / ".credentials"
      println(s"Going to use ${localCredentialsFile.getAbsolutePath} as credentials for artifacts.wavesenterprise.com")
      Credentials(localCredentialsFile)

    case _ =>
      val localCredentialsFile = Path.userHome / ".sbt" / "sonatype_credentials"
      println(s"Going to use ${localCredentialsFile.getAbsolutePath} as credentials for s01.oss.sonatype.org")
      Credentials(localCredentialsFile)
  }
}

excludeDependencies ++= Seq(
  // workaround for https://github.com/sbt/sbt/issues/3618
  // include "jakarta.ws.rs" % "jakarta.ws.rs-api" instead
  ExclusionRule("javax.ws.rs", "javax.ws.rs-api")
)

libraryDependencies += "jakarta.ws.rs" % "jakarta.ws.rs-api" % "2.1.5"

val coreVersionSource = Def.task {
  // WARNING!!!
  // Please, update the fallback version every major and minor releases.
  // This version is used then building from sources without Git repository
  // In case of not updating the version cores build from headless sources will fail to connect to newer versions
  val FallbackVersion = (1, 0, 0)

  val coreVersionFile: File = (sourceManaged in Compile).value / "com" / "wavesenterprise" / "CoreVersion.scala"
  val versionExtractor      = """(\d+)\.(\d+)\.(\d+).*""".r
  val (major, minor, patch) = version.value match {
    case versionExtractor(ma, mi, pa) => (ma.toInt, mi.toInt, pa.toInt)
    case _                            => FallbackVersion
  }

  IO.write(
    coreVersionFile,
    s"""package com.wavesenterprise
       |
       |object CoreVersion {
       |  val VersionString = "${version.value}"
       |  val VersionTuple = ($major, $minor, $patch)
       |}
       |""".stripMargin
  )

  Seq(coreVersionFile)
}

normalizedName := s"${name.value}"

lazy val branchName = Def.setting[String](sys.env.getOrElse("CI_COMMIT_REF_NAME", git.gitCurrentBranch.value))

/**
  * The version is generated by the first possible method in the following order:
  *   Release version – {Tag}[-DIRTY]. When the tag corresponding to the version pattern is set on the last commit;
  *   Snapshot version – {Tag}-{Commits-ahead}-{Branch-name}-{Commit-hash}[-DIRTY]-SNAPSHOT. When the `git describe --tags` is worked;
  *   Fallback version – {Current-date}-SNAPSHOT.
  */
ThisBuild / version := {
  val uncommittedChangesSuffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, Some("DIRTY"))
  val snapshotSuffix           = "SNAPSHOT"

  val releaseVersion = git.releaseVersion(git.gitCurrentTags.value, git.gitTagToVersionNumber.value, uncommittedChangesSuffix)

  lazy val snapshotVersion = git.gitDescribedVersion.value.map { described =>
    val commitHashLength                          = 7
    val (tagVersionWithoutCommitHash, commitHash) = described.splitAt(described.length - commitHashLength)
    val tagVersionWithCommitsAhead                = tagVersionWithoutCommitHash.dropRight(2)
    val branchSuffix                              = branchName.value
    s"$tagVersionWithCommitsAhead-$branchSuffix-$commitHash$uncommittedChangesSuffix-$snapshotSuffix"
  }

  lazy val fallbackVersion = s"${git.formattedDateVersion.value}-$snapshotSuffix"

  (releaseVersion orElse snapshotVersion) getOrElse fallbackVersion
}

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

scalaModuleInfo ~= (_.map(_.withOverrideScalaVersion(true)))

// for sbt plugins sources resolving
updateSbtClassifiers / dependencyResolution := IvyDependencyResolution((updateSbtClassifiers / ivyConfiguration).value)
resolvers ++= Seq(
  "WE Nexus" at "https://artifacts.wavesenterprise.com/repository/we-releases",
  "WE Nexus Snapshot" at "https://artifacts.wavesenterprise.com/repository/we-snapshots",
  Resolver.bintrayRepo("ethereum", "maven"),
  Resolver.bintrayRepo("dnvriend", "maven"),
  Resolver.sbtPluginRepo("releases"),
  Resolver.sbtPluginRepo("snapshots")
)

javaOptions in run ++= Seq(
  "-XX:+IgnoreUnrecognizedVMOptions"
)

Test / fork := true
Test / javaOptions ++= Seq(
  "-XX:+IgnoreUnrecognizedVMOptions",
  "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
  "-Dnode.crypto.type=WAVES"
)
Test / parallelExecution := true

lazy val excludedCryptoProJars =
  taskKey[Keys.Classpath]("CryptoPro libs excluded from assembly task")
excludedCryptoProJars in ThisBuild := (unmanagedJars in (crypto, Compile)).value

inConfig(Compile)(
  Seq(
    publishArtifact in packageDoc := !isSnapshotVersion.value,
    publishArtifact in packageSrc := true,
    publishArtifact in packageBin := true,
    sourceGenerators += coreVersionSource
  ))

inConfig(Test)(
  Seq(
    logBuffered := false,
    parallelExecution := true,
    testListeners := Seq.empty,
    testOptions += Tests.Argument("-oIDOF", "-u", "target/test-reports"),
    testOptions += Tests.Setup({ _ =>
      sys.props("sbt-testing") = "true"
    }),
    publishArtifact in packageDoc := false,
    publishArtifact in packageSrc := false,
    publishArtifact in packageBin := false
  ))

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
    clean.all(ScopeFilter(inProjects(allProjects: _*), inConfigurations(Compile))).value
  } finally {
    test.all(ScopeFilter(inProjects(langJVM, core), inConfigurations(Test))).value
    (langJS / Compile / fastOptJS).value
  }
}

lazy val lang =
  crossProject(JSPlatform, JVMPlatform)
    .withoutSuffixFor(JVMPlatform)
    .settings(
      // the following line forces scala version across all dependencies
      scalaModuleInfo ~= (_.map(_.withOverrideScalaVersion(true))),
      addCompilerPlugin(Dependencies.kindProjector),
      libraryDependencies ++=
        Dependencies.catsCore ++
          Dependencies.fp ++
          Dependencies.scalacheck ++
          Dependencies.scorex ++
          Dependencies.scalatest ++
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
      name := "RIDE Compiler",
      normalizedName := "lang",
      description := "The RIDE smart contract language compiler",
      homepage := Some(url("https://docs.wavesplatform.com/en/technical-details/waves-contracts-language-description/maven-compiler-package.html")),
      licenses := Seq(("MIT", url("https://github.com/wavesplatform/Waves/blob/master/LICENSE"))),
      libraryDependencies ++= Seq(
        "org.scala-js"                      %% "scalajs-stubs" % "1.0.0-RC1" % "provided",
        "com.github.spullara.mustache.java" % "compiler"       % "0.9.5"
      ) ++ Dependencies.logging
        .map(_ % "test") // scrypto logs an error if a signature verification was failed
    )

lazy val langJS = lang.js
lazy val langJVM = lang.jvm
  .dependsOn(crypto)
  .dependsOn(utils)
  .aggregate(crypto, utils)
  .settings(
    moduleName := "we-lang",
    publishTo := publishingRepo.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishArtifact in (Compile, packageDoc) := !isSnapshotVersion.value,
  )

lazy val utils = (project in file("utils"))
  .settings(
    moduleName := "we-utils",
    libraryDependencies ++= Seq(
      Dependencies.pureConfig,
      Dependencies.serialization,
      Dependencies.monix.value,
      Dependencies.logging,
      Dependencies.catsCore,
      Dependencies.scorex
    ).flatten,
    publishTo := publishingRepo.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishArtifact in (Compile, packageDoc) := !isSnapshotVersion.value
  )

lazy val models = (project in file("models"))
  .enablePlugins(TxSchemePlugin)
  .dependsOn(crypto)
  .dependsOn(langJVM)
  .dependsOn(grpcProtobuf)
  .dependsOn(transactionProtobuf)
  .aggregate(crypto, langJVM, grpcProtobuf, transactionProtobuf)
  .settings(
    moduleName := "we-models",
    Compile / unmanagedSourceDirectories += sourceManaged.value / "main" / "com" / "wavesenterprise" / "models",
    libraryDependencies ++= Seq(
      Dependencies.pureConfig,
      Dependencies.catsCore,
      Dependencies.monix.value,
      Dependencies.protobuf,
      Dependencies.scodec.value,
      Dependencies.serialization,
      Dependencies.commonsNet
    ).flatten,
    publishTo := publishingRepo.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishArtifact in (Compile, packageDoc) := !isSnapshotVersion.value,
  )

lazy val crypto: Project = project
  .dependsOn(utils)
  .aggregate(utils)
  .settings(
    moduleName := "we-crypto",
    libraryDependencies ++= Seq(
      Dependencies.scorex,
      Dependencies.catsCore,
      Dependencies.logging,
      Dependencies.enumeratum,
      Dependencies.bouncyCastle,
      Dependencies.serialization,
      Dependencies.scalaCollectionCompat,
      Dependencies.reflections
    ).flatten,
    publishTo := publishingRepo.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishArtifact in (Compile, packageDoc) := !isSnapshotVersion.value
  )

lazy val testCore: Project = (project in file("test-core"))
  .dependsOn(models)
  .aggregate(models)
  .settings(
    moduleName := "we-test-core",
    libraryDependencies ++= Seq(Dependencies.commonsLang, Dependencies.netty).flatten,
    scalacOptions += "-Yresolve-term-conflict:object",
    publishTo := publishingRepo.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishArtifact in (Compile, packageDoc) := !isSnapshotVersion.value,
  )

val grpcProtobufVersion = "1.5"

lazy val grpcProtobuf = (project in file("grpc-protobuf"))
  .enablePlugins(AkkaGrpcPlugin)
  .enablePlugins(GrpcApiVersionGenerator)
  .dependsOn(transactionProtobuf)
  .aggregate(transactionProtobuf)
  .settings(
    moduleName := "we-grpc-protobuf",
    version := {
      val suffix     = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, Some("DIRTY"))
      val branchName = git.gitCurrentBranch.value
      if (isSnapshotVersion.value) {
        s"$grpcProtobufVersion-$branchName-SNAPSHOT"
      } else {
        grpcProtobufVersion + suffix
      }
    },
    scalacOptions += "-Yresolve-term-conflict:object",
    libraryDependencies ++= Dependencies.protobuf,
    publishTo := publishingRepo.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishArtifact in (Compile, packageDoc) := !isSnapshotVersion.value,
  )

lazy val transactionProtobuf = (project in file("transaction-protobuf"))
  .enablePlugins(TxSchemeProtoPlugin)
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    moduleName := "we-transaction-protobuf",
    scalacOptions += "-Yresolve-term-conflict:object",
    libraryDependencies ++= Dependencies.protobuf,
    publishTo := publishingRepo.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishArtifact in (Compile, packageDoc) := !isSnapshotVersion.value,
  )

lazy val typeScriptZipTask = taskKey[File]("archive-typescript")

lazy val typeScriptZipSetting: Def.Setting[Task[File]] = typeScriptZipTask := {
  val tsDirectory: File = new sbt.File("./transactions-factory")
  val zipName           = s"we-transaction_typescript_${(version in transactionProtobuf).value}.zip"
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
    name := "we-transaction-protobuf-archive",
    publishTo := publishingRepo.value,
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
    name := "we-transaction-typescript-archive",
    publishTo := publishingRepo.value,
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
  "; cleanAll; transactionProtobuf/compile; compile; test:compile"
)

lazy val isSnapshotVersion: Def.Initialize[Boolean] = version(_ endsWith "-SNAPSHOT")

lazy val publishingRepo: Def.Initialize[Some[Resolver]] = isSnapshotVersion {
  case true =>
    Some("Sonatype Nexus Snapshots Repository Manager" at "https://artifacts.wavesenterprise.com/repository/we-snapshots")
  case _ =>
    Some("releases" at "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
}

lazy val core = project
  .in(file("."))
  .dependsOn(models)
  .dependsOn(testCore % "test->test")
  .aggregate(models)
  .settings(
    moduleName := "we-core",
    addCompilerPlugin(Dependencies.kindProjector),
    libraryDependencies ++= Seq(
      Dependencies.scalatest,
      Dependencies.scalacheck,
      Dependencies.commonsLang,
      Dependencies.docker,
      Dependencies.asyncHttpClient,
      Dependencies.netty
    ).flatten
  )
  .settings(
    publishTo := publishingRepo.value
  )

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

/* ********************************************************* */

def printMessage(msg: String): Unit = {
  println(" " + ">" * 3 + " " + msg)
}

def printMessageTask(msg: String) = Def.task {
  printMessage(msg)
}

/* ********************************************************* */

lazy val cleanProtobufManagedDirs = Def.sequential(
  Def.task[Unit] {
    IO.delete((sourceDirectory in transactionProtobuf).value / "main" / "protobuf" / "managed")
  },
  Def.task[Unit] {
    IO.delete((sourceDirectory in grpcProtobuf).value / "main" / "protobuf" / "managed")
  }
)

lazy val cleanTypeScript = Def.task[Unit] {
  IO.delete((baseDirectory in transactionTypeScript).value / "node_modules")
  IO.delete((baseDirectory in transactionTypeScript).value * "we-transaction_typescript_*.zip" get)
}

/**
  * More machinery:
  * * cleanAll - cleans all sub-projects;
  * * buildAll - builds core (to jar)
  */
lazy val cleanAll = taskKey[Unit]("Clean all sub-projects")

cleanAll := Def
  .sequential(
    printMessageTask("Clean grpcProtobuf"),
    clean in grpcProtobuf,
    printMessageTask("Clean transactionProtobuf"),
    clean in transactionProtobuf,
    cleanProtobufManagedDirs,
    printMessageTask("Clean transactionTypeScript"),
    clean in transactionTypeScript,
    cleanTypeScript,
    printMessageTask("Clean core"),
    clean in core,
    printMessageTask("Clean crypto"),
    clean in crypto,
    printMessageTask("Clean models"),
    clean in models,
    printMessageTask("Clean utils"),
    clean in utils
  )
  .value
