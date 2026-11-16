package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ServiceStatus
import com.example.data.update.InAppUpdateUiState
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings

fun iconForKey(key: String): ImageVector = when (key) {
    "car" -> Icons.Filled.DirectionsCar
    "bike" -> Icons.Filled.DirectionsBike
    "house" -> Icons.Filled.HomeWork
    "bag" -> Icons.Filled.ShoppingBag
    "bolt" -> Icons.Filled.Bolt
    "tv" -> Icons.Filled.Tv
    "heart" -> Icons.Filled.Favorite
    "building" -> Icons.Filled.Apartment
    "fork" -> Icons.Filled.Restaurant
    "rupee" -> Icons.Filled.CurrencyRupee
    "drop" -> Icons.Filled.WaterDrop
    "wrench" -> Icons.Filled.Build
    "link" -> Icons.Filled.Link
    "gear" -> Icons.Filled.Settings
    "shield" -> Icons.Filled.Shield
    "wifi" -> Icons.Filled.Wifi
    "fuel" -> Icons.Filled.LocalGasStation
    "file" -> Icons.Filled.ReceiptLong
    "bell" -> Icons.Filled.Notifications
    "scan" -> Icons.Filled.DocumentScanner
    "sparkle" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.AccountBalanceWallet
}

@Composable
fun selavuTextFieldColors(
    containerColor: Color = MaterialTheme.colorScheme.background
): TextFieldColors {
    val colorScheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = colorScheme.surfaceVariant,
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedLabelColor = colorScheme.onPrimaryContainer,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        focusedPlaceholderColor = colorScheme.outline,
        unfocusedPlaceholderColor = colorScheme.outline,
        cursorColor = colorScheme.primary
    )
}

@Composable
fun SelavuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.medium,
        colors = selavuTextFieldColors(containerColor),
        modifier = modifier
    )
}

