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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
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

private fun Modifier.brutalPanel(
    shadow: androidx.compose.ui.unit.Dp = 5.dp
): Modifier = drawBehind {
    val distance = shadow.toPx()
    drawRect(
        color = Ink,
        topLeft = Offset(distance, distance),
        size = size
    )
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

    val bottomItems = listOf(
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
                NavigationBar(
                    containerColor = Accent,
                    contentColor = Ink,
                    modifier = Modifier.border(2.dp, Ink, RectangleShape)
                ) {
                    val current =
                        nav.currentBackStackEntryAsState().value?.destination?.route

                    bottomItems.forEach { (label, route, icon) ->
                        NavigationBarItem(
                            selected = current == route,
                            onClick = {
                                nav.navigate(route) {
                                    popUpTo("home") {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(dao) { nav.navigate(it) }
                }

                composable("clips") {
                    ClipsScreen(dao, savedOnly = false)
                }

                composable("categories") {
                    CategoriesScreen(dao)
                }

                composable("topics") {
                    TopicsScreen(
                        dao = dao,
                        onOpenTopic = { topicId ->
                            nav.navigate("topic_entries/$topicId")
                        }
                    )
                }

                composable(
                    route = "topic_entries/{topicId}",
                    arguments = listOf(
                        navArgument("topicId") { type = NavType.LongType }
                    )
                ) { entry ->
                    val topicId = entry.arguments?.getLong("topicId") ?: 0L

                    TopicEntriesScreen(
                        dao = dao,
                        topicId = topicId,
                        onBack = { nav.popBackStack() },
                        onOpenEntry = { entryId ->
                            nav.navigate("entry_detail/$entryId")
                        }
                    )
                }

                composable(
                    route = "entry_detail/{entryId}",
                    arguments = listOf(
                        navArgument("entryId") { type = NavType.LongType }
                    )
                ) { entry ->
                    val entryId = entry.arguments?.getLong("entryId") ?: 0L

                    EntryDetailScreen(
                        dao = dao,
                        entryId = entryId,
                        onBack = { nav.popBackStack() }
                    )
                }

                composable("saved") {
                    ClipsScreen(dao, savedOnly = true)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 通用页面组件
// ----------------------------------------------------

@Composable
private fun PageHeader(
    title: String,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Ink
            )
            if (subtitle != null) {
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
        }
        action?.invoke()
    }
}

@Composable
private fun BrutalCard(
    modifier: Modifier = Modifier,
    color: Color = Card,
    shadow: androidx.compose.ui.unit.Dp = 5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .brutalPanel(shadow)
            .border(2.dp, Ink, RectangleShape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        color = color,
        shape = RectangleShape,
        shadowElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Ink
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun EmptyState(
    title: String,
    detail: String,
    icon: ImageVector = Icons.Default.FolderOpen
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDEFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = Ink,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(13.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            detail,
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BrutalDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .brutalPanel(7.dp)
                .border(2.dp, Ink, RectangleShape),
            shape = RectangleShape,
            color = Card
        ) {
            Column(Modifier.fillMaxWidth()) {

                // 黄色标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Accent)
                        .padding(horizontal = 18.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "CLIPNEST / EDITOR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink.copy(alpha = 0.65f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            title,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Ink
                        )
                    }
                }

                // 表单内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Canvas)
                        .padding(18.dp)
                ) {
                    content()

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(
                        color = Ink.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
        }
    }
}

@Composable
private fun BrutalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        shape = RectangleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = Ink,
            focusedContainerColor = Card,
            unfocusedContainerColor = Card,
            cursorColor = Ink
        )
    )
}

@Composable
private fun BrutalActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.border(2.dp, Ink, RectangleShape),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Ink,
            disabledContainerColor = Color(0xFFE4DFD5),
            disabledContentColor = Muted
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
    message: String
) {
    val manager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    manager.setPrimaryClip(
        ClipData.newPlainText("ClipNest", text)
    )

    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

// ----------------------------------------------------
// 首页
// ----------------------------------------------------

@Composable
private fun HomeScreen(
    dao: ClipNestDao,
    onNavigate: (String) -> Unit
) {
    val clips by dao.observeClips()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val topics by dao.observeTopics()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp)
    ) {
        PageHeader("ClipNest", "收集 · 整理 · 永久保存") {
            IconButton(onClick = { showAdd = true }) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "添加",
                    tint = Accent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        BrutalTextField(
            value = search,
            onValueChange = { search = it },
            label = "搜索剪贴板内容"
        )

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                "剪贴板",
                clips.size.toString(),
                Icons.Default.ContentPaste,
                Modifier.weight(1f)
            )
            StatCard(
                "主题空间",
                topics.size.toString(),
                Icons.Default.FolderOpen,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionTitle("快捷入口")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(
                "全部记录",
                Icons.Default.List,
                Modifier.weight(1f)
            ) { onNavigate("clips") }

            QuickAction(
                "主题空间",
                Icons.Default.Folder,
                Modifier.weight(1f)
            ) { onNavigate("topics") }

            QuickAction(
                "收藏内容",
                Icons.Default.Bookmark,
                Modifier.weight(1f)
            ) { onNavigate("saved") }
        }

        Spacer(Modifier.height(22.dp))
        SectionTitle("最近保存")

        if (clips.isEmpty()) {
            EmptyState(
                "还没有内容",
                "复制或手动添加一段内容，开始建立你的资料库。",
                Icons.Default.ContentPaste
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    clips.filter { it.content.contains(search, true) }
                        .take(8),
                    key = { it.id }
                ) {
                    ClipCard(it, dao)
                }
            }
        }
    }

    if (showAdd) {
        AddClipDialog(
            onDismiss = { showAdd = false },
            onSave = { content ->
                scope.launch {
                    val (type, category) = ClipClassifier.detect(content)
                    dao.insertClip(
                        ClipEntity(
                            content = content,
                            detectedType = type,
                            category = category
                        )
                    )
                    showAdd = false
                }
            },
            onPaste = {
                val manager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                manager.primaryClip
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)
                    ?.coerceToText(context)
                    ?.toString()
                    .orEmpty()
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    BrutalCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.height(12.dp))
            Text(count, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(title, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    BrutalCard(
        modifier = modifier.clickable(onClick = onClick),
        shadow = 4.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.height(7.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// 剪贴板
// ----------------------------------------------------

@Composable
private fun ClipsScreen(
    dao: ClipNestDao,
    savedOnly: Boolean
) {
    val all by (
        if (savedOnly) dao.observeSavedClips()
        else dao.observeClips()
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp)
    ) {
        PageHeader(
            if (savedOnly) "收藏与永久保存" else "剪贴板",
            "${all.size} 条内容"
        ) {
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, null, tint = Accent)
            }
        }

        Spacer(Modifier.height(16.dp))

        BrutalTextField(
            value = query,
            onValueChange = { query = it },
            label = "搜索剪贴板内容"
        )

        Spacer(Modifier.height(12.dp))

        val filtered = all.filter { it.content.contains(query, true) }

        if (filtered.isEmpty()) {
            EmptyState(
                "这里还很安静",
                "添加内容后，它们会整齐地保存在这里。",
                Icons.Default.ContentPaste
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) {
                    ClipCard(it, dao)
                }
            }
        }
    }

    if (showAdd) {
        AddClipDialog(
            onDismiss = { showAdd = false },
            onSave = { content ->
                scope.launch {
                    val (type, category) = ClipClassifier.detect(content)

                    dao.insertClip(
                        ClipEntity(
                            content = content,
                            detectedType = type,
                            category = category,
                            isFavorite = savedOnly,
                            isPermanent = savedOnly
                        )
                    )

                    showAdd = false
                }
            },
            onPaste = {
                val manager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                manager.primaryClip
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)
                    ?.coerceToText(context)
                    ?.toString()
                    .orEmpty()
            }
        )
    }
}

@Composable
private fun ClipCard(
    item: ClipEntity,
    dao: ClipNestDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BrutalCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFEDEFFF),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        item.detectedType,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        color = Ink,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {
                        scope.launch {
                            dao.updateClip(
                                item.copy(
                                    isFavorite = !item.isFavorite,
                                    isPermanent = item.isPermanent || !item.isFavorite
                                )
                            )
                        }
                    }
                ) {
                    Icon(
                        if (item.isFavorite || item.isPermanent)
                            Icons.Default.Bookmark
                        else Icons.Default.BookmarkBorder,
                        null,
                        tint = Accent
                    )
                }

                IconButton(
                    onClick = {
                        copyToClipboard(context, item.content, "已复制")
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = Muted)
                }

                IconButton(
                    onClick = {
                        scope.launch { dao.deleteClip(item) }
                    }
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Muted)
                }
            }

            Text(
                item.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = Ink
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "${item.category} · ${
                    java.text.SimpleDateFormat(
                        "MM-dd HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(item.createdAt))
                }",
                color = Muted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun AddClipDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onPaste: () -> String
) {
    var text by remember { mutableStateOf("") }

    BrutalDialog(
        title = "添加到 ClipNest",
        onDismiss = onDismiss,
        content = {
            BrutalTextField(
                value = text,
                onValueChange = { text = it },
                label = "内容",
                singleLine = false,
                minLines = 4
            )

            TextButton(
                onClick = { text = onPaste() }
            ) {
                Icon(Icons.Default.ContentPaste, null)
                Spacer(Modifier.width(6.dp))
                Text("读取当前剪贴板", color = Ink)
            }
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Muted)
            }
            BrutalActionButton(
                text = "保存",
                enabled = text.isNotBlank(),
                onClick = { onSave(text.trim()) }
            )
        }
    )
}

