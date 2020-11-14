package io.adobe

import akka.Done
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaType.NotCompressible
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{`Content-Encoding`, HttpEncoding}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.{ByteString, Timeout}
import org.xerial.snappy.Snappy
import prometheus.Remote

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class PrometheusStreamReader(val address: String) {

  val prometheusEndpoint = s"http://$address/api/v1/read"

  def xorChunkTransformer(frame: ByteString): List[(Long, Double)] = {
    var r: List[(Long, Double)] = List.empty[(Long, Double)]

    val data = frame.toArray
    val result: Remote.ChunkedReadResponse = Remote.ChunkedReadResponse.parseFrom(data)

    result.getChunkedSeriesList.forEach(chunkseries => {
      chunkseries.getChunksList.forEach(chunk => {
        //first two bites are chunk headers
        //last 4 bytes are used for crc32
        val data = chunk.getData.toByteArray.drop(2).dropRight(3)
        data.foreach(a => print(a + ", "))
        val xorIterator = new XORChunkIterator(data)
        while (xorIterator.hasNext) {
          xorIterator.next()
          val next = (xorIterator.t, xorIterator.v)
          r = r :+ next
        }
      })
    })
    r
  }

  def stream(prometheusReadRequest: Remote.ReadRequest)(
    implicit fm: Materializer,
    system: ActorSystem[Nothing],
    executionContext: ExecutionContext): Future[Source[ByteString, Any]] = {

    val compressed: Array[Byte] = Snappy.compress(prometheusReadRequest.toByteArray)
    implicit val timeout: Timeout = Timeout(5 seconds)
    val `application/x-protobuf` =
      MediaType.applicationBinary("x-protobuf", NotCompressible)
    HttpRequest
    val req = HttpRequest(method = HttpMethods.POST, uri = Uri(prometheusEndpoint))
      .withHeaders(`Content-Encoding`(HttpEncoding("snappy")))
      .withEntity(HttpEntity(ContentType(`application/x-protobuf`), compressed))

    val responseFuture: Future[HttpResponse] = Http().singleRequest(req)
    val responseData: Future[Source[ByteString, Any]] = responseFuture.map(a => a.entity.dataBytes)

    val chunkReader = Flow.fromFunction(xorChunkTransformer)
//    val chunkReader = Flow[ByteString, (Int, Double)].map()

//    Source.single(0).map(_ + 1).filter(_ != 0).map(_ - 2).to(Sink.fold(0, (a: Int, b: Int) => a + b))
//    Source.fromIterator()

//    responseFuture.
//    val src: Source[Source[ByteString, Any], NotUsed] =
//      Source.future(responseFuture.map(response => response.entity.dataBytes))
//
//    val res: Future[Source[ByteString, Any]] = responseFuture.map(response => response.entity.dataBytes)

    responseFuture.map
    //.via(new UvarintFramingStage).via(chunkReader)

    responseFuture
      .collect(dd)
      .via(new UvarintFramingStage)
      .via(chunkReader)

  }
}
