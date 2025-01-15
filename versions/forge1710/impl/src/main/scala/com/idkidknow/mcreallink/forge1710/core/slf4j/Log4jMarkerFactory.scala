package com.idkidknow.mcreallink.forge1710.core.slf4j

import org.apache.logging.log4j.{Logger, LogManager}
import org.slf4j.{IMarkerFactory, Marker}

object Log4jMarkerFactory extends IMarkerFactory {
  private val logger: Logger = LogManager.getLogger(classOf[Log4jMarkerFactory.type])

  override def getMarker(name: String): Marker = Log4jMarker(name)

  override def exists(name: String): Boolean = {
    if (name == null) {
      false
    } else {
      Log4jMarker.exists(name)
    }
  }

  override def detachMarker(name: String): Boolean = {
    logger.warn("IMarkerFactory#detachMarker is not supported")
    false
  }

  override def getDetachedMarker(name: String): Marker = {
    logger.warn("IMarkerFactory#getDetachedMarker is not supported")
    getMarker(name)
  }
}
