package app.aventurine.jetmapdemo.data.fileStorage

import java.io.File

interface FileStorage {
    fun saveFile(fileName: String, fileData: ByteArray): Result<File>
    fun getFileIfExists(fileName: String): Result<File>
    fun getFileData(fileName: String): Result<ByteArray>
}