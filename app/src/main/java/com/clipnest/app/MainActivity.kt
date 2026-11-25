package com.clipnest.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.clipnest.app.data.*
import kotlinx.coroutines.launch

private val Ink = Color(0xFF111111)
private val Muted = Color(0xFF555555)
private val Accent = Color(0xFFFFC900)
private val Canvas = Color(0xFFFFF6E9)
private val Card = Color(0xFFFFFFFF)
private val Sky = Color(0xFF7FBCFF)
private val Pink = Color(0xFFFF6B9D)
private val Lime = Color(0xFFB5F56D)

private fun Modifier.brutalPanel(shadow: androidx.compose.ui.unit.Dp = 5.dp): Modifier =
    drawBehind {
        val distance = shadow.toPx()
        drawRect(color = Ink, topLeft = Offset(distance, distance), size = size)
    }

class MainActivity : ComponentActivity() {
    private lateinit var dao: ClipNestDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = ClipNestDatabase.create(this).dao()
        setContent { ClipNestApp(dao) }
    }


}

@Composable
private fun ClipNestApp(dao: ClipNestDao) {
    val nav = rememberNavController()
    val items = listOf(
        Triple("首页", "home", Icons.Default.Home),
        Triple("剪贴板", "clips", Icons.Default.ContentPaste),
        Triple("分类", "categories", Icons.Default.Category),
        Triple("主题", "topics", Icons.Default.FolderOpen),
        Triple("收藏", "saved", Icons.Default.Bookmark)
    )
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            secondary = Sky,
            tertiary = Pink,
            background = Canvas,
            surface = Card,
            onPrimary = Ink,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                NavigationBar(containerColor = Accent, contentColor = Ink, modifier = Modifier.border(2.dp, Ink, RectangleShape)) {
                    val current = nav.currentBackStackEntryAsState().value?.destination?.route
                    items.forEach { (label, route, icon) ->
                        NavigationBarItem(
                            selected = current == route,
                            onClick = { nav.navigate(route) { launchSingleTop = true } },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { HomeScreen(dao, onNavigate = { nav.navigate(it) }) }
                composable("clips") { ClipsScreen(dao, savedOnly = false) }
                composable("categories") { CategoriesScreen(dao) }
                composable("topics") { TopicsScreen(dao) }
                composable("saved") { ClipsScreen(dao, savedOnly = true) }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        action?.invoke()
    }
}

@Composable
private fun HomeScreen(dao: ClipNestDao, onNavigate: (String) -> Unit) {
    val clips by dao.observeClips().collectAsStateWithLifecycle(initialValue = emptyList())
    val topics by dao.observeTopics().collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 22.dp)) {
        PageHeader("ClipNest", "收集 · 整理 · 永久保存") {
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "添加", tint = Accent, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索内容、主题或分类…", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
            singleLine = true,
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Card, focusedContainerColor = Card,
                unfocusedBorderColor = Ink, focusedBorderColor = Ink
            )
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("剪贴板", clips.size.toString(), Icons.Default.ContentPaste, Modifier.weight(1f))
            StatCard("主题空间", topics.size.toString(), Icons.Default.FolderOpen, Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle("快捷入口")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("全部记录", Icons.Default.List, Modifier.weight(1f)) { onNavigate("clips") }
            QuickAction("主题空间", Icons.Default.Folder, Modifier.weight(1f)) { onNavigate("topics") }
            QuickAction("收藏内容", Icons.Default.Bookmark, Modifier.weight(1f)) { onNavigate("saved") }
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle("最近保存")
        if (clips.isEmpty()) {
            EmptyState("还没有内容", "复制或手动添加一段内容，开始建立你的资料库。")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(clips.filter { it.content.contains(search, true) }.take(8), key = { it.id }) {
                    ClipCard(it, dao)
                }
            }
        }
    }
    if (showAdd) AddClipDialog(
        onDismiss = { showAdd = false },
        onSave = { content ->
            scope.launch {
                val (type, category) = ClipClassifier.detect(content)
                dao.insertClip(ClipEntity(content = content, detectedType = type, category = category))
                showAdd = false
            }
        },
        onPaste = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        }
    )
}

