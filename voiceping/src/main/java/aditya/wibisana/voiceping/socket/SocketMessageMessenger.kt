package aditya.wibisana.voiceping.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.msgpack.core.MessagePack
import java.nio.ByteBuffer

class SocketMessageMessenger {
    private val _messages = MutableSharedFlow<Message>()
    val messages = _messages.asSharedFlow()

    private val worker = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun send(bytes: ByteBuffer) = worker.launch {
        MessagePack.newDefaultUnpacker(bytes).use { unPacker ->
            val channelType = unPacker.unpackInt()
            val messageType = unPacker.unpackInt()
            val messageSenderUserId = unPacker.unpackInt()
            val messageReceiveChannelId = unPacker.unpackInt()

            val payload: Payload? = if (unPacker.hasNext()) {
                val unpacked = unPacker.unpackValue()
                when {
                    unpacked.isStringValue -> {
                        Payload.StringPayload(unpacked.asStringValue().asString())
                    }
                    unpacked.isIntegerValue -> {
                        Payload.IntPayload(unpacked.asIntegerValue().asInt())
                    }
                    unpacked.isArrayValue -> {
//                        val byteArray = unpacked.asArrayValue().map { it.asIntegerValue().toByte() }.toByteArray()
                        Payload.ByteArrayPayload(unpacked.asRawValue().asByteArray())
                    }
                    else -> null
                }
            } else {
                null
            }
            _messages.emit(Message(channelType, messageType, messageSenderUserId, messageReceiveChannelId, payload))
        }
    }
}
