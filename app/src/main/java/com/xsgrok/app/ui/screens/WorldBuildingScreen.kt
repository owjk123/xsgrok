package com.xsgrok.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.xsgrok.app.data.model.*
import com.xsgrok.app.ui.XSGrokViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBuildingScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf(
        stringResource(R.string.world_background),
        stringResource(R.string.characters),
        stringResource(R.string.locations),
        stringResource(R.string.factions),
        stringResource(R.string.items),
        stringResource(R.string.skills),
        stringResource(R.string.timeline)
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
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }
            
            // 内容区
            when (selectedTab) {
                0 -> WorldBackgroundTab(novel, viewModel)
                1 -> CharactersTab(novel, viewModel)
                2 -> LocationsTab(novel, viewModel)
                3 -> FactionsTab(novel, viewModel)
                4 -> ItemsTab(novel, viewModel)
                5 -> SkillsTab(novel, viewModel)
                6 -> TimelineTab(novel, viewModel)
            }
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
                minLines = 3,
                maxLines = 6
            )
        }
        
        item {
            OutlinedTextField(
                value = powerSystem,
                onValueChange = { powerSystem = it },
                label = { Text(stringResource(R.string.power_system)) },
                placeholder = { Text(stringResource(R.string.power_system_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
        }
        
        item {
            OutlinedTextField(
                value = rules,
                onValueChange = { rules = it },
                label = { Text(stringResource(R.string.world_rules)) },
                placeholder = { Text(stringResource(R.string.world_rules_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }
        
        item {
            Button(
                onClick = { 
                    viewModel.updateWorldBuilding(
                        worldBackground = worldBackground,
                        powerSystem = powerSystem,
                        rules = rules
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save))
            }
        }
        
        item {
            OutlinedButton(
                onClick = { viewModel.generateWorldBuilding() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ai_generate_world))
            }
        }
    }
}

@Composable
fun CharactersTab(novel: Novel, viewModel: XSGrokViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<Character?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(novel.characters) { character ->
                CharacterDetailCard(
                    character = character,
                    onEdit = { editingCharacter = character },
                    onDelete = { viewModel.deleteCharacter(character.id) }
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.generateCharacters() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ai_generate))
            }
            
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add))
            }
        }
    }
    
    if (showAddDialog) {
        CharacterEditDialog(
            character = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, desc, role, appearance, personality, background, abilities ->
                viewModel.addCharacterFull(name, desc, role, appearance, personality, background, abilities)
                showAddDialog = false
            }
        )
    }
    
    editingCharacter?.let { char ->
        CharacterEditDialog(
            character = char,
            onDismiss = { editingCharacter = null },
            onSave = { name, desc, role, appearance, personality, background, abilities ->
                viewModel.updateCharacter(char.id, name, desc, role, appearance, personality, background, abilities)
                editingCharacter = null
            }
        )
    }
}

