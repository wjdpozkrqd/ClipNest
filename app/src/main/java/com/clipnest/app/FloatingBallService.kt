package com.clipnest.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FloatingBallService : Service() {

    private lateinit var windowManager: WindowManager

    private var ballView: GlassBallView? = null
    private var ballParams: WindowManager.LayoutParams? = null

    private var panelView: View? = null

    private val prefs by lazy {
        getSharedPreferences("floating_ball", Context.MODE_PRIVATE)
    }

    private val density by lazy {
        resources.displayMetrics.density
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ClipNest 正在运行")
            .setContentText("悬浮球已启用")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "请先授予悬浮窗权限",
                Toast.LENGTH_LONG
            ).show()

            stopSelf()
            return START_NOT_STICKY
        }

        if (ballView == null) {
            showBall()
        }

        return START_NOT_STICKY
    }

    private fun showBall() {

        val size = prefs.getInt(KEY_SIZE, 58)
            .coerceIn(44, 84)

        val view = GlassBallView(this).apply {
            alpha = prefs.getInt(KEY_ALPHA, 82) / 100f
            setOnTouchListener(BallTouchListener())
        }

        val params = WindowManager.LayoutParams(
            dp(size),
            dp(size),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {

            gravity = Gravity.TOP or Gravity.START

            x = prefs.getInt(
                KEY_X,
                resources.displayMetrics.widthPixels - dp(size) - dp(12)
            )

            y = prefs.getInt(
                KEY_Y,
                resources.displayMetrics.heightPixels / 3
            )
        }

        ballView = view
        ballParams = params

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
            ballView = null
            stopSelf()
        }
    }

    private inner class BallTouchListener : View.OnTouchListener {

        private var downX = 0f
        private var downY = 0f

        private var startX = 0
        private var startY = 0

        private var moved = false

        override fun onTouch(
            view: View,
            event: MotionEvent
        ): Boolean {

            val params = ballParams ?: return false

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = params.x
                    startY = params.y

                    moved = false

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()

                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) {
                        moved = true
                    }

                    if (moved) {

                        params.x = startX + dx
                        params.y = startY + dy

                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {
                        }
                    }

                    return true
                }

                MotionEvent.ACTION_UP -> {

                    if (moved) {
                        snapToEdge()
                    } else {
                        showPanel()
                    }

                    return true
                }

                MotionEvent.ACTION_CANCEL -> return true
            }

            return true
        }
    }

    private fun snapToEdge() {

        val view = ballView ?: return
        val params = ballParams ?: return

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val maxX = max(0, screenWidth - view.width)
        val maxY = max(0, screenHeight - view.height - dp(24))

        params.x =
            if (params.x + view.width / 2 < screenWidth / 2) {
                dp(5)
            } else {
                maxX - dp(5)
            }

        params.y = params.y.coerceIn(dp(12), maxY)

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
        }

        prefs.edit()
            .putInt(KEY_X, params.x)
            .putInt(KEY_Y, params.y)
            .apply()
    }

    private fun showPanel() {

        if (panelView != null) {
            removePanel()
            return
        }

        val root = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(16)
            )

            background = GradientDrawable().apply {
                setColor(Color.argb(238, 28, 29, 33))
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.argb(150, 255, 255, 255))
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            label("ClipNest", 21, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        header.addView(action("×") {
            removePanel()
        })

        root.addView(header)

        root.addView(
            label(
                "ROOT X  /  GLOBAL PANEL",
                10,
                0xFFFFD54A.toInt(),
                true
            )
        )

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row1.addView(
            tile("我的资料") {
                toast("资料搜索将在后续阶段接入")
            },
            weightParams()
        )

        row1.addView(
            tile("全局搜索") {
                toast("全局搜索将在后续阶段接入")
            },
            weightParams()
        )

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row2.addView(
            tile("剪贴板") {
                toast("剪贴板联动将在后续阶段接入")
            },
            weightParams()
        )

        row2.addView(
            tile("悬浮球设置") {
                showSettings()
            },
            weightParams()
        )

        root.addView(row1)
        root.addView(row2)

        root.addView(
            label(
                "悬浮球已运行 · 拖动吸附 · 点击收起",
                11,
                0xFFBFC3CC.toInt()
            )
        )

        addPanelView(root)
    }

    private fun showSettings() {

        removePanel()

        val root = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(18),
                dp(20),
                dp(18)
            )

            background = GradientDrawable().apply {
                setColor(Color.argb(245, 35, 36, 42))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.LTGRAY)
            }
        }

        root.addView(
            label("悬浮球外观", 19, Color.WHITE, true)
        )

        val sizeLabel = label(
            "大小：${prefs.getInt(KEY_SIZE, 58)} dp",
            13,
            Color.WHITE
        )

        root.addView(sizeLabel)

        val sizeSeek = SeekBar(this).apply {

            max = 40

            progress = prefs.getInt(KEY_SIZE, 58) - 44

            setOnSeekBarChangeListener(simpleSeek { value ->

                val size = value + 44

                prefs.edit()
                    .putInt(KEY_SIZE, size)
                    .apply()

                sizeLabel.text = "大小：${size} dp"

                resizeBall(size)
            })
        }

        root.addView(sizeSeek)

        val alphaLabel = label(
            "透明度：${prefs.getInt(KEY_ALPHA, 82)}%",
            13,
            Color.WHITE
        )

        root.addView(alphaLabel)

        val alphaSeek = SeekBar(this).apply {

            max = 100

            progress = prefs.getInt(KEY_ALPHA, 82)

            setOnSeekBarChangeListener(simpleSeek { value ->

                val alpha = value.coerceIn(25, 100)

                prefs.edit()
                    .putInt(KEY_ALPHA, alpha)
                    .apply()

                alphaLabel.text = "透明度：${alpha}%"

                ballView?.alpha = alpha / 100f
            })
        }

        root.addView(alphaSeek)

        root.addView(action("完成") {
            removePanel()
        })

        addPanelView(root)
    }

    private fun simpleSeek(
        onChange: (Int) -> Unit
    ) = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            if (fromUser) onChange(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun resizeBall(size: Int) {

        val view = ballView ?: return
        val params = ballParams ?: return

        params.width = dp(size)
        params.height = dp(size)

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun addPanelView(view: View) {

        val params = WindowManager.LayoutParams(
            min(
                resources.displayMetrics.widthPixels - dp(28),
                dp(360)
            ),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        panelView = view

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
            panelView = null
        }
    }

    private fun removePanel() {

        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        panelView = null
    }

    private fun tile(
        text: String,
        onClick: () -> Unit
    ) = TextView(this).apply {

        this.text = text
        textSize = 14f

        setTextColor(0xFF171717.toInt())

        typeface = Typeface.DEFAULT_BOLD

        gravity = Gravity.CENTER

        background = GradientDrawable().apply {
            setColor(0xFFF6F1E8.toInt())
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), 0x66FFFFFF)
        }

        setOnClickListener {
            onClick()
        }

        layoutParams = LinearLayout.LayoutParams(
            0,
            dp(76),
            1f
        ).apply {
            setMargins(dp(4), dp(8), dp(4), dp(8))
        }
    }

    private fun action(
        text: String,
        onClick: () -> Unit
    ) = TextView(this).apply {

        this.text = text
        textSize = 16f

        setTextColor(0xFFFFD54A.toInt())

        gravity = Gravity.CENTER

        setPadding(
            dp(14),
            dp(8),
            dp(14),
            dp(8)
        )

        setOnClickListener {
            onClick()
        }
    }

    private fun label(
        text: String,
        size: Int,
        color: Int,
        bold: Boolean = false
    ) = TextView(this).apply {

        this.text = text
        textSize = size.toFloat()

        setTextColor(color)

        if (bold) {
            typeface = Typeface.DEFAULT_BOLD
        }

        setPadding(0, dp(5), 0, dp(5))
    }

    private fun weightParams() =
        LinearLayout.LayoutParams(0, dp(82), 1f)

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * density).toInt()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "ClipNest 悬浮球",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {

        removePanel()

        ballView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        ballView = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    inner class GlassBallView(context: Context) : View(context) {

        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        override fun onDraw(canvas: Canvas) {

            super.onDraw(canvas)

            val cx = width / 2f
            val cy = height / 2f

            val radius = min(width, height) * 0.44f

            fill.shader = RadialGradient(
                cx * 0.72f,
                cy * 0.62f,
                radius * 1.55f,
                intArrayOf(
                    0xDFFFFFFF.toInt(),
                    0x885B6578.toInt(),
                    0xB8202532.toInt(),
                    0xCC11141D.toInt()
                ),
                floatArrayOf(0f, 0.38f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )

            canvas.drawCircle(cx, cy, radius, fill)

            fill.shader = null

            stroke.color = 0xAAFFFFFF.toInt()

            canvas.drawCircle(cx, cy, radius, stroke)

            fill.color = 0x99FFFFFF.toInt()

            canvas.drawOval(
                cx - radius * 0.68f,
                cy - radius * 0.83f,
                cx + radius * 0.18f,
                cy - radius * 0.55f,
                fill
            )

            fill.color = 0xFFFFD54A.toInt()

            canvas.drawCircle(
                cx + radius * 0.65f,
                cy + radius * 0.62f,
                radius * 0.13f,
                fill
            )

            val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color = Color.WHITE

                style = Paint.Style.STROKE

                strokeWidth = radius * 0.12f

                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val left = cx - radius * 0.34f
            val top = cy - radius * 0.40f
            val right = cx + radius * 0.25f
            val bottom = cy + radius * 0.36f

            canvas.drawRoundRect(
                left,
                top + radius * 0.13f,
                right,
                bottom,
                radius * 0.08f,
                radius * 0.08f,
                icon
            )

            canvas.drawRoundRect(
                cx - radius * 0.17f,
                top,
                cx + radius * 0.08f,
                top + radius * 0.20f,
                radius * 0.04f,
                radius * 0.04f,
                icon
            )

            val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFD54A.toInt()
                strokeWidth = radius * 0.16f
                strokeCap = Paint.Cap.ROUND
            }

            canvas.drawLine(
                cx - radius * 0.02f,
                cy + radius * 0.22f,
                cx + radius * 0.35f,
                cy - radius * 0.12f,
                pen
            )
        }
    }

    companion object {

        const val ACTION_START =
            "com.clipnest.app.FLOATING_START"

        const val ACTION_STOP =
            "com.clipnest.app.FLOATING_STOP"

        private const val CHANNEL_ID =
            "clipnest_floating_ball"

        private const val NOTIFICATION_ID = 2107

        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_SIZE = "size"
        private const val KEY_ALPHA = "alpha"
    }
}
