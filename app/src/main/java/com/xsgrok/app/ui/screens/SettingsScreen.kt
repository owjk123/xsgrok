package com.xsgrok.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.xsgrok.app.R
import com.xsgrok.app.data.remote.ApiEndpoints
import com.xsgrok.app.ui.XSGrokViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: XSGrokViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val apiConfig = uiState.apiConfig
    
    var apiKey by remember(apiConfig) { mutableStateOf(apiConfig.apiKey) }
    var endpoint by remember(apiConfig) { mutableStateOf(apiConfig.endpoint) }
    var selectedModel by remember(apiConfig) { mutableStateOf(apiConfig.model) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var endpointExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.api_configuration),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        viewModel.updateApiKey(it)
                    },
                    label = { Text(stringResource(R.string.api_key)) },
                    placeholder = { Text(stringResource(R.string.enter_api_key)) },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.toggle_visibility)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = endpointExpanded,
                    onExpandedChange = { endpointExpanded = !endpointExpanded }
                ) {
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = {
                            endpoint = it
                            viewModel.updateEndpoint(it)
                        },
                        label = { Text(stringResource(R.string.api_endpoint)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endpointExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = endpointExpanded,
                        onDismissRequest = { endpointExpanded = false }
                    ) {
                        listOf(ApiEndpoints.PRIMARY, ApiEndpoints.BACKUP1, ApiEndpoints.BACKUP2).forEach { ep ->
                            DropdownMenuItem(
                                text = { Text(ep) },
                                onClick = {
                                    endpoint = ep
                                    viewModel.updateEndpoint(ep)
                                    endpointExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.model)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        ApiEndpoints.MODELS.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    selectedModel = model
                                    viewModel.updateModel(model)
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (apiConfig.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.dark_mode))
                    }
                    Switch(
                        checked = apiConfig.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "XSGrok v1.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