@Composable
fun CharacterDetailCard(
    character: Character,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                    AssistChip(
                        onClick = {},
                        label = { Text(character.role) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (character.description.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.description) + ": " + character.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (character.appearance.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.appearance) + ": " + character.appearance,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (character.personality.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.personality) + ": " + character.personality,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (character.background.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.background) + ": " + character.background,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (character.abilities.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.abilities) + ": " + character.abilities,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditDialog(
    character: Character?,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, role: String, appearance: String, personality: String, background: String, abilities: String) -> Unit
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var description by remember { mutableStateOf(character?.description ?: "") }
    var role by remember { mutableStateOf(character?.role ?: "主角") }
    var appearance by remember { mutableStateOf(character?.appearance ?: "") }
    var personality by remember { mutableStateOf(character?.personality ?: "") }
    var background by remember { mutableStateOf(character?.background ?: "") }
    var abilities by remember { mutableStateOf(character?.abilities ?: "") }
    
    val roles = listOf("主角", "反派", "配角", "龙套", "路人")
    var roleExpanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (character == null) stringResource(R.string.add_character) else stringResource(R.string.edit_character)) },
        text = {
            Column(
                modifier = Modifier.height(450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                                onClick = { role = r; roleExpanded = false }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = appearance,
                    onValueChange = { appearance = it },
                    label = { Text(stringResource(R.string.appearance)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = personality,
                    onValueChange = { personality = it },
                    label = { Text(stringResource(R.string.personality)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = background,
                    onValueChange = { background = it },
                    label = { Text(stringResource(R.string.background)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = abilities,
                    onValueChange = { abilities = it },
                    label = { Text(stringResource(R.string.abilities)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank()) {
                        onSave(name, description, role, appearance, personality, background, abilities)
                    }
                }
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

@Composable
fun LocationsTab(novel: Novel, viewModel: XSGrokViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(novel.worldBuilding.geography) { location ->
                LocationCard(
                    location = location,
                    onDelete = { viewModel.deleteLocation(location.id) }
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.generateLocations() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ai_generate))
            }
            
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add))
            }
        }
    }
    
    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, desc, type, significance ->
                viewModel.addLocation(name, desc, type, significance)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LocationCard(location: Location, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = location.name, style = MaterialTheme.typography.titleSmall)
                if (location.type.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(location.type, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (location.description.isNotBlank()) {
                    Text(
                        text = location.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, desc: String, type: String, significance: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var significance by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_location)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.location_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text(stringResource(R.string.location_type)) },
                    placeholder = { Text(stringResource(R.string.location_type_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = significance,
                    onValueChange = { significance = it },
                    label = { Text(stringResource(R.string.significance)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank()) {
                        onAdd(name, description, type, significance)
                    }
                }
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

// 简化的其他Tab
@Composable
fun FactionsTab(novel: Novel, viewModel: XSGrokViewModel) {
    GenericListTab(
        items = novel.worldBuilding.factions,
        title = stringResource(R.string.factions),
        onAiGenerate = { viewModel.generateFactions() },
        onAdd = { name, desc -> viewModel.addFaction(name, desc) },
        onDelete = { id -> viewModel.deleteFaction(id) }
    )
}

@Composable
fun ItemsTab(novel: Novel, viewModel: XSGrokViewModel) {
    GenericListTab(
        items = novel.worldBuilding.items,
        title = stringResource(R.string.items),
        onAiGenerate = { viewModel.generateItems() },
        onAdd = { name, desc -> viewModel.addItem(name, desc) },
        onDelete = { id -> viewModel.deleteItem(id) }
    )
}

@Composable
fun SkillsTab(novel: Novel, viewModel: XSGrokViewModel) {
    GenericListTab(
        items = novel.worldBuilding.skills,
        title = stringResource(R.string.skills),
        onAiGenerate = { viewModel.generateSkills() },
        onAdd = { name, desc -> viewModel.addSkill(name, desc) },
        onDelete = { id -> viewModel.deleteSkill(id) }
    )
}

@Composable
fun TimelineTab(novel: Novel, viewModel: XSGrokViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(novel.worldBuilding.timeline.sortedBy { it.chapter }) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = event.title, style = MaterialTheme.typography.titleSmall)
                            if (event.time.isNotBlank()) {
                                Text(
                                    text = event.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { viewModel.deleteTimelineEvent(event.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.generateTimeline() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ai_generate))
            }
            
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add))
            }
        }
    }
    
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_timeline_event)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.event_title)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text(stringResource(R.string.event_time)) },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                TextButton(onClick = { 
                    if (title.isNotBlank()) {
                        viewModel.addTimelineEvent(title, time, description)
                        showAddDialog = false
                    }
                }) {
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

@Composable
fun <T> GenericListTab(
    items: List<T>,
    title: String,
    onAiGenerate: () -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit
) where T : Any {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                val (id, name, desc) = when (item) {
                    is Faction -> Triple(item.id, item.name, item.description)
                    is GameItem -> Triple(item.id, item.name, item.description)
                    is Skill -> Triple(item.id, item.name, item.description)
                    else -> Triple("", "", "")
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, style = MaterialTheme.typography.titleSmall)
                            if (desc.isNotBlank()) {
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onAiGenerate,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ai_generate))
            }
            
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add))
            }
        }
    }
    
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_item, title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                TextButton(onClick = { 
                    if (name.isNotBlank()) {
                        onAdd(name, description)
                        showAddDialog = false
                    }
                }) {
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
