package com.idkidknow.realitylink.platform.slf4j

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.slf4j.IMarkerFactory
import org.slf4j.Marker

object Log4jMarkerFactory extends IMarkerFactory {
  private val logger: Logger =
    LogManager.getLogger(classOf[Log4jMarkerFactory.type])

  override def getMarker(name: String): Marker = Log4jMarker(name)

  override def exists(name: String): Boolean = {
    if (name == null) { // scalafix:ok
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
