package com.clipnest.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clips",
    indices = [
        Index(value = ["isDeleted", "isPinned", "sortOrder", "lastUsedAt"])
    ]
)
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val detectedType: String = "文本",
    val category: String = "未分类",
    val isFavorite: Boolean = false,
    val isPermanent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),

    // V3：置顶、排序、最近使用、回收站
    @ColumnInfo(defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(defaultValue = "NULL")
    val lastUsedAt: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false,

    @ColumnInfo(defaultValue = "NULL")
    val deletedAt: Long? = null
)

@Entity(
    tableName = "topics",
    indices = [
        Index(value = ["isDeleted", "isPinned", "sortOrder", "lastOpenedAt"])
    ]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // V3：置顶、排序、最近打开、回收站
    @ColumnInfo(defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(defaultValue = "NULL")
    val lastOpenedAt: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false,

    @ColumnInfo(defaultValue = "NULL")
    val deletedAt: Long? = null
)

/**
 * 第二层：资料条目
 * 一个主题可以包含多个资料条目。
 */
@Entity(
    tableName = "material_entries",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topicId", "isDeleted", "sortOrder", "lastUsedAt"])
    ]
)
data class MaterialEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,

    // V3：最近使用、回收站
    @ColumnInfo(defaultValue = "NULL")
    val lastUsedAt: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false,

    @ColumnInfo(defaultValue = "NULL")
    val deletedAt: Long? = null
)

/**
 * 第三层：资料字段
 * 一个资料条目可以包含多个字段。
 */
@Entity(
    tableName = "material_fields",
    foreignKeys = [
        ForeignKey(
            entity = MaterialEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class MaterialFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val fieldName: String,
    val fieldValue: String,
    val sortOrder: Int = 0
)
