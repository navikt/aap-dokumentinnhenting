package dokumentinnhenting

import kotlin.random.Random

fun randomNavIdent(): String {
    return ('A' .. 'Z').random() + Random.nextLong(100000, 999999).toString()
}

fun randomPersonIdent(): String {
    return Random.nextLong(10000000000L, 99999999999L).toString()
}