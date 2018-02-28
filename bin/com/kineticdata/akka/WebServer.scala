package com.kineticdata.akka

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.StdIn


object WebServer extends App with DiscussionRoutes {
  implicit val system = ActorSystem("kd-akka-demo")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val discussionRegistryActor: ActorRef = system.actorOf(DiscussionRegistryActor.props, "discussionRegistryActor")

  lazy val routes: Route = discussionRoutes

  val serverBindingFuture: Future[ServerBinding] = Http().bindAndHandle(routes, "localhost", 9000)

  println(s"Server online at http://localhost:9000\nPress ENTER to stop...")
  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done ⇒
      done.failed.map { ex ⇒ log.error(ex, "Failed unbinding") }
      system.terminate()
    }
}
