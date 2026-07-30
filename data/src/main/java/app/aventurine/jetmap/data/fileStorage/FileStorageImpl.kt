package app.aventurine.jetmap.data.fileStorage

import android.content.Context
import app.aventurine.jetmap.domain.fileStorage.FileStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

class FileStorageImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : FileStorage {
    override fun saveFile(
        fileName: String,
        fileData: ByteArray
    ): Result<File> {
        val file = File(
            context.filesDir,
            fileName
        )

        return try {
            if (!file.exists())
                file.createNewFile()

            file.outputStream().use { outputStream ->
                outputStream.write(fileData)
            }

            Result.success(value = file)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }

    override fun getFileIfExists(fileName: String): Result<File> {
        val file = File(
            context.filesDir,
            fileName
        )

        return try {
            if (!file.exists())
                return Result.failure(exception = FileNotFoundException())

            return Result.success(value = file)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }

    override fun getFileData(fileName: String): Result<ByteArray> {
        return try {
            val file = getFileIfExists(fileName = fileName).getOrThrow()
            file.inputStream().use { inputStream ->
                Result.success(value = inputStream.readBytes())
            }
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}