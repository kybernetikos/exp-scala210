package improbable.core.actor

import akka.actor.Actor

class AnonymousFunctionSubscription[MessageType] {

  private var subscriptions: Set[PartialFunction[MessageType, Unit]] = Set()

  def registerSubscription(function: PartialFunction[MessageType, Unit]): Unit = {
    subscriptions += function
  }

  def deregisterSubscription(function: PartialFunction[MessageType, Unit]): Unit = {
    subscriptions -= function
  }

  def clearSubscriptions(): Unit = {
    subscriptions = Set()
  }

  /**
   * @param message
   * @return `true` if `message` was forwarded to any subscriber.
   */
  def forwardToSubscriptions(message: MessageType): Boolean = {
    var anyRelevantSubscriptions = false

    subscriptions.foreach{
      subscription =>
        if (subscription.isDefinedAt(message)) {
          subscription(message)
          anyRelevantSubscriptions = true
        }
    }
    anyRelevantSubscriptions
  }

  def forwardToSubscriptionsOrThrow(received: MessageType) = {
    val existsInterestedDelegate = forwardToSubscriptions(received)
    if (!existsInterestedDelegate) {
      val error: String = s"Received unexpected message: $received"
      throw new RuntimeException(error)
    }
  }
}


abstract class ExtensibleActor extends Actor {

  private val functionSubscription = new AnonymousFunctionSubscription[Any]()
  private var beforeEachFunctions: Set[PartialFunction[Any, Unit]] = Set()
  private var preStartFunctions: Set[() => Unit] = Set()
  private var postStopFunctions: Set[() => Unit] = Set()
  private var preRestartFunctions: Set[(Throwable, Option[Any]) => Unit] = Set()
  private var postRestartFunctions: Set[Throwable => Unit] = Set()

  protected def addReceive(receive: PartialFunction[Any, Unit]): Unit = {
    functionSubscription.registerSubscription(receive)
  }

  protected def addBeforeEach(func: PartialFunction[Any, Unit]): Unit = {
    beforeEachFunctions += func
  }

  protected def addPreStart(func: => Unit): Unit = {
    preStartFunctions += (() => func)
  }

  protected def addPostStop(func: => Unit): Unit = {
    postStopFunctions += (() => func)
  }

  protected def addPreRestart(func: (Throwable, Option[Any]) => Unit): Unit = {
    preRestartFunctions += func
  }

  protected def addPostRestart(func: Throwable => Unit): Unit = {
    postRestartFunctions += func
  }

  override def receive = {
    case message => {
      beforeEachFunctions.foreach {
        _(message)
      }
      val existsInterestedDelegate = functionSubscription.forwardToSubscriptions(message)
      if (!existsInterestedDelegate) {
        val error = s"Received unexpected message: $message"
        throw new RuntimeException(error)
      }
    }
  }

  override def preStart(): Unit = {
    super.preStart()
    preStartFunctions.foreach(_())
  }

  override def postStop(): Unit = {
    super.postStop()
    postStopFunctions.foreach(_())
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    preRestartFunctions.foreach(_(reason, message))
  }

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    postRestartFunctions.foreach(_(reason))
  }
}
