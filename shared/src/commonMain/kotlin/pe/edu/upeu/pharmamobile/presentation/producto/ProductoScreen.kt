package pe.edu.upeu.pharmamobile.presentation.producto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.model.Sesion
import pe.edu.upeu.pharmamobile.platform.formatearSoles

import androidx.compose.material.icons.filled.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoScreen(
    sesion: Sesion? = null,
    puedeEscribir: Boolean = true,
    onSesionExpirada: () -> Unit = {},
    onProbar403: (() -> Unit)? = null,
    viewModel: ProductoViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.iniciarObservacion()
        viewModel.sincronizar()
        viewModel.iniciarTickerAntiguedad()
    }

    LaunchedEffect(state.fase) {
        val fase = state.fase
        if (fase is ProductoUiState.Fase.Error && (fase.mensaje.contains("401") || fase.mensaje.contains("autorizado", ignoreCase = true) || fase.mensaje.contains("iniciar sesión", ignoreCase = true) || fase.mensaje.contains("expirado", ignoreCase = true))) {
            onSesionExpirada()
        }
    }

    LaunchedEffect(state.mensaje) {
        if (state.mensaje.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = state.mensaje,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 600.dp
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxSize()
                    .padding(if (isCompact) 12.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sesion?.requiereRenovacion == true) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Column {
                                Text(
                                    text = "Sesión Caducada (Modo Offline Restringido)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Los datos locales se conservan para consulta. La sesión requiere renovación en línea al recuperar conexión.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                if (puedeEscribir) {
                    FormularioProducto(state, viewModel)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column {
                                Text(
                                    text = "Modo Consulta de Catálogo (Solo Lectura)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Su usuario no tiene el permiso CATALOGO_ESCRIBIR para registrar o editar productos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (onProbar403 != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Demostración de Permisos S10 (HTTP 403)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Solicita /api/v1/clientes para evidenciar la respuesta 403 Forbidden sin perder la sesión.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onProbar403,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Probar 403", fontSize = 12.sp)
                            }
                        }
                    }
                }
                // Indicador de antigüedad de sincronización en lenguaje natural
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.ultimaSincronizacion,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.sincronizar() },
                        enabled = !state.sincronizando,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sincronizar ahora",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                var searchQuery by remember { mutableStateOf("") }
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar producto por ID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.buscarPorId(searchQuery)
                            } else {
                                viewModel.cargarProductos()
                            }
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Banner de aviso de conectividad y persistencia local
                if (state.avisoRed != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.avisoRed ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Button(
                                onClick = { viewModel.sincronizar() },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Reintentar", fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (state.sincronizando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                when (val fase = state.fase) {
                    is ProductoUiState.Fase.Cargando -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ProductoUiState.Fase.SinProductos -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MensajeFase("No hay productos en la base de datos local.")
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.sincronizar() },
                                enabled = !state.sincronizando
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sincronizar con el servidor")
                            }
                        }
                    }
                    is ProductoUiState.Fase.ConProductos -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inventario de Productos (${fase.lista.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalButton(
                                onClick = { viewModel.sincronizar() },
                                enabled = !state.sincronizando,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "Sincronizar",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Sincronizar", fontSize = 12.sp)
                            }
                        }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(fase.lista, key = { it.id }) { producto ->
                                TarjetaProducto(
                                    producto = producto,
                                    enCurso = state.operacion is ProductoUiState.Operacion.EnCurso,
                                    puedeEscribir = puedeEscribir,
                                    onEdit = { viewModel.onEditarProducto(producto) },
                                    onDelete = { showDeleteDialog = producto.id },
                                    onShare = { viewModel.compartirProducto(producto) }
                                )
                            }
                        }
                    }

                    is ProductoUiState.Fase.Error -> {
                        MensajeFase("Ocurrió un error: ${fase.mensaje}", esError = true)
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("¿Eliminar producto?") },
            text = { Text("Esta acción dará de baja el producto.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarProducto(showDeleteDialog!!)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FormularioProducto(state: ProductoUiState, viewModel: ProductoViewModel) {
    val enCurso = state.operacion is ProductoUiState.Operacion.EnCurso
    val editando = state.productoEditandoId != null
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(editando) {
        if (editando) isExpanded = true
    }

    LaunchedEffect(state.fase, enCurso, editando) {
        if (!editando && !enCurso && state.nombre.isEmpty() && state.precio.isEmpty() && state.stock.isEmpty()) {
            isExpanded = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editando) "Editar producto" else "Nuevo producto",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { 
                    if (editando && isExpanded) viewModel.onCancelarEdicion()
                    isExpanded = !isExpanded 
                }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.Add,
                        contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = state.nombre,
                        onValueChange = { viewModel.onNombreChange(it) },
                        label = { Text("Nombre del Producto") },
                        isError = state.nombreError != null,
                        singleLine = true,
                        enabled = !enCurso,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.nombreError != null) {
                        Text(text = state.nombreError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = state.precio,
                                onValueChange = { viewModel.onPrecioChange(it) },
                                label = { Text("Precio (S/)") },
                                isError = state.precioError != null,
                                singleLine = true,
                                enabled = !enCurso,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.precioError != null) {
                                Text(text = state.precioError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = state.stock,
                                onValueChange = { viewModel.onStockChange(it) },
                                label = { Text("Stock") },
                                isError = state.stockError != null,
                                singleLine = true,
                                enabled = !enCurso,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.stockError != null) {
                                Text(text = state.stockError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editando) {
                            OutlinedButton(
                                onClick = { viewModel.onCancelarEdicion() },
                                enabled = !enCurso,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancelar", fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { viewModel.registrarProducto() },
                            enabled = !enCurso,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (enCurso && (state.operacion as ProductoUiState.Operacion.EnCurso).tipo != ProductoUiState.Operacion.Tipo.Eliminar) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text(
                                    text = if (editando) "Guardar cambios" else "Registrar producto",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MensajeFase(mensaje: String, esError: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = mensaje,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            textAlign = TextAlign.Center,
            color = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TarjetaProducto(
    producto: Producto,
    enCurso: Boolean,
    puedeEscribir: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = producto.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (producto.pendiente) "ID local temporal: ${producto.id}" else "ID: ${producto.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (producto.pendiente) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (producto.pendiente) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFF3E0),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SyncProblem,
                                        contentDescription = "Pendiente de sincronizar",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pendiente de sincronizar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (producto.categoria.isNotBlank()) {
                            Text(
                                text = producto.categoria,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = formatearSoles(producto.precio),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val stockColor = when {
                        producto.stock == 0 -> MaterialTheme.colorScheme.error
                        producto.stock <= Producto.STOCK_MINIMO -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val stockText = when {
                        producto.stock == 0 -> "Sin stock"
                        producto.stock <= Producto.STOCK_MINIMO -> "Stock bajo: ${producto.stock}"
                        else -> "Stock: ${producto.stock}"
                    }
                    
                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = stockColor
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    if (producto.activo) {
                        Text(
                            text = "Disponible",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    enabled = !enCurso,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Compartir",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (puedeEscribir) {
                    IconButton(
                        onClick = onEdit,
                        enabled = !enCurso,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = !enCurso,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
