package com.wavesenterprise.utils

object StringUtils {

  object ValidateAsciiAndRussian {
    def findNotValid(s: String): Option[String] = {
      //finding for for all symbols except ascii non-control and  russian letters
      val pattern = raw"[^\x21-\x7Eа-яА-Я]*".r
      val res = pattern
        .findAllMatchIn(s)
        .filterNot(_.matched.isBlank())
        .map(_.group(0))
        .toSeq
        .flatten
        .toSet
        .mkString
      if (res.isEmpty) None else Some(res)
    }
    val keyAndForbiddenSymbols: String => Option[String] =
      s => findNotValid(s).map(notValidChars => s"$s -> $notValidChars")


    def notValidOrRight(list: List[String]): Either[String, Unit] = {
      val findingErrors = list.map(keyAndForbiddenSymbols).flatten.mkString("; ")
      if (findingErrors.isBlank) Right(())
      else Left(findingErrors)
    }


    def notValidOrRight(s: String): Either[String, Unit] = notValidOrRight(List(s))
  }

}
