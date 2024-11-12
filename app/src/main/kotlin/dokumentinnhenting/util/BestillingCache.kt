package dokumentinnhenting.util

import kotlin.concurrent.timer

object BestillingCache {
    private val saksnummerCache = mutableListOf<String>()

    init {
        timer(period = 5000) {
            saksnummerCache.clear()
        }
    }

    fun add(item: String) {
        saksnummerCache.add(item)
    }

    fun contains(item: String): Boolean {
        return saksnummerCache.contains(item)
    }
}
