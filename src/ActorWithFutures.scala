import akka.actor.ActorRef
import akka.util.Timeout
import sync.{DefaultSyncPromiseFuture, SyncPromiseWriter, SyncFuture, SyncPromiseSystem}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.ClassTag
import scala.util.Try

case class PromiseCompletion[T](id: Int, value: Try[T])

class ActorWithFutures extends ExtensibleActor {
  val promiseSystem = new SyncPromiseSystem()

  def ask(actor: ActorRef, msg: Any)(implicit timeout: Timeout): SyncFuture[Any] = {
    actorFuture(akka.pattern.ask(actor, msg)(timeout))(ExecutionContext.Implicits.global)
  }

  // Turns a scala future into an 'actor' future which executes callbacks as
  // a result of a self message.  The only thing the executor is used for is to send the self message.
  def actorFuture[T](future: Future[T])(implicit executor: ExecutionContext): SyncFuture[T] = {
    val (id, syncFuture) = promiseSystem.next[T]()
    future.onComplete(
      completion =>
        self ! PromiseCompletion(id, completion)
    )(executor)
    syncFuture
  }

  final override def receive = {
    case PromiseCompletion(id, completion) =>
      promiseSystem.complete(id, completion)
    case message =>
      super.receive(message)
  }
}
