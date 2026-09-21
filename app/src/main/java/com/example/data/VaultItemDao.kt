package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    fun getItemById(id: Int): Flow<VaultItemEntity?>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItemByIdDirect(id: Int): VaultItemEntity?

    @Query("SELECT * FROM vault_items WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR website LIKE '%' || :query || '%' ORDER BY isFavorite DESC, updatedAt DESC")
    fun searchItems(query: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE website LIKE '%' || :domain || '%' OR title LIKE '%' || :domain || '%'")
    suspend fun findItemsForDomain(domain: String): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items")
    suspend fun getAllItemsSync(): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity): Long

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Delete
    suspend fun deleteItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}
