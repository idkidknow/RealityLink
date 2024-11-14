package com.idkidknow.mcreallink.utils

import cats.syntax.all.*
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Json

enum TomlToJsonException extends Exception {
  case FailFromReal
  case UnsupportedType
}

/** Not stack-safe. I believe that no one has a super deep .toml file */
@SuppressWarnings(Array("org.wartremover.warts.Recursion"))
def tomlValueToCirce(value: toml.Value): Either[TomlToJsonException, Json] =
  value match {
    case toml.Value.Arr(values) => {
      val jsons = values.map(tomlValueToCirce)
      jsons.sequence.map { jsons =>
        Json.arr(jsons*)
      }
    }
    case toml.Value.Bool(value) =>
      Json.fromBoolean(value).asRight[TomlToJsonException]
    case toml.Value.Num(value) =>
      Json.fromLong(value).asRight[TomlToJsonException]
    case toml.Value.Real(value) =>
      Json.fromDouble(value) match {
        case None        => TomlToJsonException.FailFromReal.asLeft[Json]
        case Some(value) => value.asRight[TomlToJsonException]
      }
    case toml.Value.Str(value) =>
      Json.fromString(value).asRight[TomlToJsonException]
    case toml.Value.Tbl(map) => {
      val mapEither = map.map { (k, v) => (k, tomlValueToCirce(v)) }
      val eitherMap = mapEither.toList.traverse { (k, v) => v.map((k, _)) }
      eitherMap.map { map => Json.obj(map*) }
    }
    case _ => TomlToJsonException.UnsupportedType.asLeft[Json]
  }

enum ParseException extends Exception {
  case Parse(message: String)
  case TomlToJson(e: TomlToJsonException)
  case Decoding(e: DecodingFailure)
}

def decodeToml[A: Decoder](str: String): Either[ParseException, A] = {
  toml.Toml.parse(str) match {
    case Left((_, message)) => ParseException.Parse(message).asLeft[A]
    case Right(value) =>
      tomlValueToCirce(value) match {
        case Left(e) => ParseException.TomlToJson(e).asLeft[A]
        case Right(json) => {
          Decoder[A].decodeJson(json) match {
            case Left(decodingFailure) =>
              ParseException.Decoding(decodingFailure).asLeft[A]
            case Right(value) => value.asRight[ParseException]
          }
        }
      }
  }
}
