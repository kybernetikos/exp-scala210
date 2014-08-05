import akka.actor._

import scala.concurrent.duration._

case class Echo[T](msg: T)

class EchoActor extends Actor {
  def receive = {
    case msg =>
      sender ! Echo(msg)
  }
}

class MyActor(destination: ActorRef) extends ActorWithFutures {
  implicit val timeout: akka.util.Timeout = 4 seconds

  addReceive {
    case Echo(msg) =>
      println(s"received message $msg")
    case msg =>
      ask(destination, msg) onSuccess {
        case value =>
          println(s"success $value")
      }
  }
}

object HiAkka {
  def main(args: Array[String]): Unit = {

    val actorSystem = ActorSystem("actors")
    val echoActor = actorSystem.actorOf(Props[EchoActor])
    val myActor = actorSystem.actorOf(Props {
      new MyActor(echoActor)
    })

    myActor ! "hello!"
  }
}
