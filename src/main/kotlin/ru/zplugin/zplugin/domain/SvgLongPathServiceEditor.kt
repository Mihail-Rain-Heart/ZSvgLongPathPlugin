package ru.zplugin.zplugin.domain

import com.intellij.openapi.application.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.withModalProgressContext
import com.intellij.openapi.util.TextRange
import kotlinx.coroutines.*
import ru.zplugin.zplugin.domain.error.ZSvgLongPathException.Companion.getMessageText
import ru.zplugin.zplugin.domain.helper.SvgLongPathHelper
import ru.zplugin.zplugin.presentation.ZSvgLongPathNotifier

@Service(Service.Level.PROJECT)
class SvgLongPathServiceEditor(private val project: Project) : ISvgLongPathServiceEditor {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private val notifier = ZSvgLongPathNotifier()

    private val pathHelper = SvgLongPathHelper()

    private var splitByZJob: Job? = null

    override fun splitSvgLongPathByZ(editor: Editor, logger: Logger) {
        val document = editor.document
        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd

        if (splitByZJob?.isActive == true) {
            splitByZJob?.cancel()
        }

        splitByZJob = scope.launch(context = Dispatchers.Default) {
            try {
                splitSvgLongPathByZInternal(
                    logger = logger,
                    document = document,
                    start = start,
                    end = end
                )
            } catch (error: Throwable) {
                if (error !is CancellationException) {
                    logger.error(error)
                    notifier.notifyError(
                        project = project,
                        message = error.getMessageText()
                    )
                }
                throw error
            } finally {
                logger.info("SvgServiceEditor are finished in splitByZJob!")
            }
        }
    }

    override fun cancel() {
        if (splitByZJob?.isActive == true) {
            splitByZJob?.cancel()
        }
    }

    private suspend fun splitSvgLongPathByZInternal(
        logger: Logger,
        document: Document,
        start: Int,
        end: Int
    ) {
        val text = runReadAction {
            document.getText(TextRange(start, end))
        }

        withModalProgressContext(
            title = PROGRESS_MODAL_TITLE,
            canBeCancelled = true,
            isIndeterminate = false,
            project = project
        ) {
            val splitText = pathHelper.getSplitText(text = text) { stage, step, progress ->
                withContext(Dispatchers.Main) {
                    indicator.text = stage
                    indicator.text2 = step
                    indicator.fraction = progress
                }
            }
            logger.debug("handled: $splitText")
            withContext(context = Dispatchers.EDT) {
                if (splitText == null) {
                    notifier.notifyInfo(project = project, message = "Z Plugin can't split any tags.")
                } else {
                    ApplicationManager.getApplication().invokeLater {
                        WriteCommandAction.runWriteCommandAction(project) {
                            document.replaceString(start, end, splitText)
                        }
                    }
                    notifier.notifyInfo(project = project, message = "Z Plugin work finished!")
                }
            }
        }
    }

    companion object {

        fun getInstance(project: Project): ISvgLongPathServiceEditor = project.service<SvgLongPathServiceEditor>()
    }
}

private const val PROGRESS_MODAL_TITLE = "Process svg long path ..."
