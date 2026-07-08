package com.idkidknow.realitylink.platform.api

import java.io.InputStream

trait LanguageAPI extends Types {
  trait LanguageOps {
    def apply(map: String => Option[String]): Language

    /** `.lang` format before 1.13 and `.json` format after 1.13 */
    def parseLanguageFile(stream: InputStream): Option[Map[String, String]]

    /** `"lang"` before 1.13 and `"json"` after 1.13 */
    def languageFileExtension: String
  }

  val Language: LanguageOps
}
