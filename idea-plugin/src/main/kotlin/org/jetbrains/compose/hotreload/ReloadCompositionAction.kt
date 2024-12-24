package org.jetbrains.compose.hotreload

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class ReloadCompositionAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = run {
            val service = event.project?.service<HotReloadService>() ?: return@run false
            service.engine.appStateFlow.value == AppState.RUNNING
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.project?.service<HotReloadService>()?.engine?.reload()
    }
}