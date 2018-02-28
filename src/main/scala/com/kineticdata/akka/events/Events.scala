package com.kineticdata.akka.events

import akka.actor.ActorRef

object Events {
  // Web Socket messages destined to go to the WS client for a session.
  sealed trait WSOutboundMessage
  case class WSTextOutbound(guid: String, msg: String) extends WSOutboundMessage
  case class WSUserOnline(ws: ActorRef) extends WSOutboundMessage
  case class WSUserOffline(ws: ActorRef) extends WSOutboundMessage
  case class WSJoinOutbound(guid: String) extends WSOutboundMessage

  // Web Socket messages coming from the WS client for a session.
  sealed trait WSInboundMessage {
    val guid: String
  }
  case class WSTextInbound(guid: String, msg: String) extends WSInboundMessage
  case class WSJoinInbound(guid: String) extends WSInboundMessage

  // Web Socket stream events.


  sealed trait ChatMessages
  case class ChatMessage(guid: String, msg: String)
}
