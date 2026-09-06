package com.ritik.wordpuzzle.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme

/**
 * Shared long-form learning surface.
 *
 * The header and optional footer stay fixed. The body owns the only vertical
 * scroll, so story paragraphs and glossary rows must remain ordinary children.
 * [contentKey] resets the body position when a different lesson/chapter opens.
 */
@Composable
fun LearningStoryModal(
    visible: Boolean,
    title: String,
    subtitle: String = "",
    contentKey: Any,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return

    val colors = WordPuzzleTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .background(
                    brush = Brush.verticalGradient(listOf(colors.surfaceElevated, colors.surface)),
                    shape = RoundedCornerShape(28.dp),
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                if (showClose) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close learning story",
                            tint = colors.textPrimary,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
            )

            // Keep the scroll state inside a content key. A category chapter or
            // a new level must never inherit the previous lesson's scroll offset.
            key(contentKey) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }

            if (footer != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.08f))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    footer()
                }
            }
        }
    }
}
