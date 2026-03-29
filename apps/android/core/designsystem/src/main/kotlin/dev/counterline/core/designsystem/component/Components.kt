package dev.counterline.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** A card displaying a repertoire line summary */
@Composable
fun LineCard(
    name: String,
    eco: String,
    seedLine: String,
    scorePct: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = eco,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = seedLine,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (scorePct / 100.0).toFloat() },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${scorePct}%",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** A badge-style chip for proof status or disclaimers */
@Composable
fun StatusBadge(
    text: String,
    type: BadgeType = BadgeType.INFO,
    modifier: Modifier = Modifier,
) {
    val (icon, tint) = when (type) {
        BadgeType.SUCCESS -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.secondary
        BadgeType.WARNING -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        BadgeType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = tint.copy(alpha = 0.12f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
            )
        }
    }
}

enum class BadgeType { SUCCESS, WARNING, INFO }

/** Section header used across screens */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Move notation text with proper monospace styling */
@Composable
fun MoveText(
    moveNumber: Int,
    san: String,
    isWhiteMove: Boolean,
    modifier: Modifier = Modifier,
) {
    val prefix = if (isWhiteMove) "$moveNumber." else "$moveNumber..."
    Text(
        text = "$prefix $san",
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier,
    )
}
