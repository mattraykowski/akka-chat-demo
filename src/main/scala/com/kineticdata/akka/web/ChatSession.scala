package com.kineticdata.akka.web

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import com.kineticdata.akka.ChatSessionActor
import com.kineticdata.akka.common.CommonUtils._
import com.kineticdata.akka.events.Events._


class ChatSession(implicit ec: ExecutionContext, system: ActorSystem, materialize: ActorMaterializer) {
  val chatSessionActor = system.actorOf(ChatSessionActor.props(""))

  val source: Source[WSOutboundMessage, ActorRef] = Source.actorRef[WSOutboundMessage](bufferSize = Int.MaxValue, OverflowStrategy.fail)
  def chatSessionHandler: Flow[Message, Strict, ActorRef] = Flow.fromGraph(GraphDSL.create(source) { implicit builder =>
    chatSource =>
      import GraphDSL.Implicits._

      val flowFromWS: FlowShape[Message, WSInboundMessage] = builder.add(
        Flow[Message].collect {
          case tm: TextMessage =>
            tm.textStream.runFold("")(_ + _).map { msg: String =>
              var action: String = ""
              var guid: String = ""
              var message: String = ""

              try {
                println("Rcvd: " + msg)
                val json = Json.parse(msg)
                action = getJsonString(json, "action")
                guid = getJsonString(json, "guid")
                message = getJsonString(json, "message")
              } catch {
                case e: Throwable =>
                  println("Bad things happened.")
              }

              val inbound: WSInboundMessage = action match {
                case "join" =>
                  WSJoinInbound(guid)
                case "sendMessage" =>
                  WSTextInbound(guid, message)
                case _ =>
                  WSTextInbound("", "Bad message, unknown action.")
              }
              inbound
            }
        }.buffer(1024 * 1024, OverflowStrategy.fail).mapAsync(6)(t => t)
      )

      val flowToWS: FlowShape[WSOutboundMessage, Strict] = builder.add(
        Flow[WSOutboundMessage].collect {
          case WSTextOutbound(guid, message) =>
            TextMessage("(" + guid + ") message: " + message)
        }
      )

      val connectedWS: Flow[ActorRef, WSUserOnline, NotUsed] = Flow[ActorRef].map { actor =>
        WSUserOnline(actor)
      }

      // Used to branch
      val broadcastWS: UniformFanOutShape[WSInboundMessage, WSInboundMessage] = builder.add(Broadcast[WSInboundMessage](2))
      // Good messages must have a guid.
      val filterSuccess: FlowShape[WSInboundMessage, WSInboundMessage] = builder.add(Flow[WSInboundMessage].filter(_.guid != ""))
      // Bad messages do not have a guid.
      val filterFailure: FlowShape[WSInboundMessage, WSInboundMessage] = builder.add(Flow[WSInboundMessage].filter(_.guid == ""))

      val flowAccept: FlowShape[WSInboundMessage, WSOutboundMessage] = builder.add(
        Flow[WSInboundMessage].collect {
          case WSTextInbound(guid, msg) =>
            Future(
              WSTextOutbound(guid, msg)
            )
          case WSJoinInbound(guid) =>
            Future(WSJoinOutbound(guid))
        }.buffer(1024 * 1024, OverflowStrategy.fail).mapAsync(6)(t => t)
      )

      val flowReject: FlowShape[WSInboundMessage, WSTextOutbound] = builder.add(
        Flow[WSInboundMessage].map(_ => WSTextOutbound("", "Rejected message"))
      )

      val flowAcceptBack: FlowShape[WSOutboundMessage, WSOutboundMessage] = builder.add(
        Flow[WSOutboundMessage].keepAlive(50.seconds, () => WSTextOutbound("", "Keep Alive"))
      )
      val mergeBackWs: UniformFanInShape[WSOutboundMessage, WSOutboundMessage] = builder.add(Merge[WSOutboundMessage](2))
      val mergeAccept: UniformFanInShape[WSOutboundMessage, WSOutboundMessage] = builder.add(Merge[WSOutboundMessage](2))
      val chatActorSink: Sink[WSOutboundMessage, NotUsed] = Sink.actorRef[WSOutboundMessage](chatSessionActor, WSUserOffline)

      flowFromWS ~> broadcastWS
      broadcastWS ~> filterFailure ~> flowReject
      broadcastWS ~> filterSuccess ~> flowAccept ~> mergeAccept.in(0)

      builder.materializedValue ~> connectedWS ~> mergeAccept.in(1)
      mergeAccept ~> chatActorSink

      chatSource ~> flowAcceptBack ~> mergeBackWs.in(0)
      flowReject ~> mergeBackWs.in(1)
      mergeBackWs ~> flowToWS

      FlowShape(flowFromWS.in, flowToWS.out)
  })
    
}