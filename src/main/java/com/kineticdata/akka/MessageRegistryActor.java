package com.kineticdata.akka;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.ArrayList;
import java.util.List;

public class MessageRegistryActor extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private List<String> messages = new ArrayList<>();

  // Define the messages that can be sent.
  public static final class GetMessages {
    String guid;

    public GetMessages(String guid) {
      this.guid = guid;
    }
  }

  public static final class SendMessages {
      List<String> messages;

      public SendMessages(List<String> messages) {
          this.messages = messages;
      }
  }

  public static Props props() {
    return Props.create(MessageRegistryActor.class);
  }

  @Override
  public void preStart() {
    log.info("MessageRegistryActor started.");
  }

  @Override
  public void postStop() {
    log.info("MessageRegistryActor stopped.");
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(String.class, s -> {
                messages.add(s);
                log.info("Received message to persist: '" + s + "'");
            })
            .match(GetMessages.class, s -> {
                getSender().tell(new SendMessages(this.messages), getSelf());
            })
            .build();
  }
}