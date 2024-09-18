package ru.zplugin.zplugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class SvgServiceEditor(
    private val project: Project,
    private val coroutineScope: CoroutineScope
): ISvgServiceEditor {


}