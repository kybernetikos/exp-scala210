import scala.concurrent.ExecutionContext

case class ExecutionRequest(code: Runnable)

class ActorWithExecutionContext extends ExtensibleActor {

  @volatile
  private var insideActor = false

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

  implicit val executionContext = ActorExecutionContext

  final override def receive = {
    case ExecutionRequest(code) =>
      try {
        insideActor = true
        println(s"ActorWithExecutionContext.receive : executing self message $code")
        code.run()
      } finally {
        insideActor = false
      }
    case message =>
      try {
        insideActor = true
        super.receive(message)
      } finally {
        insideActor = false
      }
  }

}
