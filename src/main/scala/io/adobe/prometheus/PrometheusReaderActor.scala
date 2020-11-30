package io.adobe.prometheus

class PrometheusReaderActor {}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors._
import akka.stream.scaladsl.Sink
import prometheus.Types.LabelMatcher
//import akka.stream.javadsl.Source
import akka.actor.typed.ActorSystem
//import akka.stream.javadsl.Source
import prometheus.Remote

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait Message
case object CalculatePredictions extends Message

object PrometheusReaderActor {

  def apply: Behavior[Message] =
    withTimers[Message] { timers =>
      timers.startTimerAtFixedRate(CalculatePredictions, 10 seconds)
      processPredictions
    }

  def processPredictions: Behavior[Message] =
    receive {
      case (context, CalculatePredictions) =>
        implicit val system: ActorSystem[Nothing] = context.system
        implicit val executionContext: ExecutionContext = context.executionContext

        val address = "localhost:9090"

        val prometheusReadRequest = Remote.ReadRequest
          .newBuilder()
          .addQueries(
            Remote.Query
              .newBuilder()
              .setStartTimestampMs(0)
              .setEndTimestampMs(System.currentTimeMillis)
              .addMatchers(
                LabelMatcher
                  .newBuilder()
                  .setName("__name__")
                  .setType(LabelMatcher.Type.EQ)
                  .setValue("akka_system_active_actors_count"))
              //              .addMatchers(LabelMatcher
              //                .newBuilder()
              //                .setName("name")
              //                .setType(LabelMatcher.Type.EQ)
              //                .setValue("akka.actor.default-dispatcher"))
              //              .addMatchers(LabelMatcher
              //                .newBuilder()
              //                .setName("kubernetes_pod_name")
              //                .setType(LabelMatcher.Type.EQ)
              //                .setValue("rainier-allocator-6d7c8f7c77-24xhv"))
              .setHints(
                (prometheus.Types.ReadHints newBuilder ())
                  .setStartMs(1604980010543L)
                  //                  .setStartMs(System.currentTimeMillis - 600 * 1000)
                  .setEndMs(System.currentTimeMillis)
                  .setStepMs(0)
                  .setRangeMs(300000)
                //                  .setFunc("series")
              ))
          //          .addAcceptedResponseTypes(Remote.ReadRequest.ResponseType.SAMPLES)
          .addAcceptedResponseTypes(Remote.ReadRequest.ResponseType.STREAMED_XOR_CHUNKS)
          .build()

        new PrometheusStreamReader(address)
          .stream(prometheusReadRequest)
          .onComplete(t =>
            t.foreach(s =>
              s.runWith(Sink.foreach(a => {
                println(a)
              }))))
        same
    }
}
