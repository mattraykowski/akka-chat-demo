package com.kineticdata.akka.web

import com.kineticdata.akka.web.Router
import akka.http.scaladsl.testkit.{ ScalatestRouteTest, WSProbe }
import org.scalatest.{ Matchers, FlatSpec }

class WebSocketTest extends FlatSpec with Matchers with ScalatestRouteTest {
  val wsClient = WSProbe()

  WS("/socket", wsClient.flow) ~> Router.routes ~>
    check {
      isWebSocketUpgrade shouldEqual true
    }
}
