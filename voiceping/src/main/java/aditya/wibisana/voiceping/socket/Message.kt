package aditya.wibisana.voiceping.socket

sealed class Payload {
    data class IntPayload(val value: Int) : Payload()
    data class StringPayload(val value: String) : Payload()
    class ByteArrayPayload(val value: ByteArray) : Payload()
}

data class Message(
    val channelType: Int,
    val messageType: Int,
    val messageSenderUserId: Int,
    val messageReceiveChannelId: Int,
    val payload: Payload? = null
)

enum class MessageType(val type: Int) {
    Unknown(-1),
    StartTalking(1),
    StopTalking(2),
    Audio(3),
    Connection(4),
    Status(5),
    AckStart(6),
    AckEnd(7),
    AckStartFail(8),
    DuplicateLogin(9),
    UpdateUser(10),
    DeleteUser(11),
    UpdateChannel(12),
    DeleteChannel(13),
    InvalidUser(14),
    ChannelAddUser(15),
    ChannelRemoveUser(16),
    Text(17),
    Image(18),
    OfflineMessage(19),
    DeliveredMessage(20),
    ReadMessage(21),
    AckText(22),
    InteractiveText(23),
    ConnectionTest(25),
    ConnectionIsClean(26),
    UnauthorizedGroup(27);
}