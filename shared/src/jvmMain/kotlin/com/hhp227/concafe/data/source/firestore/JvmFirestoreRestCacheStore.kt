package com.hhp227.concafe.data.source.firestore

import java.io.File

class JvmFirestoreRestCacheStore : FirestoreRestCacheStore {
    private val cacheDir: File = File(System.getProperty("user.home"), ".concafe${File.separator}cache${File.separator}firestore")

    override fun load(key: String): String? {
        val file = resolveFile(key)
        return if (file.exists()) runCatching { file.readText() }.getOrNull() else null
    }

    override fun save(key: String, payload: String) {
        val file = resolveFile(key)
        runCatching { file.writeText(payload) }
    }

    override fun clear() {
        cacheDir.listFiles()
            ?.filter { file -> file.name.endsWith(CACHE_FILE_EXTENSION) }
            ?.forEach { file -> file.delete() }
    }

    private fun resolveFile(key: String): File {
        return File(cacheDir, "${key.hashCode()}$CACHE_FILE_EXTENSION")
    }

    init {
        cacheDir.mkdirs()
    }
}

private const val CACHE_FILE_EXTENSION = ".cache"
