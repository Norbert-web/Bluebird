package io.github.norbertweb.bluebird.editor.editor.highlighting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.editor.ui.theme.EditorColors

// ─────────────────────────────────────────────────────────────────
// Language Definitions
// ─────────────────────────────────────────────────────────────────

private data class LangDef(
    val keywords: Set<String>,
    val types: Set<String>,
    val builtins: Set<String>,
    val lineComment: String?,
    val blockCommentOpen: String?,
    val blockCommentClose: String?,
    val strings: List<Char>,
    val templateString: Char?,
    val decorators: Boolean,
)

private val KOTLIN_DEF = LangDef(
    keywords = setOf(
        "fun","val","var","class","object","interface","if","else","when","for","while","do",
        "return","import","package","is","as","in","!in","null","true","false","this","super",
        "override","open","sealed","data","enum","companion","by","constructor","init","get","set",
        "private","public","protected","internal","abstract","suspend","inline","reified","typealias",
        "try","catch","finally","throw","break","continue","it","let","run","apply","also","with",
        "to","until","step","downTo","out","where","crossinline","noinline","vararg","expect","actual",
        "external","lateinit","const","field","property","file","receiver","param","setparam","delegate"
    ),
    types = setOf(
        "Int","Long","Short","Byte","Double","Float","Boolean","String","Char","Unit","Any","Nothing",
        "List","MutableList","Map","MutableMap","Set","MutableSet","Array","Pair","Triple","Result",
        "Sequence","Flow","StateFlow","SharedFlow","Deferred","Job","Channel","Mutex","Semaphore",
        "Modifier","Color","Dp","Sp","TextStyle","FontFamily","FontWeight","Context","Activity",
        "ViewModel","LiveData","MutableLiveData","Observable","Observer","Coroutine","Scope",
    ),
    builtins = setOf("println","print","readLine","TODO","error","require","check","assert","lazy","by"),
    lineComment = "//", blockCommentOpen = "/*", blockCommentClose = "*/",
    strings = listOf('"', '\''), templateString = '"', decorators = false
)

private val JAVA_DEF = LangDef(
    keywords = setOf(
        "abstract","assert","break","case","catch","class","const","continue","default","do","else",
        "enum","extends","final","finally","for","goto","if","implements","import","instanceof",
        "interface","native","new","null","package","private","protected","public","return","static",
        "strictfp","super","switch","synchronized","this","throw","throws","transient","try","void",
        "volatile","while","true","false","record","sealed","permits","yield"
    ),
    types = setOf(
        "int","long","short","byte","double","float","boolean","char","String","Object","Integer",
        "Long","Double","Float","Boolean","Char","Byte","Short","List","ArrayList","Map","HashMap",
        "Set","HashSet","Array","Optional","Stream","Collection","Iterator","Iterable","Comparable",
        "Thread","Runnable","Exception","RuntimeException","Error","Throwable"
    ),
    builtins = setOf("System","Math","Arrays","Collections","Objects","String","StringBuilder"),
    lineComment = "//", blockCommentOpen = "/*", blockCommentClose = "*/",
    strings = listOf('"', '\''), templateString = null, decorators = true
)

private val PYTHON_DEF = LangDef(
    keywords = setOf(
        "and","as","assert","async","await","break","class","continue","def","del","elif","else",
        "except","finally","for","from","global","if","import","in","is","lambda","nonlocal","not",
        "or","pass","raise","return","try","while","with","yield","True","False","None","match","case"
    ),
    types = setOf(
        "int","float","str","bool","bytes","list","dict","set","tuple","type","object","complex",
        "range","enumerate","zip","map","filter","reversed","sorted","frozenset","bytearray",
        "memoryview","Exception","BaseException","ValueError","TypeError","KeyError","IndexError"
    ),
    builtins = setOf(
        "print","input","len","range","enumerate","zip","map","filter","sorted","reversed","sum",
        "min","max","abs","round","type","isinstance","issubclass","hasattr","getattr","setattr",
        "delattr","dir","vars","repr","str","int","float","bool","list","dict","set","tuple",
        "open","super","property","classmethod","staticmethod","id","hash","iter","next"
    ),
    lineComment = "#", blockCommentOpen = null, blockCommentClose = null,
    strings = listOf('"', '\''), templateString = null, decorators = true
)

