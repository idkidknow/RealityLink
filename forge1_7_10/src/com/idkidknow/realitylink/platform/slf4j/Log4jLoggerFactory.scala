package com.idkidknow.realitylink.platform.slf4j

import org.slf4j.ILoggerFactory
import org.slf4j.Logger

object Log4jLoggerFactory extends ILoggerFactory {
  override def getLogger(name: String): Logger = Log4jLogger(name)
}