@Composable
private fun StatCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.brutalPanel().border(2.dp, Ink, RectangleShape), shape = RectangleShape, color = Card, shadowElevation = 0.dp) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.height(12.dp))
            Text(count, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick).brutalPanel(4.dp).border(2.dp, Ink, RectangleShape), color = Card, shape = RectangleShape) {
        Column(Modifier.padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.height(7.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFE9ECFF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ContentPaste, null, tint = Accent, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(detail, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ClipsScreen(dao: ClipNestDao, savedOnly: Boolean) {
    val all by (if (savedOnly) dao.observeSavedClips() else dao.observeClips())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 22.dp)) {
        PageHeader(if (savedOnly) "收藏与永久保存" else "剪贴板", "${all.size} 条内容") {
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null, tint = Accent) }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("搜索剪贴板内容") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RectangleShape)
        )
        Spacer(Modifier.height(12.dp))
        if (all.isEmpty()) EmptyState("这里还很安静", "添加内容后，它们会整齐地保存在这里。")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(all.filter { it.content.contains(query, true) }, key = { it.id }) { ClipCard(it, dao) }
        }
    }
    if (showAdd) AddClipDialog(
        onDismiss = { showAdd = false },
        onSave = { content ->
            scope.launch {
                val (type, category) = ClipClassifier.detect(content)
                dao.insertClip(ClipEntity(content = content, detectedType = type, category = category,
                    isFavorite = savedOnly, isPermanent = savedOnly))
                showAdd = false
            }
        },
        onPaste = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        }
    )
}