private val JS_TS_DEF = LangDef(
    keywords = setOf(
        "break","case","catch","class","const","continue","debugger","default","delete","do","else",
        "export","extends","finally","for","function","if","import","in","instanceof","let","new",
        "null","return","static","super","switch","this","throw","try","typeof","undefined","var",
        "void","while","with","yield","async","await","of","from","as","true","false","type",
        "interface","enum","implements","declare","abstract","readonly","namespace","module","keyof",
        "infer","never","unknown","any","satisfies","override","using"
    ),
    types = setOf(
        "string","number","boolean","object","symbol","bigint","void","null","undefined","never",
        "any","unknown","Array","Promise","Map","Set","WeakMap","WeakSet","Date","RegExp","Error",
        "Function","Generator","AsyncGenerator","Iterator","Iterable","Record","Partial","Required",
        "Readonly","Pick","Omit","Exclude","Extract","NonNullable","ReturnType","Parameters",
        "InstanceType","ConstructorParameters","Awaited","HTMLElement","Event","MouseEvent"
    ),
    builtins = setOf(
        "console","Math","JSON","Object","Array","String","Number","Boolean","Symbol","BigInt",
        "Date","RegExp","Error","Promise","Map","Set","WeakMap","WeakSet","Proxy","Reflect",
        "parseInt","parseFloat","isNaN","isFinite","encodeURI","decodeURI","fetch","setTimeout",
        "clearTimeout","setInterval","clearInterval","requestAnimationFrame","queueMicrotask"
    ),
    lineComment = "//", blockCommentOpen = "/*", blockCommentClose = "*/",
    strings = listOf('"', '\''), templateString = '`', decorators = true
)

private val RUST_DEF = LangDef(
    keywords = setOf(
        "as","async","await","break","const","continue","crate","dyn","else","enum","extern","false",
        "fn","for","if","impl","in","let","loop","match","mod","move","mut","pub","ref","return",
        "self","Self","static","struct","super","trait","true","type","union","unsafe","use","where",
        "while","abstract","become","box","do","final","macro","override","priv","try","typeof",
        "unsized","virtual","yield"
    ),
    types = setOf(
        "i8","i16","i32","i64","i128","isize","u8","u16","u32","u64","u128","usize",
        "f32","f64","bool","char","str","String","Vec","HashMap","HashSet","BTreeMap","BTreeSet",
        "Option","Result","Box","Rc","Arc","Cell","RefCell","Mutex","RwLock","Cow","Slice",
        "Iterator","IntoIterator","FromIterator","From","Into","AsRef","AsMut","Default","Clone",
        "Copy","Drop","Fn","FnMut","FnOnce","Send","Sync","Sized","Unpin","Future","Stream"
    ),
    builtins = setOf("println","print","eprintln","eprint","panic","todo","unimplemented","unreachable",
        "assert","assert_eq","assert_ne","debug_assert","vec","format","write","writeln","include","env",
        "concat","stringify","cfg","derive","allow","warn","deny","forbid","deprecated"),
    lineComment = "//", blockCommentOpen = "/*", blockCommentClose = "*/",
    strings = listOf('"'), templateString = null, decorators = true
)

private val GO_DEF = LangDef(
    keywords = setOf(
        "break","case","chan","const","continue","default","defer","else","fallthrough","for","func",
        "go","goto","if","import","interface","map","package","range","return","select","struct",
        "switch","type","var","true","false","nil","iota"
    ),
    types = setOf(
        "bool","byte","complex64","complex128","error","float32","float64","int","int8","int16",
        "int32","int64","rune","string","uint","uint8","uint16","uint32","uint64","uintptr","any",
    ),
    builtins = setOf("append","cap","close","complex","copy","delete","imag","len","make","new",
        "panic","print","println","real","recover"),
    lineComment = "//", blockCommentOpen = "/*", blockCommentClose = "*/",
    strings = listOf('"', '\''), templateString = '`', decorators = false
)

private val CSS_DEF = LangDef(
    keywords = setOf(
        "important","media","keyframes","supports","charset","import","namespace","page","font-face",
        "counter-style","layer","container","scope","starting-style",
        "from","to","and","not","only","or","animation","transition","transform","flex","grid",
        "block","inline","none","auto","normal","bold","italic","underline","center","left","right",
        "top","bottom","absolute","relative","fixed","sticky","static","hidden","visible","scroll",
        "transparent","inherit","initial","unset","revert","var","calc","rgb","rgba","hsl","hsla",
        "url","linear-gradient","radial-gradient","repeat","no-repeat","cover","contain"
    ),
    types = setOf(
        "px","em","rem","vh","vw","vmin","vmax","fr","deg","rad","turn","ms","s","ch","ex",
        "%","cm","mm","in","pt","pc","svh","svw","dvh","dvw","cqw","cqh"
    ),
    builtins = emptySet(),
    lineComment = null, blockCommentOpen = "/*", blockCommentClose = "*/",
    strings = listOf('"', '\''), templateString = null, decorators = false
)

