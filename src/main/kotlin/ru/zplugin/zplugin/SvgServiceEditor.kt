package ru.zplugin.zplugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.function.Consumer
import kotlin.math.roundToInt

@Service(Service.Level.PROJECT)
class SvgServiceEditor : ISvgServiceEditor {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var removeZJob: Job? = null

    override fun removeZ(
        text: String,
        progressConsumer: Consumer<Int>,
        hideDialog: java.lang.Runnable,
        consumer: Consumer<String>
    ) {
        if (removeZJob?.isActive == true) {
            removeZJob?.cancel()
        }
        removeZJob = coroutineScope.launch(Dispatchers.IO) {
            if (TAG_REGEX.containsMatchIn(text)) {
                hideDialog.run()
                return@launch
            }

            val tagPieces = text.split(SPLIT_REGEX)

            if (tagPieces.size != 2) {
                hideDialog.run()
                return@launch
            }

            val pathData = SPLIT_REGEX.find(text)
                ?.value
                ?.drop(PATH_DATA.length)
                ?.split(DELIMITER, ignoreCase = true)
                ?: return@launch

            val result = pathData.foldIndexed(initial = "") { index, acc, path ->
                progressConsumer.accept(
                    ((index / pathData.size.toFloat()) * MAX_PERCENT).roundToInt().coerceAtMost(MAX_PERCENT)
                )
                if (path.isNotBlank() && path != "\"") {
                    acc + tagPieces.first() + "$PATH_DATA${"\"".takeIf { path.first() != '\"' } ?: ""}" + path + "z\"" + tagPieces.last() + "\n"
                } else {
                    acc
                }
            }
            withContext(Dispatchers.Main) {
                consumer.accept(result)
                hideDialog.run()
            }
        }
    }

    override fun cancel() {
        if (removeZJob?.isActive == true) {
            removeZJob?.cancel()
        }
    }

    companion object {

        fun getInstance(project: Project): ISvgServiceEditor = project.service()
    }
}

private const val PATH_DATA = "android:pathData="
private const val DELIMITER = "z"

private const val MAX_PERCENT = 100

private val TAG_REGEX = "<path(\\s|$)+/>".toRegex()
private val SPLIT_REGEX = "android:pathData=(.+\")?".toRegex()
