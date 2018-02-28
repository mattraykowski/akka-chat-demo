import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.typesafe.config.ConfigFactory

// ChatRouter
class Publisher extends Actor {
  import DistributedPubSubMediator.Publish

  val mediator = DistributedPubSub(context.system).mediator

  def receive: Receive = {
    case in: String => {
      val out: String = in.toUpperCase
      println(s"Received '$in', transformed to '$out'.")
      mediator ! Publish("content", out)
    }
  }
}

// ChatSession
class Subscriber extends Actor with ActorLogging {
  import DistributedPubSubMediator.{ Subscribe, SubscribeAck }

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("content", self)

  def receive: Receive = {
    case s: String =>
      log.info("Got {}", s)
    case SubscribeAck(Subscribe("content", None, `self`)) =>
      log.info("Subscribing")
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val systemName = "PubSub"
    
    // Initialize first system
    println("***************************** INITIALIZING SYSTEM 1 WITH PUBLISHER *****************************")
    val system1 = ActorSystem(systemName)
    val joinAddress = Cluster(system1).selfAddress
    Cluster(system1).join(joinAddress)
    val publisher = system1.actorOf(Props[Publisher], "publisher")

    // Wait 5 seconds and start the 2nd system
    Thread.sleep(5000)
    println("***************************** INITIALIZING SYSTEM 2 WITH SUBSCRIBER *****************************")
    val system2 = ActorSystem(systemName)
    Cluster(system2).join(joinAddress)
    system2.actorOf(Props[Subscriber], "subscriber")

    // Wait 5 seconds and send something.
    Thread.sleep(5000)
    println("***************************** SENDING PUBLISHER MESSAGE *****************************")
    publisher ! "something"
  }
}