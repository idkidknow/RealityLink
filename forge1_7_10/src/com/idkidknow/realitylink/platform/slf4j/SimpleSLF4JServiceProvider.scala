package com.idkidknow.realitylink.platform.slf4j

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class SimpleSLF4JServiceProvider extends SLF4JServiceProvider {
  override def getMarkerFactory(): IMarkerFactory = Log4jMarkerFactory

  override def initialize(): Unit = {}

  override def getRequestedApiVersion(): String = "2.0.99"

  override def getLoggerFactory(): ILoggerFactory = Log4jLoggerFactory

  override def getMDCAdapter(): MDCAdapter = Log4jMDCAdapter
}
