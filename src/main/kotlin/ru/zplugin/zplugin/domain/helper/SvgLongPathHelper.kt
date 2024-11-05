package ru.zplugin.zplugin.domain.helper

import ru.zplugin.zplugin.domain.parser.SvgPathSplitter

internal class SvgLongPathHelper {
    suspend fun getSplitText(
        text: String,
        progressConsumer: suspend (stage: String, step: String?, progress: Double) -> Unit,
    ): String? {
        progressConsumer("Searching tags", null, 0.0)
        val tags = TAG_REGEX.findAll(text).filter { !it.value.contains(EVEN_ODD_REGEX) }.toList()
        if (tags.isEmpty()) {
            return null
        }

        val progressPerElem = (MAX_PROGRESS - SEARCH_PROGRESS) / tags.size
        val onProgressMade: suspend (Int, Double) -> Unit = { index: Int, progress: Double ->
            val globalProgress = (SEARCH_PROGRESS + progressPerElem * (index + progress)).coerceAtMost(MAX_PROGRESS)
            progressConsumer("Splitting tags", "Processed $index/${tags.size}", globalProgress)
        }

        val builder = StringBuilder(text.length).apply {
            var lastEnd = 0
            tags.forEachIndexed { index, tag ->
                onProgressMade(index, 0.0)
                val result = splitTag(tag.groupValues[1], tag.groupValues[2], tag.groupValues[3]) {
                    onProgressMade(index, it)
                }
                if (result != null) {
                    append(text, lastEnd, tag.range.first)
                    append(result)
                    lastEnd = tag.range.last + 1
                }
            }
            if (lastEnd != 0) {
                append(text, lastEnd, text.length)
            }
        }
        return builder.takeIf { it.isNotEmpty() }?.toString()
    }

    private suspend fun splitTag(
        tagStart: String,
        pathData: String,
        tagEnd: String,
        onProgressMade: suspend (progress: Double) -> Unit,
    ): StringBuilder? {
        val splitter = SvgPathSplitter(pathData)
        var tag: SvgPathSplitter.Split? = splitter.next().getOrNull() ?: return null
        var lastEnd = 0
        var lastPrefix = ""

        return StringBuilder(pathData.length).apply {
            while (tag != null) {
                append(tagStart)
                append(lastPrefix)
                append(pathData, lastEnd, tag!!.skipStart)
                append(tagEnd)
                appendLine()

                lastEnd = tag!!.skipEnd
                lastPrefix = tag!!.newPrefix
                tag = splitter.next().getOrNull()
                onProgressMade(lastEnd / pathData.length.toDouble())
            }

            if (pathData.length == lastEnd) {
                deleteAt(lastIndex)
            } else {
                append(tagStart)
                append(lastPrefix)
                append(pathData, lastEnd, pathData.length)
                append(tagEnd)
            }
        }
    }
}

private const val MAX_PROGRESS = 1.0
private const val SEARCH_PROGRESS = 0.3

// One omnipotent regex to rule them (almost) all:
// - capture whitespaces before tag to match formatting
// - search only tags with MmZz commands because those are the only possible split points
private val TAG_REGEX =
    """([^\S\r\n]*<path\s+[^>]*android:pathData\s*=\s*")([^"MmZz]*[MmZz][^"]*)("[^>]*/>)""".toRegex()

// separate regex to exclude paths with evenOdd and not make the regex above even more complex
private val EVEN_ODD_REGEX = """android:fillType\s*=\s*"evenOdd"""".toRegex()
