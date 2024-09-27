package ru.zplugin.zplugin.domain

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import ru.zplugin.zplugin.domain.helper.SvgPathHelper
import ru.zplugin.zplugin.presentation.Notifier
import java.util.function.Consumer

@Service(Service.Level.PROJECT)
class SvgServiceEditor : ISvgServiceEditor {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private val notifier = Notifier()

    private val pathHelper = SvgPathHelper()

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
        removeZJob = coroutineScope.launch(Dispatchers.Default) {
            try {
                val splitByZText = pathHelper.getSplitByZText(
                    text = text,
                    progressConsumer = progressConsumer
                )
                withContext(Dispatchers.EDT) {
                    consumer.accept(splitByZText)
                    hideDialog.run()
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                withContext(Dispatchers.EDT) {
                    notifier.notifyError(project = project, message = e.message ?: e.toString())
                    hideDialog.run()
                }
            } finally {
                withContext(Dispatchers.EDT) {
                    hideDialog.run()
                    notifier.notifyError(project = project, message = "Z Plugin work finished!")
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
