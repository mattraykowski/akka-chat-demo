package com.kineticdata.akka

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.kineticdata.akka.events.Events._

object ChatSessionActor {
  sealed trait ChatSessionMessages
  final case class JoinDiscussion(guid: String)
  final case class LeaveDiscussion(guid: String)

  def props(who: String): Props = Props(new ChatSessionActor(who))

  def discussionTopic(guid: String): String = s"discussion-$guid"
}

class ChatSessionActor(who: String) extends Actor with ActorLogging {
  import DistributedPubSubMediator.{ Subscribe, SubscribeAck, Unsubscribe, UnsubscribeAck }
  import ChatSessionActor._

  val mediator = DistributedPubSub(context.system).mediator
  var websocketActor = Actor.noSender
  var discussions: List[String] = List()

  def receive: Receive = {
    // Web Socket based messages
    case WSUserOnline(ws) =>
      websocketActor = ws
      println("user is online.")
    case _: WSUserOffline =>
      discussions.foreach(guid =>
        mediator ! Unsubscribe(discussionTopic(guid), self)
      )

    case WSJoinOutbound(guid) =>
      println("got outbound join message from session")
      mediator ! Subscribe(s"discussion-$guid", self)
    case WSTextOutbound(guid, msg) =>
      println("got outbound text message from session")
      mediator ! Publish(discussionTopic(guid), ChatMessage(guid, msg))
    case ChatMessage(guid, msg) if websocketActor != Actor.noSender =>
      println("rcvd chat message from cluster")
      websocketActor ! WSTextOutbound(guid, msg)
    case _: ChatMessage if websocketActor == Actor.noSender =>
      println("Did not register websocket actor")
    case SubscribeAck(Subscribe(discussionTopic, None, `self`)) =>
      log.info(s"Subscribing to $discussionTopic")
    case UnsubscribeAck(Unsubscribe(discussionTopic, None, `self`)) =>
      log.info(s"Subscribing to $discussionTopic")
    case any =>
      println("Bad message sent to chat session actor: " + any.toString())
  }
}