private val HTML_DEF = LangDef(
    keywords = setOf(
        "html","head","body","div","span","p","a","ul","ol","li","table","tr","td","th","thead",
        "tbody","tfoot","form","input","button","select","option","textarea","label","nav","header",
        "footer","main","section","article","aside","h1","h2","h3","h4","h5","h6","img","script",
        "style","link","meta","title","base","br","hr","strong","em","b","i","u","s","code","pre",
        "blockquote","figure","figcaption","video","audio","source","canvas","svg","path","g",
        "circle","rect","line","polyline","polygon","text","defs","use","symbol","marker","clip-path",
        "class","id","href","src","type","name","value","placeholder","required","disabled","checked",
        "selected","multiple","readonly","autocomplete","action","method","enctype","target","rel",
        "alt","width","height","style","data","aria","role","tabindex","lang","charset","content",
        "property","http-equiv","viewport","defer","async","crossorigin","integrity","nonce"
    ),
    types = emptySet(), builtins = emptySet(),
    lineComment = null, blockCommentOpen = "<!--", blockCommentClose = "-->",
    strings = listOf('"', '\''), templateString = null, decorators = false
)

private val SQL_DEF = LangDef(
    keywords = setOf(
        "SELECT","FROM","WHERE","AND","OR","NOT","IN","LIKE","BETWEEN","IS","NULL","ORDER","BY",
        "GROUP","HAVING","LIMIT","OFFSET","JOIN","INNER","LEFT","RIGHT","FULL","OUTER","ON","AS",
        "INSERT","INTO","VALUES","UPDATE","SET","DELETE","CREATE","TABLE","DROP","ALTER","ADD",
        "COLUMN","INDEX","PRIMARY","KEY","FOREIGN","REFERENCES","UNIQUE","CHECK","DEFAULT","AUTO_INCREMENT",
        "WITH","UNION","ALL","DISTINCT","CASE","WHEN","THEN","ELSE","END","OVER","PARTITION","WINDOW",
        "EXISTS","ANY","SOME","EVERY","ARRAY","RETURNING","LATERAL","CROSS","NATURAL","USING","EXPLAIN",
        "ANALYZE","VACUUM","TRUNCATE","GRANT","REVOKE","BEGIN","COMMIT","ROLLBACK","TRANSACTION","SAVEPOINT",
        "select","from","where","and","or","not","in","like","between","is","null","order","by",
        "group","having","limit","offset","join","inner","left","right","full","outer","on","as",
        "insert","into","values","update","set","delete","create","table","drop","alter","add",
        "with","union","all","distinct","case","when","then","else","end","over","partition"
    ),
    types = setOf(
        "INT","INTEGER","BIGINT","SMALLINT","TINYINT","DECIMAL","NUMERIC","FLOAT","DOUBLE","REAL",
        "BOOLEAN","BOOL","CHAR","VARCHAR","TEXT","BLOB","BINARY","VARBINARY","DATE","TIME",
        "DATETIME","TIMESTAMP","YEAR","JSON","UUID","SERIAL","BIGSERIAL","ARRAY","BYTEA",
        "int","integer","bigint","smallint","text","varchar","boolean","timestamp","json","uuid"
    ),
    builtins = setOf(
        "COUNT","SUM","AVG","MIN","MAX","COALESCE","NULLIF","IFNULL","NVL","CONCAT","SUBSTRING",
        "LENGTH","UPPER","LOWER","TRIM","REPLACE","NOW","CURRENT_DATE","CURRENT_TIMESTAMP",
        "EXTRACT","DATE_FORMAT","DATEDIFF","DATE_ADD","CAST","CONVERT","ROW_NUMBER","RANK",
        "DENSE_RANK","LAG","LEAD","FIRST_VALUE","LAST_VALUE","count","sum","avg","min","max"
    ),
    lineComment = "--", blockCommentOpen = "/*", blockCommentClose = "*/",
    strings = listOf('\'', '"'), templateString = null, decorators = false
)

