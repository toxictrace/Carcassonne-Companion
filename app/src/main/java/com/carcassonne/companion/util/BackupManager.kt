package com.carcassonne.companion.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class BackupPayload(
    val version: Int = 2,
    val players: List<PlayerBackupData>,
    val games: List<GameBackupData>,
    val gamePlayers: List<GamePlayerBackupData>
)

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
    private const val DATA_ENTRY = "d"
    private const val PHOTOS_PREFIX = "p/"
    private const val SALT = "CarcSalt20242025"
    private const val PASSWORD = "CarcassonneCompanion#2024"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 65536

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun deriveKey(): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(PASSWORD.toCharArray(), SALT.toByteArray(), ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    private fun encrypt(data: ByteArray): ByteArray {
        val key = deriveKey()
        val iv = ByteArray(IV_LENGTH).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val key = deriveKey()
        val iv = data.copyOfRange(0, IV_LENGTH)
        val encrypted = data.copyOfRange(IV_LENGTH, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }

    fun getBackupDir(context: Context): File =
        (context.getExternalFilesDir("backups") ?: File(context.filesDir, "backups"))
            .also { it.mkdirs() }

    fun listBackupFiles(context: Context): List<File> =
        getBackupDir(context)
            .listFiles { f -> f.extension.equals(EXTENSION, ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun createBackup(
        context: Context,
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ): File {
        val backupDir = getBackupDir(context)
        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
        val outFile = uniqueFile(backupDir, dateStr)
        FileOutputStream(outFile).use { writeBackupZip(it, players, games, gamePlayers) }
        return outFile
    }

    fun createBackupToUri(
        context: Context, uri: Uri,
        players: List<PlayerEntity>, games: List<GameEntity>, gamePlayers: List<GamePlayerEntity>
    ) {
        context.contentResolver.openOutputStream(uri)?.use {
            writeBackupZip(it, players, games, gamePlayers)
        } ?: throw IllegalStateException("Cannot open output stream for URI")
    }

    fun createBackupToFolderUri(
        context: Context, treeUri: Uri,
        players: List<PlayerEntity>, games: List<GameEntity>, gamePlayers: List<GamePlayerEntity>
    ): String {
        val folder = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Invalid backup folder URI")
        val dateStr = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.US).format(Date())
        val fileName = "$dateStr.$EXTENSION"
        val existing = folder.findFile(fileName)
        val docFile = if (existing != null && existing.isFile) existing
        else folder.createFile("application/octet-stream", fileName)
            ?: throw IllegalStateException("Cannot create backup file in selected folder")
        context.contentResolver.openOutputStream(docFile.uri)?.use {
            writeBackupZip(it, players, games, gamePlayers)
        } ?: throw IllegalStateException("Cannot open output stream for backup file")
        return docFile.name ?: fileName
    }

    fun restoreBackupFromUri(context: Context, uri: Uri): RestoreResult {
        val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for URI")
        return parseBackupStream(inputStream, photosDir)
    }

    fun restoreBackup(context: Context, file: File): RestoreResult {
        val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
        return parseBackupStream(FileInputStream(file), photosDir)
    }

    fun listBackupFilesInFolderUri(context: Context, treeUri: Uri): List<DocumentFile> {
        val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return folder.listFiles()
            .filter { it.isFile && it.name?.endsWith(".$EXTENSION", ignoreCase = true) == true }
            .sortedByDescending { it.lastModified() }
    }

    private fun buildPayload(
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ): BackupPayload {
        val playerData = players.map { p ->
            PlayerBackupData(p.id, p.name, p.meepleColor,
                p.avatarPath?.let { File(it) }?.takeIf { it.exists() }?.name, p.createdAt)
        }
        val gameData = games.map { g ->
            GameBackupData(g.id, g.name, g.date, g.durationSeconds, g.expansions,
                g.photoPath?.let { File(it) }?.takeIf { it.exists() }?.name, g.notes)
        }
        val gpData = gamePlayers.map { gp ->
            GamePlayerBackupData(gp.gameId, gp.playerId, gp.meepleColor, gp.finalScore,
                gp.cityPoints, gp.roadPoints, gp.monasteryPoints, gp.farmPoints, gp.placement)
        }
        return BackupPayload(players = playerData, games = gameData, gamePlayers = gpData)
    }

    private fun collectPhotoFiles(
        players: List<PlayerEntity>,
        games: List<GameEntity>
    ): Map<String, File> = buildMap {
        players.forEach { p ->
            p.avatarPath?.let { File(it) }?.takeIf { it.exists() }?.let { put(it.name, it) }
        }
        games.forEach { g ->
            g.photoPath?.let { File(it) }?.takeIf { it.exists() }?.let { put(it.name, it) }
        }
    }

    private fun writeBackupZip(
        out: OutputStream,
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ) {
        val payload = buildPayload(players, games, gamePlayers)
        val encData = encrypt(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
        val photoFiles = collectPhotoFiles(players, games)
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry(DATA_ENTRY))
            zos.write(encData)
            zos.closeEntry()
            photoFiles.forEach { (name, file) ->
                zos.putNextEntry(ZipEntry("$PHOTOS_PREFIX$name"))
                zos.write(encrypt(file.readBytes()))
                zos.closeEntry()
            }
        }
    }

    private fun parseBackupStream(inputStream: InputStream, photosDir: File): RestoreResult {
        var payload: BackupPayload? = null
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name == DATA_ENTRY -> {
                        val decrypted = decrypt(zis.readBytes())
                        payload = json.decodeFromString(String(decrypted, Charsets.UTF_8))
                    }
                    entry.name.startsWith(PHOTOS_PREFIX) -> {
                        val fname = entry.name.removePrefix(PHOTOS_PREFIX)
                        if (fname.isNotEmpty()) {
                            val bytes = decrypt(zis.readBytes())
                            FileOutputStream(File(photosDir, fname)).use { it.write(bytes) }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        val p = payload ?: throw IllegalStateException("Corrupt or invalid backup file")
        return RestoreResult(
            p.players.map { PlayerEntity(it.id, it.name, it.meepleColor,
                it.avatarFile?.let { f -> File(photosDir, f).absolutePath }, it.createdAt) },
            p.games.map { GameEntity(it.id, it.name, it.date, it.durationSeconds, it.expansions,
                it.photoFile?.let { f -> File(photosDir, f).absolutePath }, it.notes) },
            p.gamePlayers.map { GamePlayerEntity(it.gameId, it.playerId, it.meepleColor,
                it.finalScore, it.cityPoints, it.roadPoints, it.monasteryPoints,
                it.farmPoints, it.placement) }
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
            val c = File(dir, "${dateStr}_$n.$EXTENSION")
            if (!c.exists()) return c
            n++
        }
    }
}
