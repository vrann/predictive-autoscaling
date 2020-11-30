package io.adobe.prometheus

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaType.NotCompressible
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{`Content-Encoding`, HttpEncoding}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.{ByteString, Timeout}
import com.google.protobuf.InvalidProtocolBufferException
import org.xerial.snappy.Snappy
import prometheus.Remote

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class PrometheusStreamReader(val address: String) {

  val prometheusEndpoint = s"http://$address/api/v1/read"

  def xorChunkTransformer(frame: ByteString): List[(Long, Double)] = {
    var transformed: List[(Long, Double)] = List.empty[(Long, Double)]

    val data = frame.toArray
    try {
      val result: Remote.ChunkedReadResponse = Remote.ChunkedReadResponse.parseFrom(data)
      result.getChunkedSeriesList.forEach(chunkseries => {
        chunkseries.getChunksList.forEach(chunk => {
          //first two bites are chunk headers
          //last 4 bytes are used for crc32
          val data = chunk.getData.toByteArray.drop(2).dropRight(3)
          val xorIterator = new XORChunkIterator(data)
          while (xorIterator.hasNext) {
            xorIterator.next()
            val next = (xorIterator.t, xorIterator.v)
            transformed = transformed :+ next
          }
        })
      })
    } catch {
      case e: InvalidProtocolBufferException => println(e)
    }
    transformed
  }

  def stream(prometheusReadRequest: Remote.ReadRequest)(
    implicit fm: Materializer,
    system: ActorSystem[Nothing],
    executionContext: ExecutionContext): Future[Source[List[(Long, Double)], Any]] = {

    val compressed: Array[Byte] = Snappy.compress(prometheusReadRequest.toByteArray)
    implicit val timeout: Timeout = Timeout(5 seconds)
    val `application/x-protobuf` =
      MediaType.applicationBinary("x-protobuf", NotCompressible)
    HttpRequest
    val req = HttpRequest(method = HttpMethods.POST, uri = Uri(prometheusEndpoint))
      .withHeaders(`Content-Encoding`(HttpEncoding("snappy")))
      .withEntity(HttpEntity(ContentType(`application/x-protobuf`), compressed))

    val responseFuture: Future[HttpResponse] = Http().singleRequest(req)
    val chunkReader: Flow[ByteString, List[(Long, Double)], NotUsed] = Flow.fromFunction(xorChunkTransformer)
    responseFuture
      .collect {
        case a =>
          a.entity.dataBytes
            .via(new UvarintFramingStage)
            .via(chunkReader)
      }
  }
}
