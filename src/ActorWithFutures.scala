import akka.actor.ActorRef
import akka.util.Timeout
import improbable.core.actor.ExtensibleActor
import sync.{SyncPromiseSystem, SyncFuture}

import scala.util.Try

case class PromiseCompletion[T](id: Int, value: Try[T])

/**
 * Copyright (c) 2014 All Right Reserved, Improbable Worlds Ltd.
 * Date: 05/08/2014
 * Summary: 
 */
class ActorWithFutures extends ExtensibleActor {

  val promiseSystem = new SyncPromiseSystem()

  def ask[T](actor: ActorRef, msg: T)(implicit timeout: Timeout): SyncFuture[T] = {
    val (promiseId, future) = promiseSystem.next()

    akka.pattern.ask(actor, msg)(timeout).onComplete(
      completion => self ! PromiseCompletion(promiseId, completion)
    )(context.dispatcher)

    future
  }

  final override def receive = {
    case PromiseCompletion(id, completion) =>
      promiseSystem.complete(id, completion)
    case message =>
      super.receive(message)
  }
}
