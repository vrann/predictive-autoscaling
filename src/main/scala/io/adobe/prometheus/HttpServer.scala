package io.adobe.prometheus

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object HttpServer {
  sealed trait Message

  def apply(host: String, port: Int, routes: Route, system: ActorSystem[_]): Behavior[Message] = {
    implicit val executionContext: ExecutionContextExecutor =
      system.executionContext
    implicit val materializer: actor.ActorSystem = system.classicSystem

    val futureBinding = Http().newServerAt(host, port).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("HTTP server started at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(exception) =>
        system.log
          .error("Failed to bind HTTP endpoint, terminating system", exception)
        system.terminate()
    }

    Behaviors.empty
  }
}
