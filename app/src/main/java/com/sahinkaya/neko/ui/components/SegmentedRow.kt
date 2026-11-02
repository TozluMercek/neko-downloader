package com.sahinkaya.neko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Segment<T>(val value: T, val label: String, val icon: ImageVector? = null)

/** Windows-style segmented control: outlined strip, the selected segment filled solid. */
@Composable
fun <T> SegmentedRow(
    options: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    val outline = MaterialTheme.colorScheme.outline

    BoxWithConstraints(modifier = modifier) {
        // Icons make the strip too wide for narrow phones; keep labels, drop icons there
        val showIcons = maxWidth >= 360.dp
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .clip(shape)
                .border(1.dp, outline, shape)
                .selectableGroup()
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option.value == selected
                val content = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                if (index > 0) {
                    VerticalDivider(color = outline, modifier = Modifier.fillMaxHeight())
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option.value) })
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (showIcons && option.icon != null) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = content,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
