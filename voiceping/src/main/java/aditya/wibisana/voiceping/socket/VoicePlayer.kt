package aditya.wibisana.voiceping.socket

import aditya.wibisana.voiceping.player.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VoicePlayer(
    socketMessageMessenger: SocketMessageMessenger,
    player: Player,
) {
    private val worker = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        worker.launch {
            socketMessageMessenger.messages.collect {
                when (it.messageType) {
                    MessageType.Audio.type -> {
                        val payload = it.payload as Payload.ByteArrayPayload
                        player.play(payload.value)
                    }
                    MessageType.StartTalking.type -> {

                    }
                    MessageType.StopTalking.type -> {

                    }
                }
            }
        }
    }
}