// ----------------------------------------------------
// 分类
// ----------------------------------------------------

@Composable
private fun CategoriesScreen(dao: ClipNestDao) {
    val clips by dao.observeClips()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val categories = listOf(
        "链接", "手机号", "验证码", "邮箱", "代码", "文本", "未分类"
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        PageHeader("智能分类", "根据内容特征自动识别")
        Spacer(Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { name ->
                val count = clips.count { it.category == name }

                BrutalCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, tint = Accent)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            name,
                            Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                        Text("$count 条", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 第一层：主题空间
// ----------------------------------------------------

@Composable
private fun TopicsScreen(
    dao: ClipNestDao,
    onOpenTopic: (Long) -> Unit
) {
    val topics by dao.observeTopics()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAdd by remember { mutableStateOf(false) }
    var editingTopic by remember { mutableStateOf<TopicEntity?>(null) }
    var deletingTopic by remember { mutableStateOf<TopicEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp)
    ) {
        PageHeader(
            "主题空间",
            "${topics.size} 个主题 · 组织你的资料库"
        ) {
            BrutalActionButton(
                text = "+ 新建",
                onClick = { showAdd = true }
            )
        }

        Spacer(Modifier.height(20.dp))

        if (topics.isEmpty()) {
            EmptyState(
                "创建第一个主题",
                "例如：服务器资料、工作项目、账号资料。",
                Icons.Default.FolderOpen
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(13.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(topics, key = { it.id }) { topic ->
                    TopicCard(
                        topic = topic,
                        dao = dao,
                        onClick = { onOpenTopic(topic.id) },
                        onEdit = { editingTopic = topic },
                        onDelete = { deletingTopic = topic },
                        onDuplicate = {
                            scope.launch {
                                val newId =
                                    dao.duplicateTopicWithContents(topic.id)

                                if (newId != null) {
                                    Toast.makeText(
                                        context,
                                        "主题及其全部资料已复制",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAdd) {
        TopicFormDialog(
            title = "创建主题",
            initialTitle = "",
            initialDescription = "",
            onDismiss = { showAdd = false },
            onSave = { title, description ->
                scope.launch {
                    dao.insertTopic(
                        TopicEntity(
                            title = title,
                            description = description
                        )
                    )
                    showAdd = false
                }
            }
        )
    }

    editingTopic?.let { topic ->
        TopicFormDialog(
            title = "编辑主题",
            initialTitle = topic.title,
            initialDescription = topic.description,
            onDismiss = { editingTopic = null },
            onSave = { title, description ->
                scope.launch {
                    dao.updateTopic(
                        topic.copy(
                            title = title,
                            description = description
                        )
                    )
                    editingTopic = null
                }
            }
        )
    }

    deletingTopic?.let { topic ->
        ConfirmDeleteDialog(
            title = "删除主题",
            message = "确定删除「${topic.title}」吗？主题下的资料条目和字段也会一并删除，无法恢复。",
            onDismiss = { deletingTopic = null },
            onConfirm = {
                scope.launch {
                    dao.deleteTopic(topic)
                    deletingTopic = null
                }
            }
        )
    }
}

@Composable
private fun TopicCard(
    topic: TopicEntity,
    dao: ClipNestDao,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val entries by dao.observeEntries(topic.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    BrutalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(Accent)
                        .border(2.dp, Ink, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        tint = Ink,
                        modifier = Modifier.size(25.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        topic.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        topic.description.ifBlank { "点击查看资料条目" },
                        color = Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Muted
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Ink.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entries.size} 个资料条目",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onDuplicate) {
                    Icon(
                        Icons.Default.ContentCopy,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("复制", color = Ink, fontSize = 11.sp)
                }

                TextButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("编辑", color = Ink, fontSize = 11.sp)
                }

                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("删除", color = Ink, fontSize = 11.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 第二层：资料条目列表
// ----------------------------------------------------

@Composable
private fun TopicEntriesScreen(
    dao: ClipNestDao,
    topicId: Long,
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    val topics by dao.observeTopics()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val topic = topics.firstOrNull { it.id == topicId }

    val entries by dao.observeEntries(topicId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAdd by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MaterialEntryEntity?>(null) }
    var deletingEntry by remember { mutableStateOf<MaterialEntryEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Ink)
            }

            Column(Modifier.weight(1f)) {
                Text(
                    topic?.title ?: "资料条目",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${entries.size} 个资料条目",
                    color = Muted,
                    fontSize = 11.sp
                )
            }

            BrutalActionButton(
                text = "+ 新建",
                onClick = { showAdd = true }
            )
        }

        Spacer(Modifier.height(18.dp))

        if (entries.isEmpty()) {
            EmptyState(
                "这里还没有资料",
                "创建一个资料条目，例如香港服务器、QQ账号。",
                Icons.Default.Folder
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        dao = dao,
                        onClick = { onOpenEntry(entry.id) },
                        onEdit = { editingEntry = entry },
                        onDelete = { deletingEntry = entry },
                        onDuplicate = {
                            scope.launch {
                                val newId =
                                    dao.duplicateEntryWithFields(entry.id)

                                if (newId != null) {
                                    Toast.makeText(
                                        context,
                                        "资料条目及其字段已复制",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAdd) {
        EntryFormDialog(
            title = "新建资料条目",
            initialTitle = "",
            initialDescription = "",
            onDismiss = { showAdd = false },
            onSave = { title, description ->
                scope.launch {
                    dao.insertEntry(
                        MaterialEntryEntity(
                            topicId = topicId,
                            title = title,
                            description = description,
                            sortOrder = entries.size
                        )
                    )
                    showAdd = false
                }
            }
        )
    }

    editingEntry?.let { entry ->
        EntryFormDialog(
            title = "编辑资料条目",
            initialTitle = entry.title,
            initialDescription = entry.description,
            onDismiss = { editingEntry = null },
            onSave = { title, description ->
                scope.launch {
                    dao.updateEntry(
                        entry.copy(
                            title = title,
                            description = description
                        )
                    )
                    editingEntry = null
                }
            }
        )
    }

    deletingEntry?.let { entry ->
        ConfirmDeleteDialog(
            title = "删除资料条目",
            message = "确定删除「${entry.title}」吗？其中的所有字段也会一并删除。",
            onDismiss = { deletingEntry = null },
            onConfirm = {
                scope.launch {
                    dao.deleteEntry(entry)
                    deletingEntry = null
                }
            }
        )
    }
}

@Composable
private fun EntryCard(
    entry: MaterialEntryEntity,
    dao: ClipNestDao,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val fields by dao.observeFields(entry.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    BrutalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Sky)
                        .border(2.dp, Ink, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        null,
                        tint = Ink
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        entry.description.ifBlank { "点击查看字段详情" },
                        fontSize = 11.sp,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(Icons.Default.ChevronRight, null, tint = Muted)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Ink.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${fields.size} 个字段",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onDuplicate) {
                    Icon(
                        Icons.Default.ContentCopy,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("复制", color = Ink, fontSize = 11.sp)
                }

                TextButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("编辑", color = Ink, fontSize = 11.sp)
                }

                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("删除", color = Ink, fontSize = 11.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 第三层：字段详情
// ----------------------------------------------------

@Composable
private fun EntryDetailScreen(
    dao: ClipNestDao,
    entryId: Long,
    onBack: () -> Unit
) {
    val entry by dao.observeEntry(entryId)
        .collectAsStateWithLifecycle(initialValue = null)

    val fields by dao.observeFields(entryId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAdd by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<MaterialFieldEntity?>(null) }
    var deletingField by remember { mutableStateOf<MaterialFieldEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Ink)
            }

            Column(Modifier.weight(1f)) {
                Text(
                    entry?.title ?: "字段详情",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "${fields.size} 个字段",
                    color = Muted,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = {
                    if (fields.isNotEmpty()) {
                        val allText = fields.joinToString("\n") {
                            "${it.fieldName}：${it.fieldValue}"
                        }
                        copyToClipboard(
                            context,
                            allText,
                            "已复制全部字段"
                        )
                    }
                },
                enabled = fields.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制全部字段",
                    tint = Ink
                )
            }

            BrutalActionButton(
                text = "+ 添加",
                onClick = { showAdd = true }
            )
        }

        entry?.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        if (fields.isEmpty()) {
            EmptyState(
                "还没有字段",
                "添加 IP、端口、账号、密码等资料字段。",
                Icons.Default.ListAlt
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(11.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(fields, key = { it.id }) { field ->
                    val index = fields.indexOf(field)

                    FieldCard(
                        field = field,
                        index = index,
                        total = fields.size,
                        onCopy = {
                            copyToClipboard(
                                context,
                                field.fieldValue,
                                "已复制「${field.fieldName}」"
                            )
                        },
                        onEdit = { editingField = field },
                        onDelete = { deletingField = field },
                        onMoveUp = {
                            if (index > 0) {
                                scope.launch {
                                    val reordered = fields.toMutableList()
                                    val current = reordered.removeAt(index)
                                    reordered.add(index - 1, current)

                                    reordered.forEachIndexed { position, field ->
                                        dao.updateField(field.copy(sortOrder = position))
                                    }
                                }
                            }
                        },
                        onMoveDown = {
                            if (index < fields.lastIndex) {
                                scope.launch {
                                    val reordered = fields.toMutableList()
                                    val current = reordered.removeAt(index)
                                    reordered.add(index + 1, current)

                                    reordered.forEachIndexed { position, field ->
                                        dao.updateField(field.copy(sortOrder = position))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAdd) {
        FieldFormDialog(
            title = "添加字段",
            initialName = "",
            initialValue = "",
            onDismiss = { showAdd = false },
            onSave = { name, value ->
                scope.launch {
                    dao.insertField(
                        MaterialFieldEntity(
                            entryId = entryId,
                            fieldName = name,
                            fieldValue = value,
                            sortOrder = fields.size
                        )
                    )
                    showAdd = false
                }
            }
        )
    }

    editingField?.let { field ->
        FieldFormDialog(
            title = "编辑字段",
            initialName = field.fieldName,
            initialValue = field.fieldValue,
            onDismiss = { editingField = null },
            onSave = { name, value ->
                scope.launch {
                    dao.updateField(
                        field.copy(
                            fieldName = name,
                            fieldValue = value
                        )
                    )
                    editingField = null
                }
            }
        )
    }

    deletingField?.let { field ->
        ConfirmDeleteDialog(
            title = "删除字段",
            message = "确定删除字段「${field.fieldName}」吗？",
            onDismiss = { deletingField = null },
            onConfirm = {
                scope.launch {
                    dao.deleteField(field)
                    deletingField = null
                }
            }
        )
    }
}

@Composable
private fun FieldCard(
    field: MaterialFieldEntity,
    index: Int,
    total: Int,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    BrutalCard(modifier = Modifier.fillMaxWidth(), shadow = 4.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Accent,
                    shape = RectangleShape,
                    modifier = Modifier.border(1.dp, Ink, RectangleShape)
                ) {
                    Text(
                        (index + 1).toString().padStart(2, '0'),
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                }

                Spacer(Modifier.width(9.dp))

                Text(
                    field.fieldName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )

                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制字段",
                        tint = Muted
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑字段",
                        tint = Muted
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除字段",
                        tint = Muted
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            SelectionContainer {
                Text(
                    field.fieldValue.ifBlank { "（空内容）" },
                    fontSize = 14.sp,
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF9F0))
                        .border(1.dp, Ink.copy(alpha = 0.25f), RectangleShape)
                        .clickable(onClick = onCopy)
                        .padding(11.dp)
                )
            }

            if (total > 1) {
                Spacer(Modifier.height(5.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onMoveUp,
                        enabled = index > 0
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            null,
                            modifier = Modifier.size(17.dp)
                        )
                        Text("上移", color = Ink, fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = onMoveDown,
                        enabled = index < total - 1
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            null,
                            modifier = Modifier.size(17.dp)
                        )
                        Text("下移", color = Ink, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 主题、条目、字段表单
// ----------------------------------------------------

@Composable
private fun TopicFormDialog(
    title: String,
    initialTitle: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) {
        mutableStateOf(initialDescription)
    }

    BrutalDialog(
        title = title,
        onDismiss = onDismiss,
        content = {
            BrutalTextField(
                value = name,
                onValueChange = { name = it },
                label = "主题名称"
            )

            Spacer(Modifier.height(10.dp))

            BrutalTextField(
                value = description,
                onValueChange = { description = it },
                label = "主题说明",
                singleLine = false,
                minLines = 2
            )
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Muted)
            }
            BrutalActionButton(
                text = "保存",
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(name.trim(), description.trim())
                }
            )
        }
    )
}

@Composable
private fun EntryFormDialog(
    title: String,
    initialTitle: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) {
        mutableStateOf(initialDescription)
    }

    BrutalDialog(
        title = title,
        onDismiss = onDismiss,
        content = {
            BrutalTextField(
                value = name,
                onValueChange = { name = it },
                label = "资料名称"
            )

            Spacer(Modifier.height(10.dp))

            BrutalTextField(
                value = description,
                onValueChange = { description = it },
                label = "资料说明",
                singleLine = false,
                minLines = 2
            )
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Muted)
            }
            BrutalActionButton(
                text = "保存",
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(name.trim(), description.trim())
                }
            )
        }
    )
}

@Composable
private fun FieldFormDialog(
    title: String,
    initialName: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    BrutalDialog(
        title = title,
        onDismiss = onDismiss,
        content = {
            BrutalTextField(
                value = name,
                onValueChange = { name = it },
                label = "字段名称"
            )

            Spacer(Modifier.height(10.dp))

            BrutalTextField(
                value = value,
                onValueChange = { value = it },
                label = "字段内容",
                singleLine = false,
                minLines = 4
            )
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Muted)
            }
            BrutalActionButton(
                text = "保存",
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(name.trim(), value.trim())
                }
            )
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BrutalDialog(
        title = title,
        onDismiss = onDismiss,
        content = {
            Text(
                message,
                color = Ink,
                fontSize = 14.sp
            )
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Muted)
            }

            Button(
                onClick = onConfirm,
                shape = RectangleShape,
                modifier = Modifier.border(2.dp, Ink, RectangleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pink,
                    contentColor = Ink
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Text("确认删除", fontWeight = FontWeight.Bold)
            }
        }
    )
}
