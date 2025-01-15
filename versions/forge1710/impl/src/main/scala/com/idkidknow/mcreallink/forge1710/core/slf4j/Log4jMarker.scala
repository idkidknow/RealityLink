package com.idkidknow.mcreallink.forge1710.core.slf4j

import org.apache.logging.log4j.{Logger, LogManager}
import org.apache.logging.log4j.MarkerManager
import org.slf4j.Marker

import java.util
import java.util.concurrent.ConcurrentHashMap

class Log4jMarker private (val name: String) extends Marker {
  private val logger: Logger = LogManager.getLogger(classOf[Log4jMarker.type])

  def marker: org.apache.logging.log4j.Marker = MarkerManager.getMarker(name)

  override def getName: String = name

  override def add(reference: Marker): Unit =
    logger.warn("Marker#add is not supported")

  override def remove(reference: Marker): Boolean = true

  override def hasChildren: Boolean = hasReferences

  override def hasReferences: Boolean = false

  override def iterator(): util.Iterator[Marker] = util.Collections.emptyIterator()

  override def contains(other: Marker): Boolean = contains(other.getName)

  override def contains(name: String): Boolean = false
}

object Log4jMarker {
  private val map: ConcurrentHashMap[String, Log4jMarker] = new ConcurrentHashMap()

  def apply(name: String): Log4jMarker = if (map.containsKey(name)) {
    map.get(name)
  } else {
    val l = new Log4jMarker(name)
    map.put(name, l)
    l
  }

  def exists(name: String): Boolean = map.containsKey(name)
}
