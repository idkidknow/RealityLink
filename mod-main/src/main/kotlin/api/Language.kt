package com.idkidknow.mcreallink.api

import java.io.InputStream

interface LanguageClass<T> {
    fun T.get(key: String): String?
    fun create(map: (String) -> String?): T
    /** .lang before 1.13 and .json after 1.13 */
    fun parseLanguageFile(input: InputStream, output: (String, String) -> Unit)
}
