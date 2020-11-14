package io.adobe

class XORChunkIterator(bytes: Array[Byte]) {
  val bits = BigEndianBitSet.valueOf(bytes)
  var numRead = 0
  var t: Long = 0L
  var v: Double = 0.0d
  var delta: Long = 0L
  var leading: Int = 0
  var trailing: Int = 0

  def hasNext: Boolean = {
    bits.getBitsOffset <= bits.length()
  }

  def next(): Boolean = {
    if (numRead == 0) {
      val (t: Long, index: Int) = readVarint()
      val v: Long = bits.readBits(64)
      this.t = t
      this.v = java.lang.Double.longBitsToDouble(v)
      numRead += 1
      return true
    }

    if (numRead == 1) {
      val (delta: Long, index: Int) = readUVarint()
      this.delta = delta
      this.t = this.t + delta

      readValue()
      return true
    }

    var d: Int = 0
    var i = 0
    var bit = true
    while (i < 4 && bit) {
      d <<= 1
      bit = bits.readBit()
      if (bit) {
        d |= 1
      }
      i += 1
    }

    var dod: Long = 0
    val sz = d match {
      case 0x00 => 0
      case 0x02 => 14
      case 0x06 => 17
      case 0x0e => 20
      case 0x0f => {
        dod = bits.readBits(64)
        0
      }
      case _ => 0
    }
    if (sz != 0) {
      var otherbits = bits.readBits(sz)
      if (otherbits > (1 << (sz - 1))) {
        // or something
        otherbits = otherbits - (1 << sz)
      }
      dod = otherbits
    }

    this.delta = this.delta + dod
    this.t = this.t + this.delta
    readValue()
    true
  }

  def readValue() = {
    var bit = bits.readBit()

    if (!bit) {
      // it.val = it.val
    } else {
      var bit = bits.readBit()

      if (!bit) {
        // reuse leading/trailing zero bits
        // it.leading, it.trailing = it.leading, it.trailing
      } else {
        var otherBits = bits.readBits(5)
        leading = otherBits.toInt
        otherBits = bits.readBits(6)
        var mbits = otherBits.toInt
        // 0 significant bits here means we overflowed and we actually need 64; see comment in encoder
        if (mbits == 0) {
          mbits = 64
        }
        trailing = 64 - leading - mbits
      }

      val mbits = 64 - leading - trailing
      val otherBits: Long = bits.readBits(mbits)
      var vbits: Long = 0
      if (v.isNaN) {
        //Double.NaN is represented differently in golang when interpreted by Prometheus
        vbits = 0x7FF0000000000002L
      } else {
        vbits = java.lang.Double.doubleToLongBits(v)
      }

      vbits ^= otherBits << trailing
      v = java.lang.Double.longBitsToDouble(vbits)
    }

    numRead += 1
    true
  }

  def readVarint(): (Long, Int) = {
    val (ux, index) = readUVarint()
    var x = ux >> 1
    if ((ux & 1) != 0) {
      x ^= x
    }
    (x, index)
  }

  def readUVarint(): (Long, Int) = {
    var x: Long = 0
    var s = 0
    var i = 0
    var next: Long = bits.readBits(8)
    while (next >= 0x80 && i < 9) {
      next = (next & 0x7f) << s
      x |= next
      s += 7
      i += 1
      next = bits.readBits(8)
    }
    if (i == 9 && next > 1) return (0, 0)

    next = (next & 0x7f) << s
    x |= next
    i += 1
    (x, i)
  }
}
