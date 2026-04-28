package com.xsgrok.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xsgrok.app.R
import com.xsgrok.app.data.model.Character
import com.xsgrok.app.ui.XSGrokViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var characterName by remember { mutableStateOf("") }
    var characterDescription by remember { mutableStateOf("") }
    var characterRole by remember { mutableStateOf("Supporting") }
    var roleExpanded by remember { mutableStateOf(false) }
    
    val roles = listOf("Protagonist", "Antagonist", "Supporting", "Minor", "Narrator")
    
    currentNovel?.let { novel ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.characters) + " (${novel.characters.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (novel.characters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_characters),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(novel.characters) { character ->
                        CharacterCard(
                            character = character,
                            onDelete = { viewModel.deleteCharacter(character.id) }
                        )
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.add_character)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = characterName,
                            onValueChange = { characterName = it },
                            label = { Text(stringResource(R.string.name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = roleExpanded,
                            onExpandedChange = { roleExpanded = !roleExpanded }
                        ) {
                            OutlinedTextField(
                                value = characterRole,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.role)) },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = roleExpanded,
                                onDismissRequest = { roleExpanded = false }
                            ) {
                                roles.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role) },
                                        onClick = {
                                            characterRole = role
                                            roleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = characterDescription,
                            onValueChange = { characterDescription = it },
                            label = { Text(stringResource(R.string.description)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (characterName.isNotBlank()) {
                                viewModel.addCharacter(characterName, characterDescription, characterRole)
                                characterName = ""
                                characterDescription = ""
                                characterRole = "Supporting"
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCard(
    character: Character,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleSmall
                )
                AssistChip(
                    onClick = {},
                    label = { Text(character.role, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                if (character.description.isNotBlank()) {
                    Text(
                        text = character.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_character)) },
            text = { Text(stringResource(R.string.delete_character_confirm, character.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
