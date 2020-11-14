package io.adobe

import akka.stream.scaladsl.Framing.FramingException
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.{ByteIterator, ByteString}

private final class UvarintFramingStage extends GraphStage[FlowShape[ByteString, ByteString]] {

  def uVariantDecoder(bs: ByteIterator): (Int, Int) = {
    var x = 0
    var s = 0
    var i = 0
    var next = bs.next().toInt & 0xff
    while (next >= 0x80 && i < 9) {
      x |= (next & 0x7f)
      x <<= s
      s += 7
      i += 1
      next = bs.next().toInt & 0xff
    }
    if (i == 9 && next > 1) return (0, 0)
    x |= ((next & 0x7f) << s)
    i += 1
    (x, i)
  }

  private val minimumChunkSize = 1

  val in = Inlet[ByteString]("LengthFieldFramingStage.in")
  val out = Outlet[ByteString]("LengthFieldFramingStage.out")
  override val shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      private var buffer = ByteString.empty
      private var frameSize = Int.MaxValue

      /**
       * push, and reset frameSize and buffer
       *
       */
      private def pushFrame() = {
        val emit = buffer.take(frameSize).compact
        buffer = buffer.drop(frameSize)
        frameSize = Int.MaxValue
        push(out, emit)
        if (buffer.isEmpty && isClosed(in)) {
          completeStage()
        }
      }

      /**
       * try to push downstream, if failed then try to pull upstream
       *
       */
      private def tryPushFrame() = {
        val buffSize = buffer.size
        if (buffSize >= frameSize) {
          pushFrame()
        } else if (buffSize >= minimumChunkSize) {
          val (frameSize, uVariantSize) = uVariantDecoder(buffer.iterator)
          buffer = buffer.drop(4 + uVariantSize)
          if (frameSize < minimumChunkSize) {
            failStage(
              new FramingException(s"Computed frame size $frameSize is less than minimum chunk size $minimumChunkSize"))
          } else if (buffSize >= frameSize) {
            pushFrame()
          } else tryPull()
        } else tryPull()
      }

      private def tryPull() = {
        if (isClosed(in)) {
          failStage(new FramingException("Stream finished but there was a truncated final frame in the buffer"))
        } else pull(in)
      }

      override def onPush(): Unit = {
        buffer ++= grab(in)
        tryPushFrame()
      }

      override def onPull() = tryPushFrame()

      override def onUpstreamFinish(): Unit = {
        if (buffer.isEmpty) {
          completeStage()
        } else if (isAvailable(out)) {
          tryPushFrame()
        } // else swallow the termination and wait for pull
      }

      setHandlers(in, out, this)
    }
}
