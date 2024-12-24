package org.jetbrains.compose.hotreload

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Immutable
internal interface AdaptiveScope {
    @Stable
    fun Modifier.weight(weight: Float): Modifier
}

private class AdaptiveRowScopeInstance(private val row: RowScope) : AdaptiveScope {
    @Stable
    override fun Modifier.weight(weight: Float): Modifier {
        with(row) {
            return this@weight.weight(weight)
        }
    }
}

private class AdaptiveColumnScopeInstance(private val column: ColumnScope) : AdaptiveScope {
    @Stable
    override fun Modifier.weight(weight: Float): Modifier {
        with(column) {
            return this@weight.weight(weight)
        }
    }
}

@Composable
internal fun AdaptiveContainer(
    isHorizontal: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable AdaptiveScope.() -> Unit
) {
    BoxWithConstraints {
        val localContent = movableContentOf(content)
        if (isHorizontal xor (maxWidth > maxHeight)) {
            Column(modifier) {
                val adaptiveScope: AdaptiveScope = remember { AdaptiveColumnScopeInstance(this) }
                localContent(adaptiveScope)
            }
        } else {
            Row(modifier) {
                val adaptiveScope: AdaptiveScope = remember { AdaptiveRowScopeInstance(this) }
                localContent(adaptiveScope)
            }
        }
    }
}