package com.kineticdata.akka

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.io.StdIn
import com.kineticdata.akka.web.Router


object WebServer extends App with DiscussionRoutes {
  val systemName = "KDAkkaDemo"
  implicit val system = ActorSystem(systemName)
  val system2 = ActorSystem(systemName)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Prepare the connection to the cluster.
  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)
  Cluster(system2).join(joinAddress)

  // Start our "DB" actors.
  val discussionRegistryActor: ActorRef = system.actorOf(DiscussionRegistryActor.props, "discussionRegistryActor")
  
  // Start our chat stuff.
  val chatRouterActor: ActorRef = system.actorOf(ChatRouter.props, "chatRouter")
  val chatRouter2: ActorRef = system2 .actorOf(ChatRouter.props, "chatRouter")
//  val chatSession1: ActorRef = system.actorOf(ChatSessionActor.props("sesson 1"))
//  val chatSession2: ActorRef = system2.actorOf(ChatSessionActor.props("session 2"))
//  val chatSession3: ActorRef = system2.actorOf(ChatSessionActor.props("session 3"))

  // Define and start our REST service.
  lazy val routes: Route = discussionRoutes
  val serverBindingFuture: Future[ServerBinding] = Http().bindAndHandle(Router.routes, "localhost", 9000)

  // Send a join message to the chat session:
//  chatSession1 ! ChatSessionActor.JoinDiscussion("guid1")
//  chatSession2 ! ChatSessionActor.JoinDiscussion("guid1")
//  chatSession2 ! ChatSessionActor.JoinDiscussion("guid2")
//  chatSession3 ! ChatSessionActor.JoinDiscussion("guid2")

  // Start a schedule to emit messages to the chat session, simulating the socket.
//  system.scheduler.schedule(
//    0 milliseconds,
//    5 seconds,
//    chatRouterActor,
//    ChatRouter.AddChatMessage(chatSession1, "guid1", "chat session 1", "this is an automated message"))

  // Start a schedule to emit messages to the chat session, simulating the socket.
//  system2.scheduler.schedule(
//    0 milliseconds,
//    8 seconds,
//    chatRouter2,
//    ChatRouter.AddChatMessage(chatSession2, "guid1", "chat session 2", "from the OTHER SIIIDEEE"))

  // Start a schedule to emit messages to the chat session, simulating the socket.
//  system2.scheduler.schedule(
//    0 milliseconds,
//    8 seconds,
//    chatRouter2,
//    ChatRouter.AddChatMessage(chatSession3, "guid2", "chat session 3", "can 2 be in 2?"))

  println(s"Server online at http://localhost:9000\nPress ENTER to stop...")
  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done ⇒
      done.failed.map { ex ⇒ log.error(ex, "Failed unbinding") }
      system.terminate()
      system2.terminate()
    }

}
