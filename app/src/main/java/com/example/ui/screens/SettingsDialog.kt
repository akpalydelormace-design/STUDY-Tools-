package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudyViewModel
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo

@Composable
fun SettingsDialog(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()

    var showExportResultDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Paramètres & Données", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Theme selection
                item {
                    Text("Apparence :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = currentTheme == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Auto") }
                        )
                        FilterChip(
                            selected = currentTheme == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") },
                            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Clair") }
                        )
                        FilterChip(
                            selected = currentTheme == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Sombre") }
                        )
                    }
                }

                // 2. Data Backup & Transfer (Offline Export / Import)
                item {
                    Text("Sauvegarde locale & Transfert :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Exporte toutes tes données (notes, bulletin, agenda, documents) dans un fichier JSON pour les sauvegarder ou changer de téléphone sans aucun cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                viewModel.exportBackupJson { json ->
                                    showExportResultDialog = json
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).testTag("export_data_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exporter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            }
                        }

                        Surface(
                            onClick = { showImportDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).testTag("import_data_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Importer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                            }
                        }
                    }
                }

                // 2b. Historique
                item {
                    Text("Historique de navigation :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        onClick = {
                            onDismiss()
                            onOpenHistory()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().testTag("open_history_settings_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Consulter l'historique",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Recherche, reprise d'activité et gestion de l'historique",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. Reset all data
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Zone de danger :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Surface(
                        onClick = { showResetConfirmDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().testTag("reset_all_data_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Réinitialiser toutes les données", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Text("Efface toutes les notes, devoirs et bulletins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 4. Privacy & Vision Statement
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("100% Hors-Ligne & Privacy First", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Study Tools ne requiert aucun compte, aucune adresse e-mail et aucune connexion Internet. Toutes tes données restent strictement sur ton téléphone.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )

    // Export Result Dialog
    if (showExportResultDialog != null) {
        val json = showExportResultDialog!!
        AlertDialog(
            onDismissRequest = { showExportResultDialog = null },
            title = { Text("Exportation réussie") },
            text = {
                Column {
                    Text(
                        "Toutes tes données sont compilées au format JSON standard ci-dessous. Tu peux copier ce texte pour le sauvegarder dans un fichier ou le transférer sur un autre appareil :",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("StudyToolsBackup", json))
                    Toast.makeText(context, "Sauvegarde copiée dans le presse-papier !", Toast.LENGTH_LONG).show()
                    showExportResultDialog = null
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier dans le presse-papier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportResultDialog = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var jsonToImport by remember { mutableStateOf("") }
        var isImporting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importer une sauvegarde") },
            text = {
                Column {
                    Text(
                        "Colle le contenu JSON de ta sauvegarde précédente pour restaurer tes matières, devoirs, notes et bulletins :",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonToImport,
                        onValueChange = { jsonToImport = it },
                        placeholder = { Text("{\"app\": \"Study Tools\", ...}") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("import_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonToImport.isNotBlank()) {
                            isImporting = true
                            viewModel.importBackupJson(jsonToImport) { success ->
                                isImporting = false
                                if (success) {
                                    Toast.makeText(context, "Données restaurées avec succès !", Toast.LENGTH_LONG).show()
                                    showImportDialog = false
                                } else {
                                    Toast.makeText(context, "Format de sauvegarde invalide.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = jsonToImport.isNotBlank() && !isImporting
                ) {
                    Text("Restaurer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Effacer toutes les données ?") },
            text = {
                Text("Attention : Cette action effacera irrémédiablement toutes tes matières, tes notes de bulletin, ton agenda et tes cours écrits.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData {
                            Toast.makeText(context, "Données réinitialisées", Toast.LENGTH_SHORT).show()
                        }
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tout supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
