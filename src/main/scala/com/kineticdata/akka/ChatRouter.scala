package com.kineticdata.akka

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

object ChatRouter {
  sealed trait ChatRouterMessages
  final case class AddChatMessage(session: ActorRef, guid: String, from: String, body: String)


  def props: Props = Props[ChatRouter]
}

class ChatRouter extends Actor with ActorLogging {
  import DistributedPubSubMediator.Publish
  import ChatRouter._

  val mediator = DistributedPubSub(context.system).mediator

  def receive: Receive = {
    case msg: AddChatMessage =>
      mediator ! Publish(s"discussion-${msg.guid}", msg)
  }
}