package io.adobe.prometheus

import akka.actor.typed.scaladsl.Behaviors.{empty, setup, supervise}
import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.{Directives, Route}
import io.adobe.prometheus.App.Prediction.TenantProportion
import spray.json._

import scala.collection.immutable

object App {

  object Prediction {
    final case class TenantProportion(tenant: String, proportion: Double)
    final case class Prediction(current: immutable.Seq[TenantProportion], future: immutable.Seq[TenantProportion])
  }

  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val tenantFormat: RootJsonFormat[TenantProportion] = jsonFormat2(Prediction.TenantProportion)
    implicit val prediction: RootJsonFormat[Prediction.Prediction] = jsonFormat2(Prediction.Prediction)
  }

  object RootBehavior {
    def apply: Behavior[Nothing] = setup[Nothing] { context =>
      context.spawn(
        supervise(PrometheusReaderActor.apply)
          .onFailure[Exception](SupervisorStrategy.restart),
        "PrometheusStreamReader")

      context.spawn(HttpServer("0.0.0.0", 8081, PredictionService.route, context.system), "HttpServer")
      empty
    }
  }

  // use it wherever json (un)marshalling is needed
  object PredictionService extends Directives with JsonSupport {

    val route: Route =
      concat(get {
        path("api" / "v2" / "predictions") {
          complete(
            OK,
            Prediction.Prediction(List(TenantProportion("tenant1", 90.0)), List(TenantProportion("tenant1", 90.0))))
        }
      })
  }

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](RootBehavior.apply, "example")
  }

}
