package io.adobe

class PrometheusReaderActor {}

import akka.actor.typed.scaladsl.Behaviors._
import akka.actor.typed.{ActorSystem, Behavior}
import prometheus.Remote
import prometheus.Types.LabelMatcher

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

        //        val serialization = SerializationExtension(system)

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
          //          .addAcceptedResponseTypes(Remote.ReadRequest.ResponseType.SAMPLES)п
          .addAcceptedResponseTypes(Remote.ReadRequest.ResponseType.STREAMED_XOR_CHUNKS)
          .build()

        new PrometheusStreamReader(address)
          .stream(prometheusReadRequest) //.map(s => s.runWith(Sink.foreach(println(_))))

//        val compressed: Array[Byte] = Snappy.compress(prometheusReadRequest.toByteArray)
//        implicit val timeout: Timeout = Timeout(5 seconds)
//        implicit val executionContext = system.executionContext
//        val `application/x-protobuf` =
//          MediaType.applicationBinary("x-protobuf", NotCompressible)
//        HttpRequest
//        val req = HttpRequest(method = HttpMethods.POST, uri = Uri(prometheusEndpoint))
//          .withHeaders(`Content-Encoding`(HttpEncoding("snappy")))
//          .withEntity(HttpEntity(ContentType(`application/x-protobuf`), compressed))
//
//        //        val slotUri1 = s"http://localhost:9090/api/v1/query?query=readersRequestsCounter_total"
//        //        val response: Future[HttpResponse] = Http().singleRequest(HttpRequest(HttpMethods.POST, slotUri1))
//
//        val response: Future[HttpResponse] = Http().singleRequest(req)
//
//        response.foreach(res => {
//          res.entity.dataBytes
//            .via(new UvarintFramingStage)
//            .map((frame: ByteString) => {
//              val data = frame.toArray
//              val result: Remote.ChunkedReadResponse = Remote.ChunkedReadResponse.parseFrom(data)
//              println(result)
//              var isAddLength = true
//
//              result.getChunkedSeriesList.forEach(chunkseries => {
//                println("--- series --- ")
//                var b = com.google.protobuf.ByteString.EMPTY
//                chunkseries.getChunksList.forEach(chunk => {
//                  //first two bites are chunk headers
//                  //last 4 bytes are used for crc32
//                  val data = chunk.getData.toByteArray.drop(2).dropRight(3)
//                  data.foreach(a => print(a + ", "))
//                  val xorIterator = new XORChunkIterator(data)
//                  isAddLength = false
//                  while (xorIterator.hasNext) {
//                    xorIterator.next()
//                    val next = (xorIterator.t, xorIterator.v)
//                    println(next)
//                  }
//                })
//              })
//              Future.successful(result)
//            })
//            .run()
//
//        })
        same
    }
}
