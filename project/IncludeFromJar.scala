//import java.io.File
//
//import sbtassembly._
//
//class IncludeFromJar(val jarName: String) extends MergeStrategy {
//
//  val name = "includeFromJar"
//
//  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
//    val includedFiles = files.flatMap { f =>
//      val (source, _, _, isFromJar) = sbtassembly.AssemblyUtils.sourceOfFileForMerge(tempDir, f)
//      if (isFromJar && source.getName == jarName) Some(f -> path) else None
//    }
//    Right(includedFiles)
//  }
//}
