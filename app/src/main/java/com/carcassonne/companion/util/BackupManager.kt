package com.carcassonne.companion.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class BackupPayload(
    val version: Int = 3,
    val players: List<PlayerBackupData>,
    val games: List<GameBackupData>,
    val gamePlayers: List<GamePlayerBackupData>,
    val photos: List<PhotoBackupData> = emptyList()
)

@Serializable
data class PhotoBackupData(val fileName: String, val contentBase64: String)

@Serializable
data class PlayerBackupData(
    val id: Int,
    val name: String,
    val meepleColor: String,
    val avatarFile: String?,
    val createdAt: Long
)

@Serializable
data class GameBackupData(
    val id: Int,
    val name: String?,
    val date: Long,
    val durationSeconds: Long?,
    val expansions: String,
    val photoFile: String?,
    val notes: String? = null
)

@Serializable
data class GamePlayerBackupData(
    val gameId: Int,
    val playerId: Int,
    val meepleColor: String,
    val finalScore: Int,
    val cityPoints: Int,
    val roadPoints: Int,
    val monasteryPoints: Int,
    val farmPoints: Int,
    val placement: Int
)

object BackupManager {
    const val EXTENSION = "ccbackup"
    private val SUPPORTED_EXTENSIONS = setOf(EXTENSION, "bin")
    private val MAGIC = "CCBIN01".toByteArray(Charsets.US_ASCII)
    private const val SALT = "CarcSalt20242025"
    private val PASSWORD = "Carcassonne" + "Companion#2024"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 65536
    private const val MAX_BACKUP_BYTES = 256L * 1024L * 1024L

    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }

    private fun deriveKey(): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(PASSWORD.toCharArray(), SALT.toByteArray(Charsets.UTF_8), ITERATIONS, KEY_LENGTH)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun encrypt(plainText: ByteArray): ByteArray {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        cipher.updateAAD(MAGIC)
        return MAGIC + iv + cipher.doFinal(plainText)
    }

    private fun decrypt(fileBytes: ByteArray): ByteArray {
        val minimumSize = MAGIC.size + IV_LENGTH + GCM_TAG_LENGTH / 8
        require(fileBytes.size >= minimumSize) { "Повреждённый или неподдерживаемый BIN-файл" }
        require(fileBytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            "Неверный формат резервной копии. Поддерживаются только BIN-файлы."
        }
        val ivStart = MAGIC.size
        val iv = fileBytes.copyOfRange(ivStart, ivStart + IV_LENGTH)
        val encrypted = fileBytes.copyOfRange(ivStart + IV_LENGTH, fileBytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        cipher.updateAAD(MAGIC)
        return try {
            cipher.doFinal(encrypted)
        } catch (e: AEADBadTagException) {
            throw IllegalArgumentException("Файл повреждён или не является действительной резервной копией", e)
        }
    }

    fun getBackupDir(context: Context): File =
        (context.getExternalFilesDir("backups") ?: File(context.filesDir, "backups")).also { it.mkdirs() }

    fun listBackupFiles(context: Context): List<File> =
        getBackupDir(context).listFiles { f -> f.extension.lowercase(Locale.ROOT) in SUPPORTED_EXTENSIONS }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun createBackup(
        context: Context,
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ): File {
        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
        val outFile = uniqueFile(getBackupDir(context), dateStr)
        FileOutputStream(outFile).use { writeBackup(it, players, games, gamePlayers) }
        return outFile
    }

    fun createBackupToUri(
        context: Context, uri: Uri,
        players: List<PlayerEntity>, games: List<GameEntity>, gamePlayers: List<GamePlayerEntity>
    ) {
        context.contentResolver.openOutputStream(uri)?.use {
            writeBackup(it, players, games, gamePlayers)
        } ?: throw IllegalStateException("Не удалось открыть файл для записи")
    }

    fun createBackupToFolderUri(
        context: Context, treeUri: Uri,
        players: List<PlayerEntity>, games: List<GameEntity>, gamePlayers: List<GamePlayerEntity>
    ): String {
        val folder = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Недопустимая папка резервных копий")
        val dateStr = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.US).format(Date())
        val fileName = "$dateStr.$EXTENSION"
        val existing = folder.findFile(fileName)
        val docFile = if (existing != null && existing.isFile) existing
        else folder.createFile("application/octet-stream", fileName)
            ?: throw IllegalStateException("Не удалось создать файл резервной копии")
        context.contentResolver.openOutputStream(docFile.uri)?.use {
            writeBackup(it, players, games, gamePlayers)
        } ?: throw IllegalStateException("Не удалось открыть файл резервной копии")
        return docFile.name ?: fileName
    }

    fun restoreBackupFromUri(context: Context, uri: Uri): RestoreResult {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Не удалось открыть файл резервной копии")
        return input.use { parseBackup(it, File(context.filesDir, "photos")) }
    }

    fun restoreBackup(context: Context, file: File): RestoreResult {
        require(file.length() <= MAX_BACKUP_BYTES) { "Файл резервной копии слишком большой" }
        return FileInputStream(file).use { parseBackup(it, File(context.filesDir, "photos")) }
    }

    fun listBackupFilesInFolderUri(context: Context, treeUri: Uri): List<DocumentFile> {
        val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return folder.listFiles()
            .filter { it.isFile && it.name?.substringAfterLast(".", "")?.lowercase(Locale.ROOT) in SUPPORTED_EXTENSIONS }
            .sortedByDescending { it.lastModified() }
    }

    private fun buildPayload(
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ): BackupPayload {
        val photoFiles = linkedMapOf<String, File>()
        fun rememberPhoto(path: String?): String? {
            val file = path?.let(::File)?.takeIf { it.isFile } ?: return null
            val name = file.name
            photoFiles.putIfAbsent(name, file)
            return name
        }
        val playerData = players.map { p ->
            PlayerBackupData(p.id, p.name, p.meepleColor, rememberPhoto(p.avatarPath), p.createdAt)
        }
        val gameData = games.map { g ->
            GameBackupData(g.id, g.name, g.date, g.durationSeconds, g.expansions, rememberPhoto(g.photoPath), g.notes)
        }
        val gpData = gamePlayers.map { gp ->
            GamePlayerBackupData(gp.gameId, gp.playerId, gp.meepleColor, gp.finalScore,
                gp.cityPoints, gp.roadPoints, gp.monasteryPoints, gp.farmPoints, gp.placement)
        }
        val photos = photoFiles.map { (name, file) ->
            PhotoBackupData(name, Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
        }
        return BackupPayload(players = playerData, games = gameData, gamePlayers = gpData, photos = photos)
    }

    private fun writeBackup(
        out: OutputStream,
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ) {
        val payloadBytes = json.encodeToString(buildPayload(players, games, gamePlayers)).toByteArray(Charsets.UTF_8)
        require(payloadBytes.size.toLong() <= MAX_BACKUP_BYTES) { "Резервная копия слишком большая" }
        out.write(encrypt(payloadBytes))
        out.flush()
    }

    private fun parseBackup(inputStream: InputStream, photosDir: File): RestoreResult {
        val encrypted = inputStream.readBytes()
        require(encrypted.size.toLong() <= MAX_BACKUP_BYTES) { "Файл резервной копии слишком большой" }
        val plaintext = decrypt(encrypted)
        val payload = try {
            json.decodeFromString<BackupPayload>(String(plaintext, Charsets.UTF_8))
        } catch (e: Exception) {
            throw IllegalArgumentException("Не удалось прочитать содержимое резервной копии", e)
        }
        require(payload.version == 3) { "Версия резервной копии не поддерживается" }

        val decodedPhotos = linkedMapOf<String, ByteArray>()
        payload.photos.forEach { photo ->
            val name = photo.fileName
            require(name.isNotBlank() && name != "." && name != ".." &&
                !name.contains('/') && !name.contains('\\') && File(name).name == name) {
                "Недопустимое имя файла фотографии"
            }
            val bytes = try { Base64.decode(photo.contentBase64, Base64.DEFAULT) }
                catch (e: IllegalArgumentException) { throw IllegalArgumentException("Повреждены данные фотографии: $name", e) }
            decodedPhotos[name] = bytes
        }

        photosDir.mkdirs()
        decodedPhotos.forEach { (name, bytes) ->
            val target = File(photosDir, name)
            val temp = File(photosDir, "$name.restore-tmp")
            FileOutputStream(temp).use { it.write(bytes) }
            if (target.exists() && !target.delete()) {
                temp.delete()
                throw IllegalStateException("Не удалось обновить фотографию $name")
            }
            if (!temp.renameTo(target)) {
                temp.delete()
                throw IllegalStateException("Не удалось сохранить фотографию $name")
            }
        }

        return RestoreResult(
            payload.players.map { PlayerEntity(it.id, it.name, it.meepleColor,
                it.avatarFile?.let { f -> File(photosDir, f).absolutePath }, it.createdAt) },
            payload.games.map { GameEntity(it.id, it.name, it.date, it.durationSeconds, it.expansions,
                it.photoFile?.let { f -> File(photosDir, f).absolutePath }, it.notes) },
            payload.gamePlayers.map { GamePlayerEntity(it.gameId, it.playerId, it.meepleColor,
                it.finalScore, it.cityPoints, it.roadPoints, it.monasteryPoints, it.farmPoints, it.placement) }
        )
    }

    data class RestoreResult(
        val players: List<PlayerEntity>,
        val games: List<GameEntity>,
        val gamePlayers: List<GamePlayerEntity>
    )

    private fun uniqueFile(dir: File, dateStr: String): File {
        val base = File(dir, "$dateStr.$EXTENSION")
        if (!base.exists()) return base
        var n = 1
        while (true) {
            val candidate = File(dir, "${dateStr}_$n.$EXTENSION")
            if (!candidate.exists()) return candidate
            n++
        }
    }
}