@Composable
private fun ClipCard(item: ClipEntity, dao: ClipNestDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Surface(color = Card, shape = RectangleShape, shadowElevation = 0.dp, modifier = Modifier.brutalPanel(5.dp).border(2.dp, Ink, RectangleShape)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFEDEFFF), shape = RoundedCornerShape(8.dp)) {
                    Text(item.detectedType, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Accent, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    scope.launch { dao.updateClip(item.copy(isFavorite = !item.isFavorite, isPermanent = item.isPermanent || !item.isFavorite)) }
                }) {
                    Icon(if (item.isFavorite || item.isPermanent) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        null, tint = Accent)
                }
                IconButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("ClipNest", item.content))
                }) { Icon(Icons.Default.ContentCopy, null, tint = Muted) }
                IconButton(onClick = { scope.launch { dao.deleteClip(item) } }) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Muted)
                }
            }
            Text(item.content, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("${item.category} · ${java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.createdAt))}",
                color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AddClipDialog(onDismiss: () -> Unit, onSave: (String) -> Unit, onPaste: () -> String) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到 ClipNest") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("内容") }, minLines = 3)
                TextButton(onClick = { text = onPaste() }) { Icon(Icons.Default.ContentPaste, null); Spacer(Modifier.width(6.dp)); Text("读取当前剪贴板") }
            }
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun CategoriesScreen(dao: ClipNestDao) {
    val clips by dao.observeClips().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories = listOf("链接", "手机号", "验证码", "邮箱", "代码", "文本", "未分类")
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("智能分类", "根据内容特征自动识别")
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { name ->
                val count = clips.count { it.category == name }
                Surface(color = Card, shape = RectangleShape, modifier = Modifier.border(2.dp, Ink, RectangleShape)) {
                    Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = Accent)
                        Spacer(Modifier.width(14.dp))
                        Text(name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("$count 条", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicsScreen(dao: ClipNestDao) {
    val topics by dao.observeTopics().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<TopicEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TopicEntity?>(null) }
    var pendingEdit by remember { mutableStateOf<TopicEntity?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("主题空间", "把相关资料组织成自己的知识盒子")
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(title, { title = it }, modifier = Modifier.weight(1f), label = { Text("新主题名称") }, singleLine = true)
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = {
                if (title.isNotBlank()) {
                    scope.launch { dao.insertTopic(TopicEntity(title = title.trim(), description = description.trim())); title = ""; description = "" }
                }
            }) { Icon(Icons.Default.Add, null) }
        }
        Spacer(Modifier.height(14.dp))
        if (topics.isEmpty()) EmptyState("创建第一个主题", "例如：服务器资料、工作项目、活动报名。")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(topics, key = { it.id }) { topic ->
                Surface(color = Card, shape = RectangleShape, modifier = Modifier.fillMaxWidth().brutalPanel(5.dp).border(2.dp, Ink, RectangleShape).clickable { selected = topic }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFEDEFFF), shape = RoundedCornerShape(13.dp)) {
                            Icon(Icons.Default.FolderOpen, null, tint = Accent, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(topic.title, fontWeight = FontWeight.SemiBold)
                            Text(topic.description.ifBlank { "点击管理主题字段" }, color = Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { pendingEdit = topic }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑主题", tint = Muted)
                        }
                        IconButton(onClick = { pendingDelete = topic }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "删除主题", tint = Muted)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Muted)
                    }
                }
            }
        }
    }
    selected?.let { topic ->
        TopicEditorDialog(topic, dao, onDismiss = { selected = null })
    }
    pendingEdit?.let { topic ->
        EditTopicDialog(
            topic = topic,
            onDismiss = { pendingEdit = null },
            onSave = { newTitle, newDescription ->
                scope.launch { dao.updateTopic(topic.copy(title = newTitle, description = newDescription)) }
                pendingEdit = null
            }
        )
    }
    pendingDelete?.let { topic ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除主题") },
            text = { Text("确定删除「${topic.title}」吗？该主题下的所有字段也会一并删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteFieldsForTopic(topic.id); dao.deleteTopic(topic) }
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun TopicEditorDialog(topic: TopicEntity, dao: ClipNestDao, onDismiss: () -> Unit) {
    val fields by dao.observeFields(topic.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember(topic.id) { mutableStateOf("") }
    var value by remember(topic.id) { mutableStateOf("") }
    var editingField by remember { mutableStateOf<TopicFieldEntity?>(null) }
    fun addField() {
        val n = name.trim()
        val v = value.trim()
        if (n.isBlank()) return
        val nextOrder = fields.size
        scope.launch { dao.insertField(TopicFieldEntity(topicId = topic.id, fieldName = n, fieldValue = v, sortOrder = nextOrder)) }
        name = ""; value = ""
    }
    fun copyText(text: String, toast: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ClipNest", text))
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }
    fun moveField(index: Int, delta: Int) {
        val target = index + delta
        if (target < 0 || target >= fields.size) return
        val field = fields[index]
        val other = fields[target]
        scope.launch {
            dao.updateField(field.copy(sortOrder = other.sortOrder))
            dao.updateField(other.copy(sortOrder = field.sortOrder))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(topic.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("主题字段", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                fields.forEachIndexed { index, field ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(field.fieldName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(field.fieldValue, fontSize = 13.sp)
                            }
                            IconButton(onClick = { copyText(field.fieldValue, "已复制") }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制该字段", tint = Muted)
                            }
                            IconButton(onClick = { editingField = field }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑字段", tint = Muted)
                            }
                            IconButton(onClick = { scope.launch { dao.deleteField(field) } }) {
                                Icon(Icons.Default.Close, contentDescription = "删除字段", tint = Muted)
                            }
                        }
                        if (fields.size > 1) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { moveField(index, -1) }, enabled = index > 0) { Text("上移") }
                                TextButton(onClick = { moveField(index, 1) }, enabled = index < fields.lastIndex) { Text("下移") }
                            }
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(name, { name = it }, label = { Text("字段名称") }, singleLine = true)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value, { value = it }, label = { Text("字段内容") }, minLines = 2)
                TextButton(onClick = { addField() }) { Icon(Icons.Default.Add, null); Text("添加字段") }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        if (fields.isNotEmpty()) {
                            copyText(fields.joinToString("\n") { "${it.fieldName}：${it.fieldValue}" }, "已复制全部字段")
                        }
                    },
                    enabled = fields.isNotEmpty()
                ) { Text("复制全部") }
                TextButton(onClick = { addField(); onDismiss() }) { Text("完成") }
            }
        }
    )
    editingField?.let { field ->
        EditFieldDialog(
            field = field,
            onDismiss = { editingField = null },
            onSave = { newName, newValue ->
                scope.launch { dao.updateField(field.copy(fieldName = newName, fieldValue = newValue)) }
                editingField = null
            }
        )
    }
}

@Composable
private fun EditTopicDialog(topic: TopicEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember(topic.id) { mutableStateOf(topic.title) }
    var description by remember(topic.id) { mutableStateOf(topic.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑主题") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("主题名称") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(description, { description = it }, label = { Text("主题说明") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title.trim(), description.trim()) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditFieldDialog(field: TopicFieldEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember(field.id) { mutableStateOf(field.fieldName) }
    var value by remember(field.id) { mutableStateOf(field.fieldValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑字段") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("字段名称") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value, { value = it }, label = { Text("字段内容") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), value.trim()) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
