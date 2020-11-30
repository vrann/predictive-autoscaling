package io.adobe.prometheus

import akka.actor.typed.scaladsl.Behaviors.{empty, setup, supervise}
import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}

object App {

  object RootBehavior {
    def apply: Behavior[Nothing] = setup[Nothing] { context =>
      context.spawn(
        supervise(PrometheusReaderActor.apply)
          .onFailure[Exception](SupervisorStrategy.restart),
        "PrometheusStreamReader")
      empty
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](RootBehavior.apply, "example")
  }

}
