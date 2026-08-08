package com.stayalert.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stayalert.R
import com.stayalert.data.Permission
import com.stayalert.data.PermissionState
import com.stayalert.data.PermissionStatus
import com.stayalert.ui.theme.Accent
import com.stayalert.ui.theme.BorderHairline
import com.stayalert.ui.theme.InkDisabled
import com.stayalert.ui.theme.SurfaceRaised

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val targetPackage by viewModel.targetPackage.collectAsStateWithLifecycle()
    val targetActivity by viewModel.targetActivity.collectAsStateWithLifecycle()
    val batteryExempt by viewModel.batteryExempt.collectAsStateWithLifecycle()
    val targetInstalled by viewModel.targetInstalled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = "Permisos",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            border = BorderStroke(1.dp, BorderHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                permissions.forEachIndexed { index, status ->
                    PermissionRow(
                        status = status,
                        onClick = {
                            if (status.state == PermissionState.PENDING) {
                                runCatching {
                                    context.startActivity(permissionSettingsIntent(context, status.permission))
                                }.onFailure { e ->
                                    android.util.Log.e("SettingsScreen", "No se pudo abrir Ajustes para ${status.permission}", e)
                                }
                            }
                        }
                    )
                    if (index < permissions.lastIndex) {
                        HorizontalDivider(
                            color = BorderHairline,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = "App objetivo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            border = BorderStroke(1.dp, BorderHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = targetPackage,
                    onValueChange = viewModel::setTargetPackage,
                    label = { Text("Paquete") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetActivity,
                    onValueChange = viewModel::setTargetActivity,
                    label = { Text("Actividad principal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!targetInstalled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "La app objetivo no está instalada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Text(
            text = "Batería",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            border = BorderStroke(1.dp, BorderHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }.onFailure { e ->
                            android.util.Log.e("SettingsScreen", "No se pudo abrir exención de batería", e)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exención de batería",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (batteryExempt) "Exento" else "No exento. Tócalo para abrir Ajustes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (batteryExempt) Accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = if (batteryExempt) Accent else InkDisabled)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    status: PermissionStatus,
    onClick: () -> Unit
) {
    val pending = status.state == PermissionState.PENDING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = pending, onClick = onClick)
            .semantics {
                stateDescription = if (pending) "Pendiente" else "Concedido"
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (status.permission) {
                    Permission.OVERLAY -> "Permiso de overlay"
                    Permission.NOTIFICATIONS -> "Permiso de notificaciones"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (pending) {
                    "Falta el permiso. Tócalo para abrir Ajustes."
                } else {
                    "Concedido"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (pending) MaterialTheme.colorScheme.onSurfaceVariant else Accent
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = if (pending) InkDisabled else Accent)
        }
    }
}

private fun permissionSettingsIntent(context: Context, permission: Permission): Intent =
    when (permission) {
        Permission.OVERLAY -> Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        Permission.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
