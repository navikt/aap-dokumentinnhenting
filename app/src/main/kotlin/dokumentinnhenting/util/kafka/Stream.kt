package dokumentinnhenting.util.kafka

interface Stream {
    fun ready(): Boolean
    fun live(): Boolean
    fun close()
    fun start()
}