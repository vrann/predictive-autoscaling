package io.adobe.prometheus

import com.google.protobuf.ByteString
import org.scalatest.FunSuite

class XORChunkIteratorTest extends FunSuite {

  test("testIncreasing") {

    val stream: Array[Byte] = Array(-30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128,
      15, -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22,
      19, -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0,
      0, 0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1,
      -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96,
      -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10,
      -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23,
      1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15,
      -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19,
      -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0,
      0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1,
      -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96,
      -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10,
      -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23,
      1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15,
      -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19,
      -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0,
      0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1,
      -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96,
      -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10,
      -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23,
      1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15,
      -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19,
      -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0,
      0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1,
      -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96,
      -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10,
      -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23,
      1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15,
      -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19,
      -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0,
      0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1,
      -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0, -30, -96,
      -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15, -40, 14, -1, -117, 88, 92, 0, -10,
      -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19, -30, 39, 119, 3, -74, -58, -38, 23,
      1, 88, -65, -14, 64, 16, 0, -30, -96, -75, -19, -82, 93, 63, -16, 0, 0, 0, 0, 0, 0, -24, 7, -62, 95, -1, -128, 15,
      -40, 14, -1, -117, 88, 92, 0, -10, -48, 119, -4, 72, 1, -11, -1, -118, -1, -1, 96, 93, -1, -4, -33, -8, -22, 19,
      -30, 39, 119, 3, -74, -58, -38, 23, 1, 88, -65, -14, 64, 16, 0)

    //    val streamModified = stream.map(x => x & 0xff)
    val bytes: ByteString = ByteString.copyFrom(stream)
    val iterator = new XORChunkIterator(bytes.toByteArray)
    iterator.next()
    assert((1604016580657L, 1.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016581657L, 2.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016582672L, 3.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016583657L, 4.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016584672L, 5.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016585657L, 5.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016586673L, 5.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016587674L, 5.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016588674L, 6.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016589673L, 7.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016590657L, 8.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016593846L, 9.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016594673L, 10.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016595672L, 10.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016596657L, 10.0) == (iterator.t, iterator.v))
    iterator.next()
    assert((1604016597674L, 10.0) == (iterator.t, iterator.v))
  }

}