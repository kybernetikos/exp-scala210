import akka.actor._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MyActor(destination: ActorRef) extends ActorWithExecutionContext {
  implicit val timeout: akka.util.Timeout = 4 seconds

  override implicit val executionContext = DelayedActorExecutionContext // this results in 3 self messages instead of 1

  addReceive {
    case Echo(msg) =>
      // it's correct that this doesn't get called because the message comes in in response to an ask
      println(s"received message $msg")
    case msg =>

      ExecutionContext.global.execute(new Runnable() {
        // this is to show that if code is executed outside of the actor, e.g. on an entirely new thread as here,
        // it will get pushed onto a self message and execute within the actor. (Because it's using the implict
        // ActorExecutionContext)
        def run(): Unit = {

          akka.pattern.ask(destination, msg)
            .mapTo[Echo[String]]
            .map(_.msg)
            .map(_.toString() + " World!")
            .onSuccess({
              case value =>
                // this gets called as the result of a self message on the actor
                // so it's all running in the right place, and you can modify state safely
                println(s"success : $value")
            })

        }

      })

  }
}

case class Echo[T](msg: T)

class EchoActor extends Actor {
  def receive = {
    case msg =>
      sender ! Echo(msg)
  }
}

object Main {
  def main(args: Array[String]): Unit = {

    val actorSystem = ActorSystem("actors")
    val echoActor = actorSystem.actorOf(Props[EchoActor])
    val myActor = actorSystem.actorOf(Props {
      new MyActor(echoActor)
    })

    myActor ! "hello!"
  }
}