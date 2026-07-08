package com.idkidknow.realitylink.platform.api

import fs2.io.file.Path

trait Config {
  def gameRootDirectory: Path
  def configDirectory: Path
  def minecraftVersion: String
}