@Composable
fun SelavuMainHeader(
    title: String,
    subtitle: String? = null,
    userDisplayName: String? = null,
    userPhotoUrl: String? = null,
    onOpenProfile: () -> Unit,
    profileTestTag: String,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (trailingContent != null) {
                trailingContent()
            }
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .clickable(onClick = onOpenProfile)
                    .testTag(profileTestTag),
                contentAlignment = Alignment.Center
            ) {
                val initial = userDisplayName?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
                when {
                    !userPhotoUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "Profile and settings",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                    }

                    initial != null -> {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile and settings",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelavuEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    SelavuCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedButton(
                    onClick = onAction,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onPrimaryContainer),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun InAppUpdateBanner(
    state: InAppUpdateUiState,
    onStartUpdate: () -> Unit,
    onCompleteUpdate: () -> Unit,
    onIgnoreUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    AnimatedVisibility(
        visible = state !is InAppUpdateUiState.Hidden,
        enter = expandVertically(tween(240)) + fadeIn(tween(240)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(colorScheme.surface)
                    .border(1.2.dp, colorScheme.primary.copy(alpha = 0.45f), MaterialTheme.shapes.large)
                    .padding(14.dp)
                    .testTag("in_app_update_banner"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (state) {
                    is InAppUpdateUiState.Available -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SystemUpdate,
                                    contentDescription = "Update available",
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Update available",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "A newer build (v${state.availableVersionCode}) is ready to download while you use RecapSpends.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onIgnoreUpdate(state.availableVersionCode) },
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("in_app_update_ignore_button")
                            ) {
                                Text(
                                    text = "Ignore",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onStartUpdate,
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("in_app_update_button")
                            ) {
                                Text("Update", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    is InAppUpdateUiState.Downloading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading update…",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = SelavuTheme.textStyles.numericCaption,
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                        SelavuProgressBar(progress = state.progress, color = colorScheme.primary, height = 6.dp)
                    }

                    is InAppUpdateUiState.Downloaded -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(extendedColors.okSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Update ready",
                                    tint = extendedColors.ok,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Update ready to install",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Restart RecapSpends to finish applying the update.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onIgnoreUpdate(state.availableVersionCode) },
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("in_app_update_ignore_button")
                            ) {
                                Text(
                                    text = "Later",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onCompleteUpdate,
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = extendedColors.ok,
                                    contentColor = colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("in_app_update_restart_button")
                            ) {
                                Text("Restart", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    InAppUpdateUiState.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
fun CategoryTile(
    iconKey: String,
    colorIndex: Int,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val extendedColors = SelavuTheme.extendedColors
    val tint = extendedColors.categoryColor(colorIndex)
    val bg = tint.copy(alpha = if (extendedColors.isDark) 0.22f else 0.14f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconForKey(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    status: ServiceStatus? = null,
    isAccent: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val (bg, fg) = when {
        status == ServiceStatus.OVERDUE -> colorScheme.errorContainer to colorScheme.error
        status == ServiceStatus.DUE_SOON -> extendedColors.warnSoft to extendedColors.warn
        status == ServiceStatus.OK -> extendedColors.okSoft to extendedColors.ok
        isAccent -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        else -> colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SelavuProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 7.dp,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(350),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceAtLeast(0.02f))
                .height(height)
                .clip(RoundedCornerShape(99.dp))
                .background(color)
        )
    }
}

@Composable
fun SelavuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val base = modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .background(colorScheme.surface)
        .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.large)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(16.dp)
    Box(modifier = base) {
        content()
    }
}

@Composable
fun SelavuListContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        content()
    }
}

@Composable
fun SelavuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = SelavuTheme.textStyles.sectionTitle,
            color = colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SelavuTopNavBar(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(44.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(colorScheme.surface)
                .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .clickable(onClick = onBack)
                .testTag("nav_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun SelavuSegmentedControl(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (key, label) ->
            val selected = key == selectedKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(1.dp, MaterialTheme.shapes.small)
                                .background(colorScheme.surface)
                        } else Modifier
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SelavuFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bg = if (selected) colorScheme.primaryContainer else colorScheme.surface
    val border = if (selected) colorScheme.primary else colorScheme.outlineVariant
    val fg = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
fun SelavuBottomBar(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    onFabClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = selavuStrings()
    Surface(
        color = colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            SelavuDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BottomTabItem(
                    label = strings.tabHome,
                    selected = currentTab == "home",
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onSelectTab("home") },
                    modifier = Modifier.weight(1f).testTag("tab_home")
                )
                BottomTabItem(
                    label = strings.tabTrack,
                    selected = currentTab == "track",
                    selectedIcon = Icons.Filled.GridView,
                    unselectedIcon = Icons.Outlined.GridView,
                    onClick = { onSelectTab("track") },
                    modifier = Modifier.weight(1f).testTag("tab_track")
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-12).dp)
                            .size(56.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colorScheme.primary)
                            .border(4.dp, colorScheme.surface, CircleShape)
                            .clickable(onClick = onFabClick)
                            .testTag("fab_add"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                BottomTabItem(
                    label = strings.tabBills,
                    selected = currentTab == "bills",
                    selectedIcon = Icons.Filled.Notifications,
                    unselectedIcon = Icons.Outlined.Notifications,
                    onClick = { onSelectTab("bills") },
                    modifier = Modifier.weight(1f).testTag("tab_bills")
                )
                BottomTabItem(
                    label = strings.tabInsights,
                    selected = currentTab == "insights",
                    selectedIcon = Icons.Filled.PieChart,
                    unselectedIcon = Icons.Outlined.PieChart,
                    onClick = { onSelectTab("insights") },
                    modifier = Modifier.weight(1f).testTag("tab_insights")
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val tint = if (selected) colorScheme.onPrimaryContainer else colorScheme.outline
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) colorScheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = label,
            style = SelavuTheme.textStyles.badgeMicro.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class CategoryShareSlice(
    val categoryId: String,
    val name: String,
    val colorIndex: Int,
    val iconKey: String,
    val amount: Double
)

@Composable
fun CategoryStackedBar(slices: List<CategoryShareSlice>) {
    val extendedColors = SelavuTheme.extendedColors
    val total = slices.sumOf { it.amount }.coerceAtLeast(1.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(99.dp))
            .semantics { contentDescription = "Spending split by category" },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        slices.forEach { slice ->
            val weight = (slice.amount / total).toFloat().coerceAtLeast(0.02f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(12.dp)
                    .background(extendedColors.categoryColor(slice.colorIndex))
            )
        }
    }
}

@Composable
fun DonutSpendChart(
    slices: List<CategoryShareSlice>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val sliceColors = slices.map { extendedColors.categoryColor(it.colorIndex) }
    val trackColor = colorScheme.surfaceVariant
    val safeTotal = totalAmount.coerceAtLeast(1.0)
    Box(
        modifier = modifier
            .size(200.dp)
            .semantics { contentDescription = "Donut chart of spending by category" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokePx = 24.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            if (slices.isEmpty()) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
            } else {
                var startAngle = -90f
                val gapDeg = if (slices.size > 1) 2.5f else 0f
                slices.forEachIndexed { idx, slice ->
                    val sweep = ((slice.amount / safeTotal) * 360.0).toFloat()
                    val visSweep = (sweep - gapDeg).coerceAtLeast(1f)
                    drawArc(
                        color = sliceColors[idx],
                        startAngle = startAngle,
                        sweepAngle = visSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = IndianFormatters.formatLakhCrore(totalAmount),
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onSurface
            )
            Text(
                text = "total spend",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MonthlyTrendBarChart(
    months: List<Pair<String, Double>>,
    budget: Double,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val context = LocalContext.current
    val manropeTypeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.manrope)
        } catch (_: Exception) {
            null
        }
    }
    val maxVal = maxOf(budget * 1.12, months.maxOfOrNull { it.second } ?: 70000.0, 10000.0)
    val gridColor = colorScheme.outlineVariant
    val accentColor = colorScheme.primary
    val mutedBar = colorScheme.surfaceContainerHighest
    val warnColor = extendedColors.warn
    val labelArgb = colorScheme.onSurfaceVariant.toArgb()
    val boldArgb = colorScheme.onSurface.toArgb()
    val warnArgb = extendedColors.warn.toArgb()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics { contentDescription = "Monthly spending bar chart for last six months" }
    ) {
        val leftPad = 36.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 20.dp.toPx()
        val bottomPad = 26.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = labelArgb
            textSize = 10.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = manropeTypeface
        }

        val steps = 3
        for (i in 0..steps) {
            val value = (maxVal / steps) * i
            val y = topPad + chartH - ((value / maxVal).toFloat() * chartH)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = 1.dp.toPx()
            )
            val tickText = if (value < 1000) "0" else "${(value / 1000).toInt()}k"
            drawContext.canvas.nativeCanvas.drawText(
                tickText,
                leftPad - 6.dp.toPx(),
                y + 4.dp.toPx(),
                labelPaint
            )
        }

        val stepW = chartW / months.size.coerceAtLeast(1)
        val barW = (stepW * 0.56f).coerceAtMost(32.dp.toPx())

        val monthPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = labelArgb
            textSize = 11.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = manropeTypeface
        }
        val boldPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = boldArgb
            textSize = 11.sp.toPx()
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = manropeTypeface
        }

        months.forEachIndexed { index, (label, amount) ->
            val isLast = index == months.lastIndex
            val barH = ((amount / maxVal).toFloat() * chartH).coerceAtLeast(2.dp.toPx())
            val x = leftPad + index * stepW + (stepW - barW) / 2f
            val y = topPad + chartH - barH

            drawRoundRect(
                color = if (isLast) accentColor else mutedBar,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barW / 2f,
                size.height - 6.dp.toPx(),
                if (isLast) boldPaint else monthPaint
            )
            if (isLast) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${(amount / 1000).toInt()}k",
                    x + barW / 2f,
                    y - 6.dp.toPx(),
                    boldPaint
                )
            }
        }

        val budgetY = topPad + chartH - ((budget / maxVal).toFloat() * chartH)
        drawLine(
            color = warnColor,
            start = Offset(leftPad, budgetY),
            end = Offset(size.width - rightPad, budgetY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
        val budgetPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = warnArgb
            textSize = 10.5.sp.toPx()
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = manropeTypeface
        }
        drawContext.canvas.nativeCanvas.drawText(
            "Budget ${(budget / 1000).toInt()}k",
            size.width - rightPad,
            budgetY - 5.dp.toPx(),
            budgetPaint
        )
    }
}
