package com.idkidknow.mcreallink.lib

import cats.effect.kernel.MonadCancel
import cats.effect.kernel.Resource
import cats.syntax.all.*

/** Mark the behavior that leak a `Resource` */
trait Leak[F[_]] {
  def leak[A](r: Resource[F, A]): F[A]
}

object Leak {
  def apply[F[_]: Leak]: Leak[F] = summon

  given fromMonadCancel[F[_]](using MonadCancel[F, Throwable]): Leak[F] =
    new Leak[F] {
      def leak[A](r: Resource[F, A]): F[A] = r.allocated.map { case (a, _) =>
        a
      }
    }
}
