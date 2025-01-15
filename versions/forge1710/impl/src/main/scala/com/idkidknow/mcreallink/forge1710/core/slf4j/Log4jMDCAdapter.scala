package com.idkidknow.mcreallink.forge1710.core.slf4j

import org.apache.logging.log4j.{Logger, LogManager, ThreadContext}

import java.util

object Log4jMDCAdapter extends org.slf4j.spi.MDCAdapter {
  private val logger: Logger = LogManager.getLogger(classOf[Log4jMDCAdapter.type])

  override def put(key: String, `val`: String): Unit = ThreadContext.put(key, `val`)

  override def get(key: String): String = ThreadContext.get(key)

  override def remove(key: String): Unit = ThreadContext.remove(key)

  override def clear(): Unit = ThreadContext.clear()

  override def getCopyOfContextMap: util.Map[String, String] = ThreadContext.getContext

  override def setContextMap(contextMap: util.Map[String, String]): Unit = {
    ThreadContext.clear()
    contextMap.forEach((k, v) => ThreadContext.put(k, v))
  }

  override def pushByKey(key: String, value: String): Unit =
    logger.warn("MDCAdapter#pushByKey is not supported")

  override def popByKey(key: String): String = {
    logger.warn("MDCAdapter#popByKey is not supported")
    ""
  }

  override def getCopyOfDequeByKey(key: String): util.Deque[String] = {
    logger.warn("MDCAdapter#getCopyOfDequeByKey is not supported")
    new util.ArrayDeque[String]()
  }

  override def clearDequeByKey(key: String): Unit =
    logger.warn("MDCAdapter#clearDequeByKey is not supported")
}
