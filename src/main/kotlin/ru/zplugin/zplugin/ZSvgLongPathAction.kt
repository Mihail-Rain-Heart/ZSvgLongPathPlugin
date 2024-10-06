package ru.zplugin.zplugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.testFramework.registerOrReplaceServiceInstance
import ru.zplugin.zplugin.domain.ISvgLongPathServiceEditor
import ru.zplugin.zplugin.domain.SvgLongPathServiceEditor

private val logger = logger<ZSvgLongPathAction>()

class ZSvgLongPathAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        // Set visibility only in the case of
        // existing project editor, and selection
        e.presentation.isEnabledAndVisible = project != null && editor != null && editor.selectionModel.hasSelection()
        logger.info("call update")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: run {
            logger.info("project is null")
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: run {
            logger.info("editor is null")
            return
        }

        getSvgService(project = project).splitSvgLongPathByZ(editor = editor, logger = logger)
    }

    private fun getSvgService(project: Project): ISvgLongPathServiceEditor {
        return project.serviceOrNull<ISvgLongPathServiceEditor>() ?: run {
            SvgLongPathServiceEditor.getInstance(project).also { zService ->
                project.registerOrReplaceServiceInstance(ISvgLongPathServiceEditor::class.java, zService) {
                    logger.info("DISPOSE SERVICE")
                }
            }
        }
    }
}
