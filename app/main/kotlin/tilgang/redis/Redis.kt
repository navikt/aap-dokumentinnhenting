package tilgang.redis

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.SetParams
import tilgang.LOGGER
import tilgang.RedisConfig
import java.net.URI
import java.time.LocalDateTime

const val EnDagSekunder: Long = 60 * 60 * 24

data class Key(
    val prefix: String = "",
    val value: String
) {
    fun get(): ByteArray = "$prefix:$value".toByteArray()
}

private val logger = LoggerFactory.getLogger("Redis")

class Redis private constructor(
    private val pool: JedisPool
) : AutoCloseable {
    constructor(config: RedisConfig) : this(
        JedisPool(
            JedisPoolConfig(),
            HostAndPort(config.uri.host, config.uri.port),
            DefaultJedisClientConfig.builder().ssl(true).user(config.username).password(config.password).build()
        )
    )

    constructor(uri: URI) : this(JedisPool(uri))

    private fun splitKey(splittedKey: String): Key {
        val split = splittedKey.split(":")
        return Key(split[1], split[0])
    }

    fun set(key: Key, value: ByteArray, expireSec: Long = 3600) {
        pool.resource.use {
            it.set(key.get(), value, SetParams().ex(expireSec))
        }
    }

    fun setExpire(key: Key, expireSec: Long) {
        pool.resource.use {
            val updatedRows = it.expire(key.get(), expireSec)
            if (updatedRows == 0L) {
                logger.warn("Forventet å oppdatere TTL, men nøkkelen ble ikke oppdatert")
                LOGGER.warn("Forventet å oppdatere TTL, men nøkkelen[{}] ble ikke oppdatert", key)
            }
        }
    }

    operator fun get(key: Key): ByteArray? {
        pool.resource.use {
            return it.get(key.get())
        }
    }

    fun del(key: Key) {
        pool.resource.use {
            it.del(key.get())
        }
    }

    fun ready(): Boolean {
        pool.resource.use {
            return it.ping() == "PONG"
        }
    }

    fun lastUpdated(key: Key): LocalDateTime {
        pool.resource.use {
            return LocalDateTime.now().minusSeconds(EnDagSekunder - it.ttl(key.get()))
        }
    }

    fun expiresIn(key: Key): Long {
        pool.resource.use {
            return it.ttl(key.get())
        }
    }

    fun exists(key: Key): Boolean {
        pool.resource.use {
            return it.exists(key.get())
        }
    }

    override fun close() {
        pool.close()
    }

    companion object {
        inline fun <reified T> ByteArray.deserialize(): T {
            val mapper = jacksonObjectMapper()
            val tr = object : TypeReference<T>() {}
            return mapper.readValue(this, tr)
        }

        inline fun <reified T> T.serialize(): ByteArray {
            val mapper = jacksonObjectMapper()
            return mapper.writeValueAsBytes(this)
        }
    }
}