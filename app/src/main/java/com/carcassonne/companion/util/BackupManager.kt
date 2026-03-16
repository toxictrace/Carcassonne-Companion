package com.carcassonne.companion.util

import android.content.Context
import android.net.Uri
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

// ─── Backup payload data classes ─────────────────────────────────────────────

@Serializable
data class BackupPayload(
    val version: Int = 1,
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
    val photoFile: String?
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

// ─── BackupManager ────────────────────────────────────────────────────────────

object BackupManager {

    const val EXTENSION = "ccbackup"
    private const val DATA_ENTRY = "data.enc"
    private const val PHOTOS_PREFIX = "photos/"
    private val XOR_KEY = "CarcassonneBackup2024".toByteArray(Charsets.UTF_8)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Directory where backups are stored — no permission needed */
    fun getBackupDir(context: Context): File =
        (context.getExternalFilesDir("backups") ?: File(context.filesDir, "backups"))
            .also { it.mkdirs() }

    /** Returns all .ccbackup files sorted newest-first */
    fun listBackupFiles(context: Context): List<File> =
        getBackupDir(context)
            .listFiles { f -> f.extension.equals(EXTENSION, ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    // ─── Create backup ────────────────────────────────────────────────────────

    fun createBackup(
        context: Context,
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ): File {
        val backupDir = getBackupDir(context)
        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
        val outFile = uniqueFile(backupDir, dateStr)

        // Collect photo files (avoid duplicates)
        val photoFiles: Map<String, File> = buildMap {
            players.forEach { p -> p.avatarPath?.let { File(it) }?.takeIf { it.exists() }?.let { put(it.name, it) } }
            games.forEach { g -> g.photoPath?.let { File(it) }?.takeIf { it.exists() }?.let { put(it.name, it) } }
        }

        val playerData = players.map { p ->
            val fname = p.avatarPath?.let { File(it) }?.takeIf { it.exists() }?.name
            PlayerBackupData(p.id, p.name, p.meepleColor, fname, p.createdAt)
        }
        val gameData = games.map { g ->
            val fname = g.photoPath?.let { File(it) }?.takeIf { it.exists() }?.name
            GameBackupData(g.id, g.name, g.date, g.durationSeconds, g.expansions, fname)
        }
        val gpData = gamePlayers.map { gp ->
            GamePlayerBackupData(
                gp.gameId, gp.playerId, gp.meepleColor, gp.finalScore,
                gp.cityPoints, gp.roadPoints, gp.monasteryPoints, gp.farmPoints, gp.placement
            )
        }

        val payload = BackupPayload(players = playerData, games = gameData, gamePlayers = gpData)
        val jsonBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        val encBytes = xorCrypt(jsonBytes)

        FileOutputStream(outFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zos.putNextEntry(ZipEntry(DATA_ENTRY))
                zos.write(encBytes)
                zos.closeEntry()

                photoFiles.forEach { (name, file) ->
                    zos.putNextEntry(ZipEntry("$PHOTOS_PREFIX$name"))
                    FileInputStream(file).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        return outFile
    }

    // ─── URI-based backup (SAF) ───────────────────────────────────────────────

    fun createBackupToUri(
        context: Context,
        uri: Uri,
        players: List<PlayerEntity>,
        games: List<GameEntity>,
        gamePlayers: List<GamePlayerEntity>
    ) {
        val photoFiles: Map<String, File> = buildMap {
            players.forEach { p -> p.avatarPath?.let { File(it) }?.takeIf { it.exists() }?.let { put(it.name, it) } }
            games.forEach { g -> g.photoPath?.let { File(it) }?.takeIf { it.exists() }?.let { put(it.name, it) } }
        }
        val playerData = players.map { p ->
            PlayerBackupData(p.id, p.name, p.meepleColor,
                p.avatarPath?.let { File(it) }?.takeIf { it.exists() }?.name, p.createdAt)
        }
        val gameData = games.map { g ->
            GameBackupData(g.id, g.name, g.date, g.durationSeconds, g.expansions,
                g.photoPath?.let { File(it) }?.takeIf { it.exists() }?.name)
        }
        val gpData = gamePlayers.map { gp ->
            GamePlayerBackupData(gp.gameId, gp.playerId, gp.meepleColor, gp.finalScore,
                gp.cityPoints, gp.roadPoints, gp.monasteryPoints, gp.farmPoints, gp.placement)
        }
        val payload = BackupPayload(players = playerData, games = gameData, gamePlayers = gpData)
        val encBytes = xorCrypt(json.encodeToString(payload).toByteArray(Charsets.UTF_8))

        context.contentResolver.openOutputStream(uri)?.use { os ->
            writeBackupZip(os, encBytes, photoFiles)
        } ?: throw IllegalStateException("Cannot open output stream for URI")
    }

    fun restoreBackupFromUri(context: Context, uri: Uri): RestoreResult {
        val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for URI")
        return parseBackupStream(inputStream, photosDir)
    }

    // ─── Shared write helper ──────────────────────────────────────────────────

    private fun writeBackupZip(out: OutputStream, encData: ByteArray, photoFiles: Map<String, File>) {
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry(DATA_ENTRY))
            zos.write(encData)
            zos.closeEntry()
            photoFiles.forEach { (name, file) ->
                zos.putNextEntry(ZipEntry("$PHOTOS_PREFIX$name"))
                FileInputStream(file).use { it.copyTo(zos) }
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
                        payload = json.decodeFromString(String(xorCrypt(zis.readBytes()), Charsets.UTF_8))
                    }
                    entry.name.startsWith(PHOTOS_PREFIX) -> {
                        val fname = entry.name.removePrefix(PHOTOS_PREFIX)
                        if (fname.isNotEmpty()) FileOutputStream(File(photosDir, fname)).use { zis.copyTo(it) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        val p = payload ?: throw IllegalStateException("Corrupt or invalid backup file")
        val players = p.players.map { pd ->
            PlayerEntity(id = pd.id, name = pd.name, meepleColor = pd.meepleColor,
                avatarPath = pd.avatarFile?.let { File(photosDir, it).absolutePath }, createdAt = pd.createdAt)
        }
        val games = p.games.map { gd ->
            GameEntity(id = gd.id, name = gd.name, date = gd.date, durationSeconds = gd.durationSeconds,
                expansions = gd.expansions, photoPath = gd.photoFile?.let { File(photosDir, it).absolutePath })
        }
        val gamePlayers = p.gamePlayers.map { gp ->
            GamePlayerEntity(gameId = gp.gameId, playerId = gp.playerId, meepleColor = gp.meepleColor,
                finalScore = gp.finalScore, cityPoints = gp.cityPoints, roadPoints = gp.roadPoints,
                monasteryPoints = gp.monasteryPoints, farmPoints = gp.farmPoints, placement = gp.placement)
        }
        return RestoreResult(players, games, gamePlayers)
    }

    // ─── Restore backup ───────────────────────────────────────────────────────

    data class RestoreResult(
        val players: List<PlayerEntity>,
        val games: List<GameEntity>,
        val gamePlayers: List<GamePlayerEntity>
    )

    fun restoreBackup(context: Context, file: File): RestoreResult {
        val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
        var payload: BackupPayload? = null

        FileInputStream(file).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == DATA_ENTRY -> {
                            val encBytes = zis.readBytes()
                            val jsonStr = String(xorCrypt(encBytes), Charsets.UTF_8)
                            payload = json.decodeFromString(jsonStr)
                        }
                        entry.name.startsWith(PHOTOS_PREFIX) -> {
                            val fname = entry.name.removePrefix(PHOTOS_PREFIX)
                            if (fname.isNotEmpty()) {
                                FileOutputStream(File(photosDir, fname)).use { zis.copyTo(it) }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        val p = payload ?: throw IllegalStateException("Corrupt or invalid backup file")

        val players = p.players.map { pd ->
            PlayerEntity(
                id = pd.id,
                name = pd.name,
                meepleColor = pd.meepleColor,
                avatarPath = pd.avatarFile?.let { File(photosDir, it).absolutePath },
                createdAt = pd.createdAt
            )
        }
        val games = p.games.map { gd ->
            GameEntity(
                id = gd.id,
                name = gd.name,
                date = gd.date,
                durationSeconds = gd.durationSeconds,
                expansions = gd.expansions,
                photoPath = gd.photoFile?.let { File(photosDir, it).absolutePath }
            )
        }
        val gamePlayers = p.gamePlayers.map { gp ->
            GamePlayerEntity(
                gameId = gp.gameId,
                playerId = gp.playerId,
                meepleColor = gp.meepleColor,
                finalScore = gp.finalScore,
                cityPoints = gp.cityPoints,
                roadPoints = gp.roadPoints,
                monasteryPoints = gp.monasteryPoints,
                farmPoints = gp.farmPoints,
                placement = gp.placement
            )
        }
        return RestoreResult(players, games, gamePlayers)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun xorCrypt(data: ByteArray): ByteArray =
        ByteArray(data.size) { i ->
            (data[i].toInt() xor XOR_KEY[i % XOR_KEY.size].toInt()).toByte()
        }

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
