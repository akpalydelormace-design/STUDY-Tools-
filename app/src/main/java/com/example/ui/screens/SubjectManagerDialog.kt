package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectEntity
import com.example.ui.StudyViewModel
import com.example.ui.components.parseHexColor
import com.example.ui.theme.PrimaryIndigo

@Composable
fun SubjectManagerDialog(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val subjects by viewModel.subjects.collectAsState()
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gestion des matières", fontWeight = FontWeight.Bold)
                Surface(
                    onClick = { showAddSubjectDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ajuste les coefficients de chaque matière pour que les moyennes du bulletin correspondent exactement à ta filière ou classe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjects, key = { it.id }) { subj ->
                        val parsedColor = parseHexColor(subj.colorHex)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(subj.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Coefficient : ${subj.coefficient.toInt()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { subjectToEdit = subj },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { subjectToDelete = subj },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
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

    // Edit Subject Coefficient Dialog
    if (subjectToEdit != null) {
        val editing = subjectToEdit!!
        var coefStr by remember { mutableStateOf(editing.coefficient.toInt().toString()) }
        var coefficientError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Modifier ${editing.name}") },
            text = {
                Column {
                    Text("Coefficient de matière pour ${editing.name} :")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = coefStr,
                        onValueChange = { coefStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (coefficientError != null) Text(coefficientError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newCoef = coefStr.toFloatOrNull()
                    if (newCoef != null && newCoef > 0f) {
                        viewModel.updateSubject(editing.copy(coefficient = newCoef))
                        subjectToEdit = null
                    } else {
                        coefficientError = "Le coefficient de la matière doit être strictement supérieur à 0."
                    }
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var newSubjName by remember { mutableStateOf("") }
        var newSubjCoef by remember { mutableStateOf("") }
        var coefficientError by remember { mutableStateOf<String?>(null) }
        var selectedColorHex by remember { mutableStateOf("#4F46E5") }
        val colorPalette = listOf(
            "#4F46E5", "#0284C7", "#10B981", "#F59E0B", "#F43F5E",
            "#8B5CF6", "#EC4899", "#14B8A6", "#64748B"
        )

        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Ajouter une matière") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSubjName,
                        onValueChange = { newSubjName = it },
                        label = { Text("Nom de la matière *") },
                        placeholder = { Text("ex: Économie, SVT, Espagnol...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_subject_name_input")
                    )

                    OutlinedTextField(
                        value = newSubjCoef,
                        onValueChange = { newSubjCoef = it },
                        label = { Text("Coefficient de la matière *") },
                        placeholder = { Text("ex: 2") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (coefficientError != null) Text(coefficientError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

                    Text("Couleur associée :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorPalette.forEach { hex ->
                            val c = parseHexColor(hex)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedColorHex = hex }
                                    .then(
                                        if (selectedColorHex == hex) {
                                            Modifier.clip(CircleShape)
                                        } else Modifier
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val coef = newSubjCoef.replace(",", ".").toFloatOrNull()
                        if (newSubjName.isBlank()) {
                            coefficientError = "Le nom de la matière est obligatoire."
                        } else if (coef == null || coef <= 0f) {
                            coefficientError = "Le coefficient de la matière doit être strictement supérieur à 0."
                        } else {
                            viewModel.addSubject(
                                name = newSubjName.trim(),
                                coefficient = coef,
                                colorHex = selectedColorHex,
                                iconName = "School"
                            )
                            showAddSubjectDialog = false
                        }
                    },
                    enabled = newSubjName.isNotBlank()
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Delete Subject Dialog
    if (subjectToDelete != null) {
        val subj = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Supprimer la matière ?") },
            text = {
                Text("Supprimer ${subj.name} supprimera également toutes les notes associées à cette matière dans le bulletin.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subj)
                        subjectToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
