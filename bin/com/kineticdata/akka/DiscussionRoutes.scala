package com.kineticdata.akka

import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.duration._
import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import scala.concurrent.Future
import com.kineticdata.akka.DiscussionRegistryActor._
import akka.pattern.ask
import akka.util.Timeout

trait DiscussionRoutes extends JsonSupport {
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[DiscussionRoutes])

  def discussionRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val discussionRoutes: Route =
    // /discussions
    pathPrefix("discussions") {
      concat(
        // /discussions
        pathEnd {
          concat(
            // GET /discussions - fetch all discussions
            get {
              // Send a request to the actor that acts as the Service Layer, it returns a Future.
              // Tell it to map the result to a Discussion case class.
              val discussions: Future[Discussions] =
                (discussionRegistryActor ? GetDiscussions).mapTo[Discussions]

              // Complete the request and send the discussions that were returned.
              complete(discussions)
            },

            // POST /discussions - create a new discussion.
            post {
              // Unmarshal the body as a DiscussionRest case class.
              entity(as[DiscussionRest]) { discussion ⇒
                // Call the service layer to create a discussion.
                val discussionCreated: Future[Discussion] =
                  (discussionRegistryActor ? CreateDiscussion(discussion)).mapTo[Discussion]

                // When the request to the actor is successful log that the discussion
                // was created and return the newly created discussion.
                onSuccess(discussionCreated) { d ⇒
                  log.info("Created user[{}]: {}", d.name, d.description)
                  complete(StatusCodes.Created, d)
                }
              }
            }
          )
        },
        // discussions/{guid}
        path(Segment) { guid ⇒
          concat(
            // GET /discussions/{guid}
            get {
              val maybeDiscussion: Future[Option[Discussion]] =
                (discussionRegistryActor ? GetDiscussion(guid)).mapTo[Option[Discussion]]

              rejectEmptyResponse {
                complete(maybeDiscussion)
              }
            }
          )
        }
      )
    }
}
