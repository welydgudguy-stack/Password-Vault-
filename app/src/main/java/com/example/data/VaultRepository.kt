package com.example.data

import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import javax.crypto.SecretKey

class VaultRepository(
    private val dao: VaultItemDao,
    private val preferences: VaultPreferences
) {
    private var sessionKey: SecretKey? = null

    val allItems: Flow<List<VaultItemEntity>> = dao.getAllItems()

    fun searchItems(query: String): Flow<List<VaultItemEntity>> {
        return if (query.isBlank()) dao.getAllItems() else dao.searchItems(query)
    }

    fun isUnlocked(): Boolean = sessionKey != null

    fun unlockWithPassword(password: String): Boolean {
        val salt = preferences.getSalt() ?: return false
        val storedHash = preferences.masterPasswordHash ?: return false
        val isValid = CryptoManager.verifyPassword(password, storedHash, salt)
        if (isValid) {
            sessionKey = CryptoManager.deriveKey(password, salt)
        }
        return isValid
    }

    fun unlockWithBiometric(): Boolean {
        // When biometric is verified, we derive or restore session
        val salt = preferences.getSalt() ?: return false
        // For biometric session unlock without retyping master password,
        // we can derive a temporary or use master session if initialized
        // Or if first time with biometric, derive from cached state or sample
        if (sessionKey == null) {
            // Biometric verified unlock creates session
            sessionKey = CryptoManager.deriveKey("VAULTKEEP_BIOMETRIC_SESSION_ROOT", salt)
        }
        return true
    }

    fun lock() {
        sessionKey = null
    }

    fun setupMasterPassword(password: String) {
        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPassword(password, salt)
        preferences.setMasterPassword(hash, salt)
        sessionKey = CryptoManager.deriveKey(password, salt)
    }

    fun encryptPassword(plain: String): String {
        val key = sessionKey ?: return plain
        return CryptoManager.encrypt(plain, key)
    }

    fun decryptPassword(encrypted: String): String {
        val key = sessionKey ?: return "••••••••"
        val decrypted = CryptoManager.decrypt(encrypted, key)
        return if (decrypted.isNotEmpty()) decrypted else encrypted
    }

    suspend fun insertItem(
        title: String,
        website: String,
        username: String,
        passwordPlain: String,
        notes: String = "",
        category: String = "Social"
    ): Long {
        val encrypted = encryptPassword(passwordPlain)
        val entity = VaultItemEntity(
            title = title,
            website = website,
            username = username,
            encryptedPassword = encrypted,
            notes = notes,
            category = category,
            updatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        return dao.insertItem(entity)
    }

    suspend fun updateItem(item: VaultItemEntity) {
        dao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteItem(item: VaultItemEntity) {
        dao.deleteItem(item)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteItemById(id)
    }

    suspend fun toggleFavorite(item: VaultItemEntity) {
        dao.updateItem(item.copy(isFavorite = !item.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun populateSampleDataIfEmpty() {
        val items = dao.getAllItemsSync()
        if (items.isEmpty()) {
            val samples = listOf(
                Pair(
                    VaultItemEntity(
                        title = "Google",
                        website = "google.com",
                        username = "johndoe@gmail.com",
                        encryptedPassword = encryptPassword("G00gl3#P@ssw0rd!2026"),
                        notes = "Primary personal email and Google Drive storage.",
                        category = "Social",
                        isFavorite = true
                    ),
                    "G00gl3#P@ssw0rd!2026"
                ),
                Pair(
                    VaultItemEntity(
                        title = "Facebook",
                        website = "facebook.com",
                        username = "johndoe@facebook.com",
                        encryptedPassword = encryptPassword("Fb!Secur3&Lock88"),
                        notes = "Social profile login.",
                        category = "Social"
                    ),
                    "Fb!Secur3&Lock88"
                ),
                Pair(
                    VaultItemEntity(
                        title = "Instagram",
                        website = "instagram.com",
                        username = "johndoe@instagram.com",
                        encryptedPassword = encryptPassword("Insta#Vibes*9924"),
                        notes = "Photo sharing account.",
                        category = "Social"
                    ),
                    "Insta#Vibes*9924"
                ),
                Pair(
                    VaultItemEntity(
                        title = "Twitter / X",
                        website = "x.com",
                        username = "johndoe@x.com",
                        encryptedPassword = encryptPassword("X!Tw1tter#P0w3r2026"),
                        notes = "Tech and news feed.",
                        category = "Social"
                    ),
                    "X!Tw1tter#P0w3r2026"
                ),
                Pair(
                    VaultItemEntity(
                        title = "YouTube",
                        website = "youtube.com",
                        username = "johndoe@youtube.com",
                        encryptedPassword = encryptPassword("YTube*Premium#456"),
                        notes = "Video creator and subscriptions.",
                        category = "Entertainment"
                    ),
                    "YTube*Premium#456"
                ),
                Pair(
                    VaultItemEntity(
                        title = "GitHub",
                        website = "github.com",
                        username = "johndoe@github.com",
                        encryptedPassword = encryptPassword("Gh#Token_GitVault88"),
                        notes = "Code repositories and SSH keys.",
                        category = "Work",
                        isFavorite = true
                    ),
                    "Gh#Token_GitVault88"
                ),
                Pair(
                    VaultItemEntity(
                        title = "Netflix",
                        website = "netflix.com",
                        username = "johndoe@netflix.com",
                        encryptedPassword = encryptPassword("NetFl!x&Chill#2026"),
                        notes = "Streaming subscription account.",
                        category = "Entertainment"
                    ),
                    "NetFl!x&Chill#2026"
                )
            )

            for ((item, _) in samples) {
                dao.insertItem(item)
            }
        }
    }

    suspend fun exportBackupJson(): String {
        val items = dao.getAllItemsSync()
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("website", item.website)
            obj.put("username", item.username)
            obj.put("encryptedPassword", item.encryptedPassword)
            obj.put("notes", item.notes)
            obj.put("category", item.category)
            obj.put("isFavorite", item.isFavorite)
            obj.put("createdAt", item.createdAt)
            obj.put("updatedAt", item.updatedAt)
            jsonArray.put(obj)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "VaultKeep")
        root.put("exportDate", System.currentTimeMillis())
        root.put("items", jsonArray)
        return root.toString(2)
    }

    suspend fun exportCsv(): String {
        val items = dao.getAllItemsSync()
        val sb = StringBuilder()
        sb.append("Title,Website,Username,Password,Notes,Category\n")
        for (item in items) {
            val decrypted = decryptPassword(item.encryptedPassword)
            sb.append("\"${item.title.replace("\"", "\"\"")}\",")
            sb.append("\"${item.website.replace("\"", "\"\"")}\",")
            sb.append("\"${item.username.replace("\"", "\"\"")}\",")
            sb.append("\"${decrypted.replace("\"", "\"\"")}\",")
            sb.append("\"${item.notes.replace("\"", "\"\"")}\",")
            sb.append("\"${item.category.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    suspend fun importBackupJson(jsonString: String): Int {
        try {
            val root = JSONObject(jsonString)
            val array = root.getJSONArray("items")
            var count = 0
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val item = VaultItemEntity(
                    title = obj.optString("title", "Untitled"),
                    website = obj.optString("website", ""),
                    username = obj.optString("username", ""),
                    encryptedPassword = obj.optString("encryptedPassword", ""),
                    notes = obj.optString("notes", ""),
                    category = obj.optString("category", "General"),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                dao.insertItem(item)
                count++
            }
            return count
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }
}
