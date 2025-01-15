package com.idkidknow.mcreallink.forge1710.core.slf4j

import org.slf4j.{ILoggerFactory, Logger}

object Log4jLoggerFactory extends ILoggerFactory {
  override def getLogger(name: String): Logger = Log4jLogger(name)
}
