package com.clipnest.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val detectedType: String = "文本",
    val category: String = "未分类",
    val isFavorite: Boolean = false,
    val isPermanent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "topic_fields")
data class TopicFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val fieldName: String,
    val fieldValue: String,
    val sortOrder: Int = 0
)
