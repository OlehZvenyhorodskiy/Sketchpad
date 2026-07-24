package com.example.data.storage

import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AtomicCanvasStorage(private val targetFile: File) {

    init {
        val parentDir = targetFile.parentFile
        if (parentDir != null && !targetFile.canonicalPath.startsWith(parentDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt detected: ${targetFile.name}")
        }
    }

    private val atomicFile = AtomicFile(targetFile)
    private val backupFile = File(targetFile.parentFile, "${targetFile.name}.bak")

    suspend fun saveCanvasData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        var stream: FileOutputStream? = null
        try {
            if (targetFile.exists()) {
                runCatching { targetFile.copyTo(backupFile, overwrite = true) }
            }
            stream = atomicFile.startWrite()
            stream.write(data)
            stream.fd.sync()
            atomicFile.finishWrite(stream)
            true
        } catch (e: Exception) {
            Log.e("AtomicCanvasStorage", "Failed to save canvas data", e)
            stream?.let { atomicFile.failWrite(it) }
            false
        }
    }

    suspend fun readCanvasData(): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            if (targetFile.exists() && targetFile.length() > 0) {
                atomicFile.readFully()
            } else if (backupFile.exists() && backupFile.length() > 0) {
                Log.w("AtomicCanvasStorage", "Target file unreadable/empty, fallback to backup file")
                backupFile.readBytes()
            } else null
        }.getOrNull()
    }
}
