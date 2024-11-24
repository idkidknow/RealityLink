package com.idkidknow.mcreallink.lib.platform

import fs2.Stream

/** [[net.minecraft.locale.Language]] */
type Language = Language.type

trait LanguageClass[P[_], F[_]] {
  def get(language: P[Language], key: String): Option[String]
  def create(map: String => Option[String]): P[Language]
  def classLoaderResourceStream(path: String): Stream[F, Byte]
  def parseLanguageFile(
      stream: Stream[F, Byte],
  ): F[Option[Map[String, String]]]
}

object Language {
  def apply[P[_], F[_]](using inst: LanguageClass[P, F]): LanguageClass[P, F] = inst
}

object LanguageClass {
  given platformLanguageClass[P[_], F[_]](using
      p: Platform[P, F],
  ): LanguageClass[P, F] = p.languageClass
}
