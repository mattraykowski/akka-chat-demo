package com.kineticdata.akka

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }

object ChatDiscussion {
  sealed trait ChatDiscussionMessages
  final case class Subscribe(session: ActorRef) extends ChatDiscussionMessages
  final case class Unsubscribe(session: ActorRef) extends ChatDiscussionMessages
  final case class RxChatMessage(session: ActorRef, name: String, body: String) extends ChatDiscussionMessages
  final case class TxChatMessage(name: String, body: String) extends ChatDiscussionMessages

  def props: Props = Props[ChatDiscussion]
}

class ChatDiscussion extends Actor with ActorLogging {
  import ChatDiscussion._

  var subscriptions = Set.empty[ActorRef]

  def receive: Receive = {
    case Subscribe(session) =>
      subscriptions += session
    case Unsubscribe(session) =>
      subscriptions = subscriptions filter { _ != session }
    case RxChatMessage(session, from, body) =>
      for(sub <- subscriptions if sub != session) {
        sub ! TxChatMessage(from, body)
      }
  }
}