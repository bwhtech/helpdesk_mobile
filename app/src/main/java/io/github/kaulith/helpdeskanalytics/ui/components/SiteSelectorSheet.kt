package io.github.kaulith.helpdeskanalytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.domain.model.SiteProfile
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSelectorSheet(
    activeSite: SiteProfile,
    otherSites: List<SiteProfile>,
    onSiteSelected: (SiteProfile) -> Unit,
    onAddSite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surfaceContainerLow,
        contentColor = cs.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Select site",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = cs.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            SectionLabel("Current")
            SiteRow(site = activeSite, isSelected = true, onClick = onDismiss)

            if (otherSites.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                SectionLabel("Other sites")
                otherSites.forEach { site ->
                    SiteRow(site = site, isSelected = false, onClick = {
                        onSiteSelected(site)
                        onDismiss()
                    })
                }
            }

            HorizontalDivider(color = cs.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        onAddSite()
                        onDismiss()
                    }
                    .padding(vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Add site",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurface,
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Spacing.xs),
    )
}

@Composable
private fun SiteRow(site: SiteProfile, isSelected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) cs.primaryContainer else cs.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        InitialsAvatar(name = site.siteUrl, size = 40.dp, isSelected = isSelected)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = site.siteUrl,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) cs.onPrimaryContainer else cs.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            site.userEmail?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) cs.onPrimaryContainer.copy(alpha = 0.7f)
                        else cs.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = cs.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
