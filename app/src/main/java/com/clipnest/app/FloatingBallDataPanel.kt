package com.clipnest.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.clipnest.app.data.ClipNestDatabase
import com.clipnest.app.data.MaterialEntryEntity
import com.clipnest.app.data.MaterialFieldEntity
import com.clipnest.app.data.TopicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Root X 全局资料浏览器
 * 主题 -> 资料条目 -> 字段
 * 直接读取现有 clipnest.db，不创建新数据库。
 */
class FloatingBallDataPanel(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val database = ClipNestDatabase.create(context)
    private val dao = database.dao()

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var topicsJob: Job? = null
    private var panel: LinearLayout? = null
    private var body: LinearLayout? = null

    private var currentTopic: TopicEntity? = null

    private val density = context.resources.displayMetrics.density

    private val ink = Color.rgb(24, 25, 29)
    private val cream = Color.rgb(250, 246, 237)
    private val yellow = Color.rgb(255, 213, 74)
    private val muted = Color.rgb(145, 148, 157)

    fun showTopics() {
        currentTopic = null
        topicsJob?.cancel()

        createShell(
            title = "我的资料",
            subtitle = "选择一个主题",
            showBack = false
        )

        topicsJob = scope.launch {
            dao.observeTopics().collect { topics ->
                renderTopics(topics)
            }
        }
    }

    private fun renderTopics(topics: List<TopicEntity>) {
        val target = body ?: return
        target.removeAllViews()

        if (topics.isEmpty()) {
            addEmpty(target, "还没有主题", "先在 ClipNest 主应用中创建主题。")
            return
        }

        topics.forEach { topic ->
            addRow(
                target = target,
                title = topic.title,
                subtitle = topic.description.ifBlank { "点击查看资料条目" },
                tag = if (topic.isPinned) "置顶" else null
            ) {
                openTopic(topic)
            }
        }
    }

    private fun openTopic(topic: TopicEntity) {
        currentTopic = topic
        topicsJob?.cancel()

        createShell(
            title = topic.title,
            subtitle = "主题中的资料条目",
            showBack = true,
            onBack = { showTopics() }
        )

        scope.launch {
            val entries = dao.getEntriesForTopic(topic.id)
                .filter { !it.isDeleted }

            renderEntries(topic, entries)
        }
    }

    private fun renderEntries(
        topic: TopicEntity,
        entries: List<MaterialEntryEntity>
    ) {
        val target = body ?: return
        target.removeAllViews()

        if (entries.isEmpty()) {
            addEmpty(target, "这个主题还没有资料", "可以回到主应用添加资料条目。")
            return
        }

        entries.forEach { entry ->
            addRow(
                target = target,
                title = entry.title,
                subtitle = entry.description.ifBlank { "点击查看字段内容" }
            ) {
                openEntry(topic, entry)
            }
        }
    }

    private fun openEntry(
        topic: TopicEntity,
        entry: MaterialEntryEntity
    ) {
        createShell(
            title = entry.title,
            subtitle = "字段详情 · 点击内容即可复制",
            showBack = true,
            onBack = { openTopic(topic) }
        )

        scope.launch {
            val fields = dao.getFieldsForEntry(entry.id)
            renderFields(fields)
        }
    }

    private fun renderFields(fields: List<MaterialFieldEntity>) {
        val target = body ?: return
        target.removeAllViews()

        if (fields.isEmpty()) {
            addEmpty(target, "暂无字段", "这个资料条目还没有添加字段。")
            return
        }

        fields.forEach { field ->
            addField(target, field)
        }

        target.addView(
            addText(
                target,
                "点击字段内容复制到系统剪贴板",
                11,
                muted,
                false
            ).apply {
                setPadding(4, dp(12), 4, dp(4))
            }
        )
    }

    private fun addField(
        target: LinearLayout,
        field: MaterialFieldEntity
    ) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = rounded(Color.WHITE, dp(13), ink)
        }

        val name = addText(card, field.fieldName, 12, Color.rgb(105, 105, 105), true)

        val value = addText(
            card,
            field.fieldValue.ifBlank { "（空内容）" },
            14,
            ink,
            false
        ).apply {
            setPadding(0, dp(7), 0, dp(5))
            maxLines = 6
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val copy = addText(card, "复制内容  ↗", 11, Color.rgb(130, 100, 0), true)
            .apply {
                gravity = Gravity.END
                setPadding(0, dp(5), 0, 0)
            }

        card.addView(name)
        card.addView(value)
        card.addView(copy)

        val copyAction = View.OnClickListener {
            copyValue(field.fieldValue)
        }

        value.setOnClickListener(copyAction)
        copy.setOnClickListener(copyAction)

        target.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )
    }

    private fun createShell(
        title: String,
        subtitle: String,
        showBack: Boolean,
        onBack: (() -> Unit)? = null
    ) {
        removeWindow()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(14), dp(17), dp(14))
            background = rounded(
                Color.rgb(28, 29, 34),
                dp(23),
                Color.argb(150, 255, 255, 255)
            )
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        if (showBack && onBack != null) {
            header.addView(
                actionText("‹", 25) { onBack() },
                LinearLayout.LayoutParams(dp(38), dp(42))
            )
        }

        val titleBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleBox.addView(
            addText(header = titleBox, text = "CLIPNEST / ROOT X", size = 9, color = yellow, bold = true)
        )

        titleBox.addView(
            addText(titleBox, title, 19, Color.WHITE, true)
        )

        header.addView(
            titleBox,
            LinearLayout.LayoutParams(0, dp(54), 1f)
        )

        header.addView(
            actionText("×", 23) { dismiss() },
            LinearLayout.LayoutParams(dp(38), dp(42))
        )

        root.addView(header)

        root.addView(
            addText(root, subtitle, 11, muted, false).apply {
                setPadding(0, 0, 0, dp(10))
            }
        )

        val divider = View(context).apply {
            setBackgroundColor(Color.argb(55, 255, 255, 255))
        }
        root.addView(
            divider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(12), 0, dp(4))
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(410)
            )
        )

        body = content
        panel = root

        val params = WindowManager.LayoutParams(
            minOf(
                context.resources.displayMetrics.widthPixels - dp(28),
                dp(380)
            ),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(root, params)
        } catch (_: Exception) {
            panel = null
            body = null
            Toast.makeText(context, "资料面板打开失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addRow(
        target: LinearLayout,
        title: String,
        subtitle: String,
        tag: String? = null,
        onClick: () -> Unit
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(cream, dp(14), Color.argb(45, 255, 255, 255))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        top.addView(
            addText(top, title, 15, ink, true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        if (tag != null) {
            top.addView(
                addText(top, tag, 10, ink, true).apply {
                    setPadding(dp(7), dp(3), dp(7), dp(3))
                    background = rounded(yellow, dp(6), yellow)
                }
            )
        }

        row.addView(top)
        row.addView(
            addText(row, subtitle, 11, Color.rgb(105, 105, 105), false).apply {
                setPadding(0, dp(5), 0, 0)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        )

        target.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )
    }

    private fun addEmpty(
        target: LinearLayout,
        title: String,
        subtitle: String
    ) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(48), dp(12), dp(48))
        }

        box.addView(addText(box, title, 16, Color.WHITE, true))
        box.addView(
            addText(box, subtitle, 11, muted, false).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            }
        )

        target.addView(box)
    }

    private fun addText(
        header: LinearLayout,
        text: String,
        size: Int,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun actionText(
        text: String,
        size: Int,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(yellow)
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun rounded(
        color: Int,
        radius: Int,
        strokeColor: Int
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        setStroke(dp(1), strokeColor)
    }

    private fun copyValue(value: String) {
        val manager = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        manager.setPrimaryClip(
            ClipData.newPlainText("ClipNest", value)
        )

        Toast.makeText(context, "已复制到系统剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun dismiss() {
        topicsJob?.cancel()
        removeWindow()
    }

    private fun removeWindow() {
        panel?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        panel = null
        body = null
    }

    fun destroy() {
        dismiss()
        scope.cancel()
        database.close()
    }

    private fun dp(value: Int): Int =
        (value * density).toInt()
}
