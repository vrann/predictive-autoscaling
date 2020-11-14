package io.adobe

import org.scalatest.FunSuite

class PrometheusStreamReaderTest extends FunSuite {

  test("testStream") {
//    Source.single(0).map(_ + 1).filter(_ != 0).map(_ - 2).to(Sink.fold(0, (a: Int, b: Int) => a + b))
    val reader = new PrometheusStreamReader("localhost:9090")
//    reader.stream()
  }

}
