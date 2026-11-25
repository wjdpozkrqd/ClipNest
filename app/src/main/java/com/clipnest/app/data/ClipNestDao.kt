package com.clipnest.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipNestDao {

    // 剪贴板
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

    // 第一层：主题
    @Query("SELECT * FROM topics ORDER BY createdAt DESC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Insert
    suspend fun insertTopic(item: TopicEntity): Long

    @Update
    suspend fun updateTopic(item: TopicEntity)

    @Delete
    suspend fun deleteTopic(item: TopicEntity)

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: Long): TopicEntity?

    @Query("SELECT * FROM material_entries WHERE topicId = :topicId ORDER BY sortOrder, createdAt DESC")
    suspend fun getEntriesForTopic(topicId: Long): List<MaterialEntryEntity>

    @Query("SELECT * FROM material_fields WHERE entryId = :entryId ORDER BY sortOrder, id")
    suspend fun getFieldsForEntry(entryId: Long): List<MaterialFieldEntity>

    /**
     * 深复制整个主题：主题、资料条目、字段全部生成新 ID。
     */
    @Transaction
    suspend fun duplicateTopicWithContents(sourceTopicId: Long): Long? {
        val source = getTopicById(sourceTopicId) ?: return null

        val newTopicId = insertTopic(
            source.copy(
                id = 0,
                title = "${source.title} 副本",
                createdAt = System.currentTimeMillis()
            )
        )

        val entries = getEntriesForTopic(sourceTopicId)

        entries.forEach { entry ->
            val newEntryId = insertEntry(
                entry.copy(
                    id = 0,
                    topicId = newTopicId,
                    createdAt = System.currentTimeMillis()
                )
            )

            val fields = getFieldsForEntry(entry.id)

            fields.forEach { field ->
                insertField(
                    field.copy(
                        id = 0,
                        entryId = newEntryId
                    )
                )
            }
        }

        return newTopicId
    }

    // 第二层：资料条目
    @Query("SELECT * FROM material_entries WHERE topicId = :topicId ORDER BY sortOrder, createdAt DESC")
    fun observeEntries(topicId: Long): Flow<List<MaterialEntryEntity>>

    @Query("SELECT * FROM material_entries WHERE id = :entryId LIMIT 1")
    fun observeEntry(entryId: Long): Flow<MaterialEntryEntity?>

    @Query("SELECT * FROM material_entries WHERE id = :entryId LIMIT 1")
    suspend fun getEntryById(entryId: Long): MaterialEntryEntity?

    @Insert
    suspend fun insertEntry(item: MaterialEntryEntity): Long

    @Update
    suspend fun updateEntry(item: MaterialEntryEntity)

    @Delete
    suspend fun deleteEntry(item: MaterialEntryEntity)

    /**
     * 深复制单个资料条目及其全部字段。
     */
    @Transaction
    suspend fun duplicateEntryWithFields(sourceEntryId: Long): Long? {
        val source = getEntryById(sourceEntryId) ?: return null

        val newEntryId = insertEntry(
            source.copy(
                id = 0,
                title = "${source.title} 副本",
                createdAt = System.currentTimeMillis()
            )
        )

        val fields = getFieldsForEntry(sourceEntryId)

        fields.forEach { field ->
            insertField(
                field.copy(
                    id = 0,
                    entryId = newEntryId
                )
            )
        }

        return newEntryId
    }

    // 第三层：资料字段
    @Query("SELECT * FROM material_fields WHERE entryId = :entryId ORDER BY sortOrder, id")
    fun observeFields(entryId: Long): Flow<List<MaterialFieldEntity>>

    @Insert
    suspend fun insertField(item: MaterialFieldEntity): Long

    @Update
    suspend fun updateField(item: MaterialFieldEntity)

    @Delete
    suspend fun deleteField(item: MaterialFieldEntity)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 创建资料条目表
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `material_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`topicId` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, " +
                "FOREIGN KEY(`topicId`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_material_entries_topicId` ON `material_entries` (`topicId`)")

        // 创建资料字段表
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `material_fields` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`entryId` INTEGER NOT NULL, " +
                "`fieldName` TEXT NOT NULL, " +
                "`fieldValue` TEXT NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, " +
                "FOREIGN KEY(`entryId`) REFERENCES `material_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_material_fields_entryId` ON `material_fields` (`entryId`)")

        // 保留旧数据：给每个已有字段的主题建一个默认条目，把它的字段整体搬过去
        db.execSQL(
            "INSERT INTO material_entries (topicId, title, description, createdAt, sortOrder) " +
                "SELECT id, '默认资料', '', createdAt, 0 FROM topics " +
                "WHERE id IN (SELECT DISTINCT topicId FROM topic_fields)"
        )
        db.execSQL(
            "INSERT INTO material_fields (entryId, fieldName, fieldValue, sortOrder) " +
                "SELECT e.id, f.fieldName, f.fieldValue, f.sortOrder FROM topic_fields f " +
                "JOIN material_entries e ON e.topicId = f.topicId"
        )

        // 旧字段表使命完成，删除
        db.execSQL("DROP TABLE IF EXISTS topic_fields")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 剪贴板：置顶、排序、最近使用、软删除
        db.execSQL(
            "ALTER TABLE `clips` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `clips` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `clips` ADD COLUMN `lastUsedAt` INTEGER DEFAULT NULL"
        )
        db.execSQL(
            "ALTER TABLE `clips` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `clips` ADD COLUMN `deletedAt` INTEGER DEFAULT NULL"
        )

        // 主题：置顶、排序、最近打开、软删除
        db.execSQL(
            "ALTER TABLE `topics` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `topics` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `topics` ADD COLUMN `lastOpenedAt` INTEGER DEFAULT NULL"
        )
        db.execSQL(
            "ALTER TABLE `topics` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `topics` ADD COLUMN `deletedAt` INTEGER DEFAULT NULL"
        )

        // 资料条目：最近使用、软删除
        db.execSQL(
            "ALTER TABLE `material_entries` ADD COLUMN `lastUsedAt` INTEGER DEFAULT NULL"
        )
        db.execSQL(
            "ALTER TABLE `material_entries` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `material_entries` ADD COLUMN `deletedAt` INTEGER DEFAULT NULL"
        )

        // 主题索引：置顶、排序、最近打开
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_topics_isDeleted_isPinned_sortOrder_lastOpenedAt` " +
                "ON `topics` (`isDeleted`, `isPinned`, `sortOrder`, `lastOpenedAt`)"
        )

        // 剪贴板索引：软删除、置顶、排序、最近使用
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_clips_isDeleted_isPinned_sortOrder_lastUsedAt` " +
                "ON `clips` (`isDeleted`, `isPinned`, `sortOrder`, `lastUsedAt`)"
        )

        // 资料条目索引：主题、软删除、排序、最近使用
        // 复合索引以 topicId 开头，可以覆盖原来的 topicId 单列索引用途
        db.execSQL(
            "DROP INDEX IF EXISTS `index_material_entries_topicId`"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_material_entries_topicId_isDeleted_sortOrder_lastUsedAt` " +
                "ON `material_entries` (`topicId`, `isDeleted`, `sortOrder`, `lastUsedAt`)"
        )
    }
}

@Database(
    entities = [ClipEntity::class, TopicEntity::class, MaterialEntryEntity::class, MaterialFieldEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ClipNestDatabase : RoomDatabase() {
    abstract fun dao(): ClipNestDao

    companion object {
        fun create(context: Context): ClipNestDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ClipNestDatabase::class.java,
                "clipnest.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
