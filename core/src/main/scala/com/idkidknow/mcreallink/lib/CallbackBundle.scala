package com.idkidknow.mcreallink.lib

import cats.Apply
import cats.Semigroup
import cats.effect.Concurrent
import cats.effect.Ref
import cats.effect.implicits.*
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream

trait CallbackBundle[F[_], A, R] {
  def +=(callback: A => F[R]): F[Unit]
  def -=(callback: A => F[R]): F[Unit]
}

object CallbackBundle {
  def apply[F[_]: Concurrent, A](
      cont: (A => F[Unit]) => F[Unit]
  ): F[CallbackBundle[F, A, Unit]] = combineAll[F, A, Unit](())(cont)

  /** Create an empty callback bundle and generate a single function which can
   *  invoke all callbacks. The function generated will be passed to `cont` and
   *  therefore can be used as a single callback.
   */
  def combineAll[F[_]: Concurrent, A, R: Semigroup](default: R)(
      cont: (A => F[R]) => F[Unit]
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
        override def +=(callback: A => F[R]): F[Unit] =
          setRef.update(set => set + callback)
        override def -=(callback: A => F[R]): F[Unit] =
          setRef.update(set => set - callback)
      }
    } yield bundle
  }

  def fromImpure[F[_]: Async, A](
      callbackSetter: (A => Unit) => Unit,
      unsafeRunner: F[Unit] => Unit,
  ): F[CallbackBundle[F, A, Unit]] = CallbackBundle[F, A] {
    cb =>
      Async[F].delay {
        callbackSetter(a => unsafeRunner(cb(a)))
      }
  }

  extension [F[_], A](bundle: CallbackBundle[F, A, Unit]) {
    def registerAsStream(queue: F[Queue[F, A]])(using Concurrent[F]): Stream[F, A] = {
      val r: Resource[F, Stream[F, A]] = for {
        queue <- Resource.eval(queue)
        callback = queue.offer(_)
        acquire = {
          (bundle += callback) *>
            Stream.fromQueueUnterminated[F, A](queue).pure[F]
        }
        release = (bundle -= callback)
        resource <- Resource.make[F, Stream[F, A]](acquire)(_ => release)
      } yield resource
      Stream.resource(r).flatten
    }
  }

  extension [F[_]: Apply, A, R](bundle: CallbackBundle[F, A, R]) {
    def registerRunOnce(callback: A => F[R]): F[Unit] = {
      def newCallback: A => F[R] = { a =>
        (bundle -= newCallback) *> callback(a)
      }
      bundle += newCallback
    }

    def registerAsResource(callback: A => F[R]): Resource[F, Unit] =
      Resource.make(bundle += callback)(_ => bundle -= callback)
  }
}
