package com.kineticdata.akka.web

import akka.actor.{ ActorSystem, ActorSelection }
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.{ Future }
import akka.pattern.ask
import akka.util.Timeout
import com.kineticdata.akka.ChatModels._
import com.kineticdata.akka.DiscussionRegistryActor

object ApiControllers {
  import DiscussionRegistryActor._
  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  def discussionRegistry(implicit system: ActorSystem): ActorSelection = system.actorSelection("/user/discussionRegistryActor")

  def getDiscussions(implicit system: ActorSystem): Future[Discussions] = {
    (discussionRegistry ? GetDiscussions).mapTo[Discussions]
  }

  def getDiscussion(guid: String)(implicit system: ActorSystem): Future[Option[Discussion]] = {
    (discussionRegistry ? GetDiscussion(guid)).mapTo[Option[Discussion]]
  }

  def createDiscussion(discussion: DiscussionRest)(implicit system: ActorSystem): Future[Discussion] = {
    (discussionRegistry ? CreateDiscussion(discussion)).mapTo[Discussion]
  }
}