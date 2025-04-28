package com.fodsdk.games

class GameFactory(private val apk: String) {
    fun getGame(gid: String): Game? {
        return when (gid) {
            else -> null
        }
    }
}