package com.wanderingledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Standard alert dialog for confirmations and messages.
 */
@Composable
fun WLAlertDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            WLTextButton(
                text = confirmText,
                onClick = onConfirm ?: onDismiss
            )
        },
        dismissButton = if (dismissText != null) {
            {
                WLTextButton(
                    text = dismissText,
                    onClick = onDismiss
                )
            }
        } else null,
        modifier = modifier
    )
}

/**
 * Custom dialog for more complex content.
 */
@Composable
fun WLDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            content()
        }
    }
}

/**
 * Confirmation dialog for destructive or important actions.
 */
@Composable
fun WLConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    WLAlertDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        modifier = modifier
    )
}

/**
 * Result dialog for showing travel or transaction outcomes.
 */
@Composable
fun WLResultDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = true
) {
    WLDialog(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = if (isSuccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                WLTextButton(
                    text = "OK",
                    onClick = onDismiss
                )
            }
        }
    }
}
