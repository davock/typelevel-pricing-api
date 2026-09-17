package com.example.server.json

import cats.effect.Concurrent
import cats.syntax.all.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.http4s.headers.`Content-Type`
import org.http4s.{DecodeResult, EntityDecoder, EntityEncoder, InvalidMessageBodyFailure, MediaType}

/** Generic http4s <-> jsoniter-scala bridge, mirroring `org.http4s.circe.CirceEntityCodec`:
  * given a `JsonValueCodec[A]` (typically derived via `JsonCodecMaker.make` on `A`'s companion),
  * this wires up encoding/decoding for any route without per-type boilerplate.
  */
object JsoniterEntityCodec:

  given jsoniterEntityEncoder[F[_], A](using codec: JsonValueCodec[A]): EntityEncoder[F, A] =
    EntityEncoder
      .byteArrayEncoder[F]
      .contramap[A](writeToArray(_))
      .withContentType(`Content-Type`(MediaType.application.json))

  given jsoniterEntityDecoder[F[_]: Concurrent, A](using codec: JsonValueCodec[A]): EntityDecoder[F, A] =
    EntityDecoder.decodeBy(MediaType.application.json) { msg =>
      DecodeResult(
        msg.body.compile.to(Array).map { bytes =>
          Either
            .catchNonFatal(readFromArray[A](bytes))
            .leftMap(t => InvalidMessageBodyFailure(t.getMessage, Some(t)))
        }
      )
    }