private val SHELL_DEF = LangDef(
    keywords = setOf(
        "if","then","else","elif","fi","for","in","do","done","while","until","case","esac",
        "function","return","exit","break","continue","local","declare","typeset","readonly",
        "export","unset","source","alias","unalias","shift","set","unset","trap","wait","jobs",
        "bg","fg","kill","echo","printf","read","test","true","false","select","time","coproc"
    ),
    types = emptySet(),
    builtins = setOf(
        "cd","ls","pwd","mkdir","rm","cp","mv","chmod","chown","grep","sed","awk","find","xargs",
        "cat","head","tail","sort","uniq","wc","diff","tar","gzip","curl","wget","ssh","scp",
        "git","docker","kubectl","npm","pip","apt","brew","systemctl","journalctl","ps","top",
        "kill","killall","pgrep","pkill","lsof","netstat","ss","ip","ifconfig","ping","nslookup"
    ),
    lineComment = "#", blockCommentOpen = null, blockCommentClose = null,
    strings = listOf('"', '\''), templateString = '"', decorators = false
)

private val MARKDOWN_DEF = LangDef(
    keywords = emptySet(), types = emptySet(), builtins = emptySet(),
    lineComment = null, blockCommentOpen = null, blockCommentClose = null,
    strings = listOf('`'), templateString = null, decorators = false
)

private val XML_JSON_DEF = LangDef(
    keywords = setOf("true","false","null"),
    types = emptySet(), builtins = emptySet(),
    lineComment = null, blockCommentOpen = "<!--", blockCommentClose = "-->",
    strings = listOf('"', '\''), templateString = null, decorators = false
)

private fun langDef(ext: String): LangDef? = when (ext) {
    "kt", "kts" -> KOTLIN_DEF
    "java" -> JAVA_DEF
    "py", "pyw" -> PYTHON_DEF
    "js", "jsx", "mjs", "cjs" -> JS_TS_DEF
    "ts", "tsx" -> JS_TS_DEF
    "rs" -> RUST_DEF
    "go" -> GO_DEF
    "css", "scss", "sass", "less" -> CSS_DEF
    "html", "htm", "xml", "svg" -> HTML_DEF
    "sql" -> SQL_DEF
    "sh", "bash", "zsh", "fish", "ps1" -> SHELL_DEF
    "md", "mdx" -> MARKDOWN_DEF
    "json", "jsonc" -> XML_JSON_DEF
    "c", "h" -> JAVA_DEF // close enough for C
    "cpp", "hpp", "cc", "cxx" -> JAVA_DEF
    "cs" -> JAVA_DEF
    "swift" -> KOTLIN_DEF // similar structure
    "rb" -> PYTHON_DEF
    "php" -> JS_TS_DEF
    "dart" -> KOTLIN_DEF
    "yaml", "yml" -> SHELL_DEF // simple highlight
    "toml" -> SHELL_DEF
    else -> null
}

// ─────────────────────────────────────────────────────────────────
// Highlight Engine
// ─────────────────────────────────────────────────────────────────

