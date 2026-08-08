package com.idkidknow.realitylink.server

import com.comcast.ip4s.Host
import com.comcast.ip4s.Port

final case class RealityLinkServerConfig(
    host: Host,
    port: Port,
)
