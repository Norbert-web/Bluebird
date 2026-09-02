package io.github.norbertweb.bluebird.editor.core

/** Context-aware lightweight completion provider. Designed to be replaced by LSP providers later. */
data class CompletionItem(val label: String, val detail: String, val insertText: String = label)

object CompletionEngine {
    private val htmlTags = listOf("html","head","body","main","section","article","header","footer","nav","div","span","p","h1","h2","h3","h4","h5","h6","a","img","button","form","input","label","textarea","select","option","ul","ol","li","table","thead","tbody","tr","th","td","script","style","link","meta","title","video","audio","canvas","svg")
    private val htmlAttrs = listOf("class","id","href","src","alt","title","style","type","name","value","placeholder","required","disabled","checked","selected","target","rel","width","height","aria-label","role","data-testid")
    private val cssProps = listOf("display","position","top","right","bottom","left","width","height","min-width","max-width","min-height","max-height","margin","padding","box-sizing","color","background","background-color","border","border-radius","font-family","font-size","font-weight","line-height","text-align","text-decoration","opacity","overflow","z-index","flex","flex-direction","flex-wrap","justify-content","align-items","gap","grid","grid-template-columns","grid-template-rows","transform","transition","animation","box-shadow","cursor")
    private val cssValues = listOf("block","inline","inline-block","flex","grid","none","auto","relative","absolute","fixed","sticky","hidden","visible","center","start","end","space-between","space-around","wrap","nowrap","solid","transparent","inherit","initial","unset","pointer","ease","linear")
    private val jsKeywords = listOf("const","let","var","function","return","if","else","for","while","do","switch","case","break","continue","class","extends","new","this","import","export","from","async","await","try","catch","finally","throw","typeof","instanceof","in","of","true","false","null","undefined")
    private val jsApis = listOf("console.log","console.error","document.querySelector","document.querySelectorAll","document.getElementById","document.createElement","addEventListener","removeEventListener","fetch","setTimeout","setInterval","JSON.parse","JSON.stringify","Math.max","Math.min","Array.isArray","Object.keys","Promise","URL","Date")

    fun suggest(text: String, cursor: Int, fileName: String, projectWords: List<String>): List<CompletionItem> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val safeCursor = cursor.coerceIn(0, text.length)
        val prefixStart = findPrefixStart(text, safeCursor)
        val prefix = text.substring(prefixStart, safeCursor)
        if (prefix.length < 1) return emptyList()
        val context = text.substring(0, safeCursor).takeLast(500)
        val candidates = when (ext) {
            "html", "htm" -> {
                if (context.lastIndexOf('<') > context.lastIndexOf('>')) htmlTags.map { CompletionItem(it, "HTML tag") }
                else htmlAttrs.map { CompletionItem(it, "HTML attribute") }
            }
            "css", "scss", "sass", "less" -> {
                if (context.lastIndexOf('{') > context.lastIndexOf('}')) cssProps.map { CompletionItem(it, "CSS property") }
                else cssValues.map { CompletionItem(it, "CSS value") }
            }
            "js", "jsx", "mjs", "cjs", "ts", "tsx" -> (jsKeywords.map { CompletionItem(it, "JavaScript keyword") } + jsApis.map { CompletionItem(it, "JavaScript API") })
            else -> emptyList()
        } + projectWords.map { CompletionItem(it, "Workspace symbol") }
        return candidates.distinctBy { it.label }.filter { it.label.startsWith(prefix, ignoreCase = true) && it.label != prefix }.take(10)
    }

    fun findPrefixStart(text: String, cursor: Int): Int {
        var i = cursor.coerceIn(0, text.length)
        while (i > 0 && (text[i - 1].isLetterOrDigit() || text[i - 1] == '_' || text[i - 1] == '-' || text[i - 1] == '$')) i--
        return i
    }
}