fun buildSyntaxHighlight(
    text: String,
    ext: String,
    colors: EditorColors,
    findQuery: String = "",
    matchCase: Boolean = false,
    useRegex: Boolean = false,
    showWhitespace: Boolean = false,
    currentMatchRange: IntRange? = null,
    allMatchRanges: List<IntRange> = emptyList(),
): AnnotatedString {
    val def = langDef(ext)

    return buildAnnotatedString {
        append(text)

        if (def != null) {
            if (ext == "md" || ext == "mdx") {
                highlightMarkdown(text, colors)
            } else if (ext == "html" || ext == "htm" || ext == "xml" || ext == "svg") {
                highlightHtml(text, colors, def)
            } else {
                highlightGeneric(text, colors, def, ext)
            }
        }

        // Find highlights (on top of syntax)
        if (findQuery.isNotEmpty()) {
            try {
                val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                val pat = if (useRegex) Regex(findQuery, flags) else Regex(Regex.escape(findQuery), flags)
                pat.findAll(text).forEach { m ->
                    val range = m.range
                    val isCurrentMatch = currentMatchRange != null && range == currentMatchRange
                    addStyle(
                        SpanStyle(
                            background = if (isCurrentMatch) colors.findCurrentHighlight else colors.findHighlight,
                            color = if (isCurrentMatch) Color.White else Color.Unspecified
                        ),
                        range.first, range.last + 1
                    )
                }
            } catch (_: Exception) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Generic Highlighter (covers most languages)
// ─────────────────────────────────────────────────────────────────

private fun AnnotatedString.Builder.highlightGeneric(
    text: String,
    colors: EditorColors,
    def: LangDef,
    ext: String,
) {
    var i = 0
    val n = text.length

    while (i < n) {
        // Block comments
        if (def.blockCommentOpen != null && text.startsWith(def.blockCommentOpen, i)) {
            val end = text.indexOf(def.blockCommentClose ?: "", i + def.blockCommentOpen.length)
            val commentEnd = if (end == -1) n else end + (def.blockCommentClose?.length ?: 0)
            addStyle(SpanStyle(color = colors.synComment, fontStyle = FontStyle.Italic), i, commentEnd)
            i = commentEnd
            continue
        }

        // Line comments
        if (def.lineComment != null && text.startsWith(def.lineComment, i)) {
            val end = text.indexOf('\n', i).let { if (it == -1) n else it }
            addStyle(SpanStyle(color = colors.synComment, fontStyle = FontStyle.Italic), i, end)
            i = end
            continue
        }

        // Decorators / annotations
        if (def.decorators && text[i] == '@') {
            val end = run {
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '_' || text[j] == '.')) j++
                j
            }
            if (end > i + 1) {
                addStyle(SpanStyle(color = colors.synAnnotation, fontWeight = FontWeight.Medium), i, end)
                i = end
                continue
            }
        }

        // Template strings (backticks in JS/Go, f-strings handled simply)
        if (def.templateString != null && text[i] == def.templateString && ext in listOf("js","jsx","ts","tsx","go")) {
            val end = findStringEnd(text, i, def.templateString)
            addStyle(SpanStyle(color = colors.synString), i, end)
            i = end
            continue
        }

        // Strings
        if (text[i] in def.strings) {
            val quote = text[i]
            val isTriple = text.startsWith("$quote$quote$quote", i)
            val end = if (isTriple) {
                val te = text.indexOf("$quote$quote$quote", i + 3)
                if (te == -1) n else te + 3
            } else {
                findStringEnd(text, i, quote)
            }
            addStyle(SpanStyle(color = colors.synString), i, end)
            i = end
            continue
        }

        // Numbers
        if (text[i].isDigit() || (text[i] == '-' && i + 1 < n && text[i + 1].isDigit() && (i == 0 || !text[i - 1].isLetterOrDigit()))) {
            val start = i
            if (text[i] == '-') i++
            // hex
            if (i + 1 < n && text[i] == '0' && (text[i + 1] == 'x' || text[i + 1] == 'X')) {
                i += 2
                while (i < n && (text[i].isDigit() || text[i] in 'a'..'f' || text[i] in 'A'..'F' || text[i] == '_')) i++
            } else {
                while (i < n && (text[i].isDigit() || text[i] == '.' || text[i] == '_' || text[i] in listOf('e','E','f','L','u','U','b','B'))) i++
            }
            addStyle(SpanStyle(color = colors.synNumber), start, i)
            continue
        }

        // Identifiers / keywords
        if (text[i].isLetter() || text[i] == '_') {
            val start = i
            while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
            val word = text.substring(start, i)

            val style = when {
                def.keywords.contains(word) -> SpanStyle(color = colors.synKeyword, fontWeight = FontWeight.SemiBold)
                def.types.contains(word) -> SpanStyle(color = colors.synType)
                def.builtins.contains(word) -> SpanStyle(color = colors.synFunction)
                i < n && text[i] == '(' -> SpanStyle(color = colors.synFunction)
                word[0].isUpperCase() -> SpanStyle(color = colors.synType)
                else -> null
            }
            if (style != null) addStyle(style, start, i)
            continue
        }

        // Operators
        if (text[i] in "=<>!&|+-*/%^~?") {
            val start = i
            while (i < n && text[i] in "=<>!&|+-*/%^~?") i++
            addStyle(SpanStyle(color = colors.synOperator), start, i)
            continue
        }

        i++
    }
}

private fun findStringEnd(text: String, start: Int, quote: Char): Int {
    var i = start + 1
    while (i < text.length) {
        if (text[i] == '\\') { i += 2; continue }
        if (text[i] == quote) return i + 1
        if (text[i] == '\n') return i // unterminated string on this line
        i++
    }
    return text.length
}

// ─────────────────────────────────────────────────────────────────
// Markdown Highlighter
// ─────────────────────────────────────────────────────────────────

private fun AnnotatedString.Builder.highlightMarkdown(text: String, colors: EditorColors) {
    val lines = text.split('\n')
    var offset = 0
    for (line in lines) {
        val trimmed = line.trimStart()
        when {
            // Headings
            trimmed.startsWith("# ") -> addStyle(SpanStyle(color = colors.synKeyword, fontWeight = FontWeight.Bold, fontSize = 20.sp), offset, offset + line.length)
            trimmed.startsWith("## ") -> addStyle(SpanStyle(color = colors.synKeyword, fontWeight = FontWeight.Bold), offset, offset + line.length)
            trimmed.startsWith("### ") -> addStyle(SpanStyle(color = colors.synType, fontWeight = FontWeight.SemiBold), offset, offset + line.length)
            trimmed.startsWith("#### ") || trimmed.startsWith("##### ") || trimmed.startsWith("###### ") ->
                addStyle(SpanStyle(color = colors.synType), offset, offset + line.length)
            // Block quote
            trimmed.startsWith("> ") -> addStyle(SpanStyle(color = colors.synComment, fontStyle = FontStyle.Italic), offset, offset + line.length)
            // Code block start/end
            trimmed.startsWith("```") || trimmed.startsWith("~~~") ->
                addStyle(SpanStyle(color = colors.synAnnotation), offset, offset + line.length)
            // Horizontal rule
            trimmed == "---" || trimmed == "***" || trimmed == "___" ->
                addStyle(SpanStyle(color = colors.synOperator), offset, offset + line.length)
            // List item
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") ||
                    trimmed.matches(Regex("\\d+\\..*")) ->
                addStyle(SpanStyle(color = colors.synConstant), offset, offset + 2.coerceAtMost(line.length))
            else -> {
                // Inline: bold, italic, code, links
                highlightMarkdownInline(line, offset, colors)
            }
        }
        offset += line.length + 1 // +1 for \n
    }
}



private fun AnnotatedString.Builder.highlightMarkdownInline(line: String, lineOffset: Int, colors: EditorColors) {
    var i = 0
    while (i < line.length) {
        when {
            // Bold **text** or __text__
            (line.startsWith("**", i) || line.startsWith("__", i)) -> {
                val marker = line.substring(i, i + 2)
                val end = line.indexOf(marker, i + 2)
                if (end != -1) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.text), lineOffset + i, lineOffset + end + 2)
                    i = end + 2
                } else i++
            }
            // Italic *text* or _text_
            (line[i] == '*' || line[i] == '_') -> {
                val marker = line[i]
                val end = line.indexOf(marker, i + 1)
                if (end != -1) {
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = colors.synFunction), lineOffset + i, lineOffset + end + 1)
                    i = end + 1
                } else i++
            }
            // Inline code `code`
            line[i] == '`' -> {
                val end = line.indexOf('`', i + 1)
                if (end != -1) {
                    addStyle(SpanStyle(color = colors.synString, background = colors.surface), lineOffset + i, lineOffset + end + 1)
                    i = end + 1
                } else i++
            }
            // Links [text](url)
            line[i] == '[' -> {
                val textEnd = line.indexOf(']', i)
                if (textEnd != -1 && textEnd + 1 < line.length && line[textEnd + 1] == '(') {
                    val urlEnd = line.indexOf(')', textEnd + 2)
                    if (urlEnd != -1) {
                        addStyle(SpanStyle(color = colors.synKeyword, fontWeight = FontWeight.Medium), lineOffset + i, lineOffset + textEnd + 1)
                        addStyle(SpanStyle(color = colors.synComment), lineOffset + textEnd + 1, lineOffset + urlEnd + 1)
                        i = urlEnd + 1
                    } else i++
                } else i++
            }
            else -> i++
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// HTML/XML Highlighter
// ─────────────────────────────────────────────────────────────────

