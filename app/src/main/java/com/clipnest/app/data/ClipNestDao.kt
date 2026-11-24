package com.clipnest.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipNestDao {
    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    fun observeClips(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE isFavorite = 1 OR isPermanent = 1 ORDER BY createdAt DESC")
    fun observeSavedClips(): Flow<List<ClipEntity>>

    @Insert
    suspend fun insertClip(item: ClipEntity): Long

    @Update
    suspend fun updateClip(item: ClipEntity)

    @Delete
    suspend fun deleteClip(item: ClipEntity)

    @Query("DELETE FROM clips")
    suspend fun clearClips()

    @Query("SELECT * FROM topics ORDER BY createdAt DESC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Insert
    suspend fun insertTopic(item: TopicEntity): Long

    @Update
    suspend fun updateTopic(item: TopicEntity)

    @Delete
    suspend fun deleteTopic(item: TopicEntity)

    @Query("SELECT * FROM topic_fields WHERE topicId = :topicId ORDER BY sortOrder, id")
    fun observeFields(topicId: Long): Flow<List<TopicFieldEntity>>

    @Insert
    suspend fun insertField(item: TopicFieldEntity)

    @Update
    suspend fun updateField(item: TopicFieldEntity)

    @Delete
    suspend fun deleteField(item: TopicFieldEntity)
}

@Database(
    entities = [ClipEntity::class, TopicEntity::class, TopicFieldEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ClipNestDatabase : RoomDatabase() {
    abstract fun dao(): ClipNestDao

    companion object {
        fun create(context: android.content.Context): ClipNestDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ClipNestDatabase::class.java,
                "clipnest.db"
            ).build()
    }
}
