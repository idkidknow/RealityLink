package com.idkidknow.mcreallink.forge1710

import cats.effect.{IO, Sync}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import org.apache.logging.log4j.{LogManager, ThreadContext}

object ScalaEntry {
  def init(): Unit = {
    com.idkidknow.mcreallink.ModInit.entry(MinecraftImpl, SimpleLoggerFactory())
  }
}

class SimpleLoggerFactory extends LoggerFactory[IO] {

  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[IO] = new SelfAwareStructuredLogger[IO] {
    private val logger = LogManager.getLogger(name)

    override def isTraceEnabled: IO[Boolean] = IO(logger.isTraceEnabled)
    override def isDebugEnabled: IO[Boolean] = IO(logger.isDebugEnabled)
    override def isInfoEnabled: IO[Boolean] = IO(logger.isInfoEnabled)
    override def isWarnEnabled: IO[Boolean] = IO(logger.isWarnEnabled)
    override def isErrorEnabled: IO[Boolean] = IO(logger.isErrorEnabled)

    private def contextLog(isEnabled: IO[Boolean], ctx: Map[String, String], logging: () => Unit): IO[Unit] = {
      val ifEnabled: IO[Unit] = IO {
        val backup = ThreadContext.getContext
        for ((k, v) <- ctx) {
          ThreadContext.put(k, v)
        }

        try logging()
        finally {
          if (backup eq null) {
            ThreadContext.clear()
          } else {
            ThreadContext.clear()
            import scala.collection.JavaConversions.*
            for ((k, v) <- backup) {
              ThreadContext.put(k, v)
            }
          }
        }
      }
      isEnabled.ifM(ifEnabled, IO.unit)
    }

    override def trace(t: Throwable)(msg: => String): IO[Unit] =
      isTraceEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.trace(msg, t)), IO.unit)

    override def trace(msg: => String): IO[Unit] =
      isTraceEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.trace(msg)), IO.unit)

    override def trace(ctx: Map[String, String])(msg: => String): IO[Unit] =
      contextLog(isTraceEnabled, ctx, () => logger.trace(msg))

    override def debug(t: Throwable)(msg: => String): IO[Unit] =
      isDebugEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.debug(msg, t)), IO.unit)

    override def debug(msg: => String): IO[Unit] =
      isDebugEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.debug(msg)), IO.unit)

    override def debug(ctx: Map[String, String])(msg: => String): IO[Unit] =
      contextLog(isDebugEnabled, ctx, () => logger.debug(msg))

    override def info(t: Throwable)(msg: => String): IO[Unit] =
      isInfoEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.info(msg, t)), IO.unit)

    override def info(msg: => String): IO[Unit] =
      isInfoEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.info(msg)), IO.unit)

    override def info(ctx: Map[String, String])(msg: => String): IO[Unit] =
      contextLog(isInfoEnabled, ctx, () => logger.info(msg))

    override def warn(t: Throwable)(msg: => String): IO[Unit] =
      isWarnEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.warn(msg, t)), IO.unit)

    override def warn(msg: => String): IO[Unit] =
      isWarnEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.warn(msg)), IO.unit)

    override def warn(ctx: Map[String, String])(msg: => String): IO[Unit] =
      contextLog(isWarnEnabled, ctx, () => logger.warn(msg))

    override def error(t: Throwable)(msg: => String): IO[Unit] =
      isErrorEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.error(msg, t)), IO.unit)

    override def error(msg: => String): IO[Unit] =
      isErrorEnabled
        .ifM(IO.suspend(Sync.Type.Delay)(logger.error(msg)), IO.unit)

    override def error(ctx: Map[String, String])(msg: => String): IO[Unit] =
      contextLog(isErrorEnabled, ctx, () => logger.error(msg))

    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] =
      contextLog(isTraceEnabled, ctx, () => logger.trace(msg, t))

    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] =
      contextLog(isDebugEnabled, ctx, () => logger.debug(msg, t))

    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] =
      contextLog(isInfoEnabled, ctx, () => logger.info(msg, t))

    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] =
      contextLog(isWarnEnabled, ctx, () => logger.warn(msg, t))

    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] =
      contextLog(isErrorEnabled, ctx, () => logger.error(msg, t))
  }

  override def fromName(name: String): IO[SelfAwareStructuredLogger[IO]] = IO {
    getLoggerFromName(name)
  }
}
