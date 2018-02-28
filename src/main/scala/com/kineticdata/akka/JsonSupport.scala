package com.kineticdata.akka

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.kineticdata.akka.DiscussionRegistryActor.ActionPerformed
import spray.json.DefaultJsonProtocol
import com.kineticdata.akka.ChatModels._

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit val discussionRestJsonFormat = jsonFormat2(DiscussionRest)
  implicit val discussionJsonFormat = jsonFormat3(Discussion)
  implicit val discussionsJsonFormat = jsonFormat1(Discussions)
  implicit val discussionMessageJsonFormat = jsonFormat3(DiscussionMessage)
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
