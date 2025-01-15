package com.idkidknow.mcreallink.forge1710.core.slf4j

import org.apache.logging.log4j.{LogManager, Logger}
import org.slf4j.Marker

import java.util.concurrent.ConcurrentHashMap

class Log4jLogger private (val name: String) extends org.slf4j.Logger {
  def logger: Logger = LogManager.getLogger(name)

  override def getName: String = name

  extension (marker: Marker) {
    private def adapted: org.apache.logging.log4j.Marker =
      Log4jMarker(marker.getName).marker
  }

  override def isTraceEnabled: Boolean = logger.isTraceEnabled

  override def trace(msg: String): Unit = logger.trace(msg)

  override def trace(format: String, arg: Any): Unit = logger.trace(format, arg)

  override def trace(format: String, arg1: Any, arg2: Any): Unit = logger.trace(format, arg1, arg2)

  override def trace(format: String, arguments: Any*): Unit = logger.trace(format, arguments)

  override def trace(msg: String, t: Throwable): Unit = logger.trace(msg, t)

  override def isTraceEnabled(marker: Marker): Boolean = logger.isTraceEnabled(marker.adapted)

  override def trace(marker: Marker, msg: String): Unit = logger.trace(marker.adapted, msg)

  override def trace(marker: Marker, format: String, arg: Any): Unit = logger.trace(marker.adapted, format, arg)

  override def trace(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = logger.trace(marker.adapted, format, arg1, arg2)

  override def trace(marker: Marker, format: String, argArray: Any*): Unit = logger.trace(marker.adapted, format, argArray)

  override def trace(marker: Marker, msg: String, t: Throwable): Unit = logger.trace(marker.adapted, msg, t)

  override def isDebugEnabled: Boolean = logger.isDebugEnabled

  override def debug(msg: String): Unit = logger.debug(msg)

  override def debug(format: String, arg: Any): Unit = logger.debug(format, arg)

  override def debug(format: String, arg1: Any, arg2: Any): Unit = logger.debug(format, arg1, arg2)

  override def debug(format: String, arguments: Any*): Unit = logger.debug(format, arguments)

  override def debug(msg: String, t: Throwable): Unit = logger.debug(msg, t)

  override def isDebugEnabled(marker: Marker): Boolean = logger.isDebugEnabled(marker.adapted)

  override def debug(marker: Marker, msg: String): Unit = logger.debug(marker.adapted, msg)

  override def debug(marker: Marker, format: String, arg: Any): Unit = logger.debug(marker.adapted, format, arg)

  override def debug(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = logger.debug(marker.adapted, format, arg1, arg2)

  override def debug(marker: Marker, format: String, arguments: Any*): Unit = logger.debug(marker.adapted, format, arguments)

  override def debug(marker: Marker, msg: String, t: Throwable): Unit = logger.debug(marker.adapted, msg, t)

  override def isInfoEnabled: Boolean = logger.isInfoEnabled

  override def info(msg: String): Unit = logger.info(msg)

  override def info(format: String, arg: Any): Unit = logger.info(format, arg)

  override def info(format: String, arg1: Any, arg2: Any): Unit = logger.info(format, arg1, arg2)

  override def info(format: String, arguments: Any*): Unit = logger.info(format, arguments)

  override def info(msg: String, t: Throwable): Unit = logger.info(msg, t)

  override def isInfoEnabled(marker: Marker): Boolean = logger.isInfoEnabled(marker.adapted)

  override def info(marker: Marker, msg: String): Unit = logger.info(marker.adapted, msg)

  override def info(marker: Marker, format: String, arg: Any): Unit = logger.info(marker.adapted, format, arg)

  override def info(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = logger.info(marker.adapted, format, arg1, arg2)

  override def info(marker: Marker, format: String, arguments: Any*): Unit = logger.info(marker.adapted, format, arguments)

  override def info(marker: Marker, msg: String, t: Throwable): Unit = logger.info(marker.adapted, msg, t)

  override def isWarnEnabled: Boolean = logger.isWarnEnabled

  override def warn(msg: String): Unit = logger.warn(msg)

  override def warn(format: String, arg: Any): Unit = logger.warn(format, arg)

  override def warn(format: String, arguments: Any*): Unit = logger.warn(format, arguments)

  override def warn(format: String, arg1: Any, arg2: Any): Unit = logger.warn(format, arg1, arg2)

  override def warn(msg: String, t: Throwable): Unit = logger.warn(msg, t)

  override def isWarnEnabled(marker: Marker): Boolean = logger.isWarnEnabled(marker.adapted)

  override def warn(marker: Marker, msg: String): Unit = logger.warn(marker.adapted, msg)

  override def warn(marker: Marker, format: String, arg: Any): Unit = logger.warn(marker.adapted, format, arg)

  override def warn(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = logger.warn(marker.adapted, format, arg1, arg2)

  override def warn(marker: Marker, format: String, arguments: Any*): Unit = logger.warn(marker.adapted, format, arguments)

  override def warn(marker: Marker, msg: String, t: Throwable): Unit = logger.warn(marker.adapted, msg, t)

  override def isErrorEnabled: Boolean = logger.isErrorEnabled

  override def error(msg: String): Unit = logger.error(msg)

  override def error(format: String, arg: Any): Unit = logger.error(format, arg)

  override def error(format: String, arg1: Any, arg2: Any): Unit = logger.error(format, arg1, arg2)

  override def error(format: String, arguments: Any*): Unit = logger.error(format, arguments)

  override def error(msg: String, t: Throwable): Unit = logger.error(msg, t)

  override def isErrorEnabled(marker: Marker): Boolean = logger.isErrorEnabled(marker.adapted)

  override def error(marker: Marker, msg: String): Unit = logger.error(marker.adapted, msg)

  override def error(marker: Marker, format: String, arg: Any): Unit = logger.error(marker.adapted, format, arg)

  override def error(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = logger.error(marker.adapted, format, arg1, arg2)

  override def error(marker: Marker, format: String, arguments: Any*): Unit = logger.error(marker.adapted, format, arguments)

  override def error(marker: Marker, msg: String, t: Throwable): Unit = logger.error(marker.adapted, msg, t)
}

object Log4jLogger {
  private val map: ConcurrentHashMap[String, Log4jLogger] = new ConcurrentHashMap()

  def apply(name: String): Log4jLogger = if (map.containsKey(name)) {
    map.get(name)
  } else {
    val l = new Log4jLogger(name)
    map.put(name, l)
    l
  }
}
