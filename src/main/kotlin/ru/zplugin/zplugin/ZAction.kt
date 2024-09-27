package ru.zplugin.zplugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.registerOrReplaceServiceInstance
import com.intellij.util.ui.JBDimension
import ru.zplugin.zplugin.domain.ISvgServiceEditor
import ru.zplugin.zplugin.domain.SvgServiceEditor
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JProgressBar

private val Log = logger<ZAction>()

class ZAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        // Set visibility only in the case of
        // existing project editor, and selection
        e.presentation.isEnabledAndVisible = project != null && editor != null && editor.selectionModel.hasSelection()
        Log.info("call update")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: run {
            Log.info("project is null")
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: run {
            Log.info("editor is null")
            return
        }

        val (builder, progress) = getDialogProgressBuilder()

        val zService = getSvgService(project = project)

        val document = editor.document
        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd

        val text = document.getText(TextRange(start, end))

        zService.removeZ(
            project = project,
            text = text,
            progressConsumer = { currentProgress ->
                progress.value = currentProgress
            },
            hideDialog = {
                builder.dialogWrapper.doCancelAction()
            }
        ) { handledText ->
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(start, end, handledText)
            }
        }

        builder.addDisposable {
            zService.cancel()
        }
        Log.info("call actionPerformed")
        builder.show()
    }

    private fun getSvgService(project: Project): ISvgServiceEditor {
        return project.serviceOrNull<ISvgServiceEditor>() ?: run {
            Log.info("create new service!!!")
            SvgServiceEditor.getInstance(project).also { zService ->
                project.registerOrReplaceServiceInstance(ISvgServiceEditor::class.java, zService) {
                    Log.info("DISPOSE SERVICE")
                }
            }
        }
    }

    private fun getDialogProgressBuilder(): Pair<DialogBuilder, JProgressBar> {
        val builder = DialogBuilder()
        builder.addCancelAction()
        builder.title(buildString { append("Make more path`s by z split") })
        val dialogPanel = JPanel()
        dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
        val progress = JProgressBar(0, 0, 100)
        dialogPanel.add(Box.createRigidArea(JBDimension(40, 30)))
        dialogPanel.add(progress)
        dialogPanel.add(Box.createRigidArea(JBDimension(40, 30)))
        builder.setCenterPanel(dialogPanel)
        return builder to progress
    }
}
