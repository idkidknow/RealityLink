package com.idkidknow.mcreallink.utils

import cats.effect.Concurrent
import cats.effect.Ref
import cats.effect.implicits.*
import cats.Semigroup
import cats.syntax.all.*

trait CallbackBundle[F[_], A, R] {
  def +(callback: A => F[R]): F[Unit]
  def -(callback: A => F[R]): F[Unit]
  def clear: F[Unit]
}

object CallbackBundle {
  def apply[F[_]: Concurrent, A](
      cont: (A => F[Unit]) => F[Unit],
  ): F[CallbackBundle[F, A, Unit]] = combineAll[F, A, Unit](())(cont)

  /** Create an empty callback bundle and generate a single function which can
   *  invoke all callbacks. The function generated will be passed to `cont` and
   *  therefore can be used as a single callback.
   */
  def combineAll[F[_]: Concurrent, A, R: Semigroup](default: R)(
      cont: (A => F[R]) => F[Unit],
  ): F[CallbackBundle[F, A, R]] = {
    def unifiedCallback(setRef: Ref[F, Set[A => F[R]]])(a: A): F[R] =
      setRef.get.flatMap { set =>
        set.parUnorderedTraverse { cb => cb(a) }.map { rSet =>
          rSet.foldLeft(default)(Semigroup[R].combine)
        }
      }

    for {
      setRef <- Ref[F].of(Set.empty[A => F[R]])
      _ <- cont(unifiedCallback(setRef))
      bundle = new CallbackBundle[F, A, R] {
        override def +(callback: A => F[R]): F[Unit] =
          setRef.update(set => set + callback)
        override def -(callback: A => F[R]): F[Unit] =
          setRef.update(set => set - callback)
        override def clear: F[Unit] = setRef.set(Set.empty)
      }
    } yield bundle
  }
}
