package integrasjonportal.util.kafka

class NoopStream : Stream {
    override fun ready() = true
    override fun live() = true
    override fun close() {}
    override fun start() {}
}