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
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.ui.XSGrokViewModel

/**
 * 简化版世界观界面 - 第一性原理优化
 * 只保留核心的世界观和角色管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBuildingScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf(
        stringResource(R.string.world_background),
        stringResource(R.string.characters)
    )
    
    currentNovel?.let { novel ->
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.world_building),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = novel.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Tab栏
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // 内容区
            when (selectedTab) {
                0 -> WorldBackgroundTab(novel, viewModel)
                1 -> SimpleCharactersTab(novel, viewModel)
            }
        }
    } ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("请先选择一本小说")
        }
    }
}

@Composable
fun WorldBackgroundTab(novel: Novel, viewModel: XSGrokViewModel) {
    var worldBackground by remember { mutableStateOf(novel.worldBuilding.worldBackground) }
    var powerSystem by remember { mutableStateOf(novel.worldBuilding.powerSystem) }
    var rules by remember { mutableStateOf(novel.worldBuilding.rules) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = worldBackground,
                onValueChange = { worldBackground = it },
                label = { Text(stringResource(R.string.world_background)) },
                placeholder = { Text(stringResource(R.string.world_background_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
        
        item {
            OutlinedTextField(
                value = powerSystem,
                onValueChange = { powerSystem = it },
                label = { Text(stringResource(R.string.power_system)) },
                placeholder = { Text(stringResource(R.string.power_system_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
        
        item {
            OutlinedTextField(
                value = rules,
                onValueChange = { rules = it },
                label = { Text(stringResource(R.string.world_rules)) },
                placeholder = { Text(stringResource(R.string.world_rules_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
        
        item {
            Button(
                onClick = {
                    viewModel.updateWorldBuilding(novel.id, worldBackground, powerSystem, rules)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
fun SimpleCharactersTab(novel: Novel, viewModel: XSGrokViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<Character?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 角色列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(novel.characters) { character ->
                CharacterCard(
                    character = character,
                    onEdit = { editingCharacter = character },
                    onDelete = { viewModel.deleteCharacter(novel.id, character.id) }
                )
            }
            
            if (novel.characters.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_characters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        // 添加按钮
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_character))
            }
        }
    }
    
    // 添加角色对话框
    if (showAddDialog) {
        AddCharacterDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, role, description ->
                viewModel.addCharacter(novel.id, name, description, role)
                showAddDialog = false
            }
        )
    }
    
    // 编辑角色对话框
    editingCharacter?.let { character ->
        EditCharacterDialog(
            character = character,
            onDismiss = { editingCharacter = null },
            onConfirm = { updated ->
                viewModel.updateCharacter(novel.id, updated)
                editingCharacter = null
            }
        )
    }
}

@Composable
fun CharacterCard(
    character: Character,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = character.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
            
            if (character.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = character.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_character)) },
            text = { Text(stringResource(R.string.delete_character_confirm, character.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddCharacterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("主角") }
    var description by remember { mutableStateOf("") }
    var roleExpanded by remember { mutableStateOf(false) }
    
    val roles = listOf("主角", "配角", "反派", "导师", "伙伴")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_character)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
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
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, role, description) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditCharacterDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var name by remember { mutableStateOf(character.name) }
    var role by remember { mutableStateOf(character.role) }
    var description by remember { mutableStateOf(character.description) }
    var roleExpanded by remember { mutableStateOf(false) }
    
    val roles = listOf("主角", "配角", "反派", "导师", "伙伴")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_character)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
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
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(character.copy(name = name, role = role, description = description))
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