private fun AnnotatedString.Builder.highlightHtml(text: String, colors: EditorColors, def: LangDef) {
    var i = 0
    val n = text.length
    while (i < n) {
        // Comments
        if (def.blockCommentOpen != null && text.startsWith(def.blockCommentOpen, i)) {
            val closeTag = def.blockCommentClose ?: ""
            val end = text.indexOf(closeTag, i + def.blockCommentOpen.length)
            val commentEnd = if (end == -1) n else end + closeTag.length
            addStyle(SpanStyle(color = colors.synComment, fontStyle = FontStyle.Italic), i, commentEnd)
            i = commentEnd
            continue
        }
        // Doctype
        if (text.startsWith("<!", i)) {
            val end = text.indexOf('>', i)
            val tagEnd = if (end == -1) n else end + 1
            addStyle(SpanStyle(color = colors.synComment), i, tagEnd)
            i = tagEnd
            continue
        }
        // Tags
        if (text[i] == '<') {
            val end = text.indexOf('>', i)
            val tagEnd = if (end == -1) n else end + 1
            val tagContent = text.substring(i, tagEnd)
            // Tag name
            val nameMatch = Regex("</?([a-zA-Z][a-zA-Z0-9-]*)").find(tagContent)
            if (nameMatch != null) {
                val nameStart = i + nameMatch.range.first + (if (tagContent.startsWith("</")) 2 else 1)
                val nameEnd = nameStart + nameMatch.groupValues[1].length
                addStyle(SpanStyle(color = colors.synKeyword, fontWeight = FontWeight.SemiBold), nameStart, nameEnd)
            }
            // Attributes
            Regex("""(\s)([a-zA-Z:-]+)(=)""").findAll(tagContent).forEach { m ->
                val attrStart = i + m.range.first + 1
                val attrEnd = i + m.groups[2]!!.range.last + 1
                if (attrEnd <= tagEnd) addStyle(SpanStyle(color = colors.synAnnotation), attrStart, attrEnd)
            }
            // Attribute values
            Regex("""="([^"]*)" """).findAll(tagContent).plus(Regex("""='([^']*)'""").findAll(tagContent)).forEach { m ->
                val valStart = i + m.range.first
                val valEnd = i + m.range.last + 1
                if (valEnd <= tagEnd) addStyle(SpanStyle(color = colors.synString), valStart, valEnd)
            }
            // Angle brackets
            addStyle(SpanStyle(color = colors.synOperator), i, i + 1)
            if (end != -1) addStyle(SpanStyle(color = colors.synOperator), end, end + 1)
            i = tagEnd
            continue
        }
        // Strings inside attribute values are already handled above
        i++
    }
}

