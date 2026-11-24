package com.clipnest.app.data

object ClipClassifier {
    private val phone = Regex("^(\\+?\\d[\\d -]{6,14}\\d)$")
    private val email = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val url = Regex("^(https?://|www\\.)\\S+$", RegexOption.IGNORE_CASE)
    private val codeHints = listOf("fun ", "class ", "import ", "public ", "const ", "def ")

    fun detect(raw: String): Pair<String, String> {
        val value = raw.trim()
        return when {
            value.isEmpty() -> "文本" to "未分类"
            email.matches(value) -> "邮箱" to "邮箱"
            phone.matches(value) -> "手机号" to "手机号"
            url.matches(value) -> "链接" to "链接"
            value.matches(Regex("^\\d{4,8}$")) -> "验证码" to "验证码"
            codeHints.any { value.contains(it) } || value.contains("{") && value.contains("}") ->
                "代码" to "代码"
            else -> "文本" to "文本"
        }
    }
}
