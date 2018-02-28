package com.kineticdata.akka

object ChatModels {
  final case class DiscussionRest(name: String, description: String)
  final case class Discussion(guid: String, name: String, description: String)
  final case class Discussions(discussions: Seq[Discussion])
  final case class DiscussionMessage(guid: String, discussion_guid: String, body: String)
}