// ─────────────────────────────────────────────────────────────────
// Bracket Matching
// ─────────────────────────────────────────────────────────────────

data class BracketMatch(val openPos: Int, val closePos: Int, val isValid: Boolean)

fun findMatchingBracket(text: String, cursorPos: Int): BracketMatch? {
    if (cursorPos < 0 || cursorPos >= text.length) return null

    val openers = "([{<"
    val closers = ")]}>"
    val char = text[cursorPos]

    return when {
        char in openers -> {
            val closeChar = closers[openers.indexOf(char)]
            var depth = 0
            for (i in cursorPos until text.length) {
                when (text[i]) {
                    char -> depth++
                    closeChar -> {
                        depth--
                        if (depth == 0) return BracketMatch(cursorPos, i, true)
                    }
                }
            }
            BracketMatch(cursorPos, -1, false)
        }
        char in closers -> {
            val openChar = openers[closers.indexOf(char)]
            var depth = 0
            for (i in cursorPos downTo 0) {
                when (text[i]) {
                    char -> depth++
                    openChar -> {
                        depth--
                        if (depth == 0) return BracketMatch(i, cursorPos, true)
                    }
                }
            }
            BracketMatch(-1, cursorPos, false)
        }
        else -> null
    }
}

// ─────────────────────────────────────────────────────────────────
// Word autocomplete index
// ─────────────────────────────────────────────────────────────────

fun extractWords(text: String, minLength: Int = 3): List<String> {
    return Regex("[a-zA-Z_][a-zA-Z0-9_]{${minLength - 1},}")
        .findAll(text)
        .map { it.value }
        .distinct()
        .sortedBy { it.length }
        .take(200)
        .toList()
}

fun getSuggestions(text: String, cursorPos: Int, allWords: List<String>): List<String> {
    if (cursorPos <= 0) return emptyList()
    val wordStart = text.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '(', ')', '[', ']', '{', '}', '.', ',', ';', ':'), cursorPos - 1) + 1
    val prefix = text.substring(wordStart, cursorPos)
    if (prefix.length < 2) return emptyList()
    return allWords.filter { it.startsWith(prefix, ignoreCase = true) && it != prefix }.take(8)
}
