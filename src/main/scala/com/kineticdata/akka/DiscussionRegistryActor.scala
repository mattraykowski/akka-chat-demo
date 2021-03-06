package com.kineticdata.akka

import java.util.UUID
import akka.actor.{ Actor, ActorLogging, Props }
import com.kineticdata.akka.DiscussionRegistryActor
import com.kineticdata.akka.ChatModels._

object DiscussionRegistryActor {
  final case class ActionPerformed(description: String)
  final case object GetDiscussions
  final case class CreateDiscussion(discussion: DiscussionRest)
  final case class GetDiscussion(guid: String)

  def props: Props = Props[DiscussionRegistryActor]

  def generateUniqueId = UUID.randomUUID().toString
}

class DiscussionRegistryActor extends Actor with ActorLogging {
  import DiscussionRegistryActor._

  var discussions = Set.empty[Discussion]

  def receive: Receive = {
    case GetDiscussions ⇒
      sender() ! Discussions(discussions.toSeq)
    case CreateDiscussion(discussion) ⇒
      val savedDiscussion = Discussion(generateUniqueId, discussion.name, discussion.description)
      discussions += savedDiscussion
      sender() ! savedDiscussion
    case GetDiscussion(guid) ⇒
      sender() ! discussions.find(_.guid == guid)
  }
}
