import scala.concurrent.ExecutionContext

case class ExecutionRequest(code: Runnable)

class ActorWithExecutionContext extends ExtensibleActor {

  // This is the executionContext used by subclasses unless they explicitly  override it
  implicit val executionContext = ActorExecutionContext

  @volatile
  private var insideActor = false // this may be accessed from another thread

  // Runs code immediately if already within the actor, otherwise sends a self message
  object ActorExecutionContext extends ExecutionContext {
    override def execute(runnable: Runnable): Unit = {
      if (insideActor) {
        runnable.run()
      } else {
        self ! ExecutionRequest(runnable)
      }
    }
    override def reportFailure(t: Throwable): Unit = ExecutionContext.defaultReporter
  }

  // always sends a self message to execute code
  object DelayedActorExecutionContext extends ExecutionContext {
    override def execute(runnable: Runnable): Unit = {
      self ! ExecutionRequest(runnable)
    }
    override def reportFailure(t: Throwable): Unit = ExecutionContext.defaultReporter
  }

  final override def receive = wrap(enterActor, exitActor) {
    case ExecutionRequest(code) =>
      println(s"ActorWithExecutionContext.receive : executing self message $code")
      code.run()
    case otherMessage =>
      super.receive(otherMessage)
  }

  private def enterActor() = {
    insideActor = true
  }

  private def exitActor() = {
    insideActor = false
  }

  private def wrap[T, U](before:() => Unit, after: () => Unit)(pf: PartialFunction[T, U]): PartialFunction[T, U] = {
    return new PartialFunction[T, U] {
      def apply(value: T): U = {
        var result: Option[U] = None
        if (pf.isDefinedAt(value)) {
          try {
            before()
            result = Some(pf(value))
          } finally {
            after()
          }
        }
        result.get
      }
      def isDefinedAt(value: T) = pf.isDefinedAt(value)
    }
  }

}
