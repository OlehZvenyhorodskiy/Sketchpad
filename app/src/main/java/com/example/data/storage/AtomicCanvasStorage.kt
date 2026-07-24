package com.example.data.storage

import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AtomicCanvasStorage(private val targetFile: File) {

    private val atomicFile = AtomicFile(targetFile)

    suspend fun saveCanvasData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(data)
            atomicFile.finishWrite(stream)
            true
        } catch (e: Exception) {
            stream?.let { atomicFile.failWrite(it) }
            false
        }
    }

    suspend fun readCanvasData(): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            atomicFile.readFully()
        }.getOrNull()
    }
}
