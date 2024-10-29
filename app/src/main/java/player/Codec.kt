package player

interface Codec {
    fun decode(input: ByteArray) : ByteArray
    fun encode(input: ByteArray) : ByteArray
}

class CodecImp : Codec {
    override fun decode(input: ByteArray): ByteArray {
        return input
    }

    override fun encode(input: ByteArray): ByteArray {
        return input
    }
}