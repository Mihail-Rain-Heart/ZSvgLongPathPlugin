package ru.zplugin.zplugin

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.constrainedReadAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.withUiContext
import kotlinx.coroutines.*
import ru.zplugin.zplugin.presentation.Notifier
import java.util.function.Consumer
import kotlin.math.roundToInt

@Service(Service.Level.PROJECT)
class SvgServiceEditor : ISvgServiceEditor {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private val notifier = Notifier()

    private var removeZJob: Job? = null

    override fun removeZ(
        project: Project,
        text: String,
        progressConsumer: Consumer<Int>,
        hideDialog: java.lang.Runnable,
        consumer: Consumer<String>
    ) {
        if (removeZJob?.isActive == true) {
            removeZJob?.cancel()
        }
        removeZJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                if (!TAG_REGEX.containsMatchIn(text)) {
                    withContext(Dispatchers.EDT) {
                        notifier.notifyError(project = project, message = "Unsupported")
                        hideDialog.run()
                    }
                    return@launch
                }
            } catch (e: Exception) {
                notifier.notifyError(project = project, message = "Unsupported: $text; error: $e")
                hideDialog.run()
            }

            val tagPieces = text.split(SPLIT_REGEX)

            if (tagPieces.size != 2) {
                withContext(Dispatchers.EDT) {
                    hideDialog.run()
                    notifier.notifyError(project = project, message = "can`t split: $text")
                }
                return@launch
            }

            val pathData = SPLIT_REGEX.find(text)
                ?.value
                ?.drop(PATH_DATA.length)
                ?.split(DELIMITER, ignoreCase = true)
                ?: run {
                    hideDialog.run()
                    notifier.notifyError(project = project, message = "pathData is null for: $text")
                    return@launch
                }

            try {
                val result = pathData.foldIndexed(initial = "") { index, acc, path ->
                    progressConsumer.accept(
                        ((index / pathData.size.toFloat().coerceAtLeast(1f)) * MAX_PERCENT).roundToInt().coerceAtMost(MAX_PERCENT)
                    )
                    if (path.isNotBlank() && path != "\"") {
                        acc + tagPieces.first() + "$PATH_DATA${"\"".takeIf { path.first() != '\"' } ?: ""}" + path + "z\"" + tagPieces.last() + "\n"
                    } else {
                        acc
                    }
                }
                withContext(Dispatchers.EDT) {
                    consumer.accept(result)
                    hideDialog.run()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.EDT) {
                    hideDialog.run()
                    notifier.notifyError(project = project, message = "error: $e")
                }
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

private val TAG_REGEX = "<path\\s+[^>]*\\bandroid:pathData=\"[^\"]*\"\\s*[^>]*/>".toRegex()
private val SPLIT_REGEX = "android:pathData=(.+\")?".toRegex()
