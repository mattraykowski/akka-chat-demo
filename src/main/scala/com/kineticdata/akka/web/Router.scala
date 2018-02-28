package com.kineticdata.akka.web

import akka.actor.{ ActorSystem, ActorRef }
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import scala.concurrent.{ ExecutionContext, Future }
import com.kineticdata.akka.JsonSupport
import com.kineticdata.akka.ChatModels._

object Router extends JsonSupport {
  def routes(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Route = {
    routeAssets ~
    routeWebSockets ~
    routeDiscussions
  }

  def routeAssets(implicit ec: ExecutionContext): Route = {
    get {
      pathSingleSlash {
        redirect("chat/", StatusCodes.PermanentRedirect)
      } ~ path("chat") {
        redirect("chat/", StatusCodes.PermanentRedirect)
      } ~ path("chat" / "") {
        getFromFile("www/index.html")
      } ~ path("chat" / "test.js") {
        getFromFile("www/test.js")
      }
    }
  }

  def routeDiscussions(implicit ec: ExecutionContext, system: ActorSystem): Route = {
    pathPrefix("api" / "discussions") {
      pathEnd {
        get {
          val discussions: Future[Discussions] = ApiControllers.getDiscussions
          complete(discussions)
        } ~ 
        post {
          entity(as[DiscussionRest]) { discussion: DiscussionRest ⇒
            val discussionCreated: Future[Discussion] = ApiControllers.createDiscussion(discussion)
            complete(discussionCreated)
          }
        }
      } ~
      path(Segment) { guid: String =>
        get {
          val maybeDiscussion: Future[Option[Discussion]] = ApiControllers.getDiscussion(guid)
          rejectEmptyResponse {
            complete(maybeDiscussion)
          }
        }
      }
    } 
  }

  def routeWebSockets(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Route = {
    get {
      path("socket") {
        val chatSession = new ChatSession()
        handleWebSocketMessages(chatSession.chatSessionHandler)
      }
    }
  }
}