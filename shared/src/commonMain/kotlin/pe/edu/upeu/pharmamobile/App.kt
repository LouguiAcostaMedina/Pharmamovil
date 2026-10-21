package pe.edu.upeu.pharmamobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import pe.edu.upeu.pharmamobile.di.initKoin
import pe.edu.upeu.pharmamobile.presentation.auth.AuthViewModel
import pe.edu.upeu.pharmamobile.presentation.auth.LoginScreen
import pe.edu.upeu.pharmamobile.presentation.diagnostico.DiagnosticoS10Screen
import pe.edu.upeu.pharmamobile.presentation.navigation.*
import pe.edu.upeu.pharmamobile.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobile.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobile.presentation.theme.PharmaMobilTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    remember {
        runCatching { initKoin() }
    }
    KoinContext {
        val authViewModel: AuthViewModel = koinInject()
        val productoViewModel: ProductoViewModel = koinInject()
        val sesion by authViewModel.sesionState.collectAsStateWithLifecycle()
        val mensajePermiso by authViewModel.mensajePermiso.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val ejecutarCierreSesion = {
            authViewModel.cerrarSesion()
            productoViewModel.limpiarEstado()
        }

        var isDarkMode by remember { mutableStateOf(false) }

        LaunchedEffect(mensajePermiso) {
            val msg = mensajePermiso
            if (!msg.isNullOrBlank()) {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Long
                )
                authViewModel.limpiarMensajePermiso()
            }
        }

        LaunchedEffect(sesion) {
            val sesionActual = sesion
            if (sesionActual != null) {
                productoViewModel.limpiarEstado()
                if (sesionActual.puede("CATALOGO_LEER")) {
                    productoViewModel.cargarProductos()
                }
            } else {
                productoViewModel.limpiarEstado()
            }
        }

        PharmaMobilTheme(darkTheme = isDarkMode) {
            val sesionActiva = sesion
            if (sesionActiva == null) {
                // Guardia de sesión: si no hay credencial activa, renderizar LoginScreen
                LoginScreen(viewModel = authViewModel)
            } else {
                // Guardia de sesión aprobada: renderizar UI protegida con menús por permisos
                val pantallas = remember(sesionActiva) {
                    buildList {
                        add(Screen.Inicio)
                        if (sesionActiva.puede("CATALOGO_LEER")) {
                            add(Screen.Productos)
                        }
                        if (sesionActiva.puede("CLIENTES_LEER")) {
                            add(Screen.Clientes)
                        }
                        if (sesionActiva.puede("VENTAS_LEER")) {
                            add(Screen.Pedidos)
                        }
                        if (sesionActiva.puede("REPORTES_VER")) {
                            add(Screen.Reportes)
                        }
                        add(Screen.Diagnostico)
                        add(Screen.AcercaDe)
                    }
                }

                var pantallaActual by remember(pantallas) {
                    mutableStateOf(if (pantallas.contains(Screen.Productos)) Screen.Productos else Screen.Inicio)
                }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isCompact = maxWidth < 600.dp

                    if (isCompact) {
                        // Layout móvil compacto
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet {
                                    Spacer(Modifier.height(16.dp))
                                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "PharmaMobil 💊",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Usuario: ${sesionActiva.usuario}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = sesionActiva.nombreCompleto,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    pantallas.forEach { pantalla ->
                                        NavigationDrawerItem(
                                            label = { Text(pantalla.title) },
                                            icon = {
                                                Icon(
                                                    imageVector = pantalla.icon,
                                                    contentDescription = pantalla.title
                                                )
                                            },
                                            selected = pantallaActual == pantalla,
                                            onClick = {
                                                pantallaActual = pantalla
                                                scope.launch { drawerState.close() }
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))
                                    HorizontalDivider()

                                    // Botón Cerrar Sesión en Drawer
                                    NavigationDrawerItem(
                                        label = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.ExitToApp,
                                                contentDescription = "Cerrar Sesión",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        selected = false,
                                        onClick = {
                                            scope.launch {
                                                drawerState.close()
                                                ejecutarCierreSesion()
                                            }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { isDarkMode = false },
                                                    modifier = Modifier.size(40.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = if (!isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (!isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.LightMode, contentDescription = "Claro", modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(
                                                    onClick = { isDarkMode = true },
                                                    modifier = Modifier.size(40.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = if (isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.DarkMode, contentDescription = "Oscuro", modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Scaffold(
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Column {
                                                Text(pantallaActual.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                Text(
                                                    "${sesionActiva.usuario} · ${sesionActiva.nombreCompleto}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { ejecutarCierreSesion() }) {
                                                Icon(
                                                    imageVector = Icons.Default.ExitToApp,
                                                    contentDescription = "Cerrar sesión"
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            ) { paddingValues ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                 ) {
                                    RenderizarPantalla(
                                        pantalla = pantallaActual,
                                        sesion = sesionActiva,
                                        authViewModel = authViewModel,
                                        productoViewModel = productoViewModel,
                                        onSesionExpirada = { authViewModel.notificarSesionExpirada() },
                                        onProbar403 = { authViewModel.probar403Controlado() }
                                    )
                                }
                            }
                        }
                    } else {
                        // Layout de escritorio y tablet
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                header = {
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = "💊",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        text = sesionActiva.usuario,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(12.dp))
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Spacer(Modifier.weight(0.1f))
                                pantallas.forEach { pantalla ->
                                    NavigationRailItem(
                                        selected = pantallaActual == pantalla,
                                        onClick = { pantallaActual = pantalla },
                                        icon = {
                                            Icon(
                                                imageVector = pantalla.icon,
                                                contentDescription = pantalla.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = pantalla.title,
                                                fontWeight = if (pantallaActual == pantalla) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                                Spacer(Modifier.weight(1f))

                                // Botón Cerrar Sesión en Rail
                                IconButton(
                                    onClick = { ejecutarCierreSesion() },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Cerrar sesión",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }

                                IconButton(
                                    onClick = { isDarkMode = !isDarkMode },
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = "Alternar Tema",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            VerticalDivider()

                            Scaffold(
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = pantallaActual.title,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${sesionActiva.nombreCompleto} (${sesionActiva.usuario})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    )
                                                }
                                                Button(
                                                    onClick = { ejecutarCierreSesion() },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Cerrar Sesión", fontSize = 12.sp)
                                                }
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            ) { paddingValues ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                ) {
                                    RenderizarPantalla(
                                        pantalla = pantallaActual,
                                        sesion = sesionActiva,
                                        authViewModel = authViewModel,
                                        productoViewModel = productoViewModel,
                                        onSesionExpirada = { authViewModel.notificarSesionExpirada() },
                                        onProbar403 = { authViewModel.probar403Controlado() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderizarPantalla(
    pantalla: Screen,
    sesion: pe.edu.upeu.pharmamobile.domain.model.Sesion,
    authViewModel: AuthViewModel,
    productoViewModel: ProductoViewModel,
    onSesionExpirada: () -> Unit,
    onProbar403: () -> Unit
) {
    when (pantalla) {
        Screen.Inicio -> InicioScreen()
        Screen.Productos -> ProductoScreen(
            puedeEscribir = sesion.puede("CATALOGO_ESCRIBIR"),
            onSesionExpirada = onSesionExpirada,
            onProbar403 = onProbar403,
            viewModel = productoViewModel
        )
        Screen.Clientes -> ClientesScreen()
        Screen.Pedidos -> PedidosScreen()
        Screen.Reportes -> ReportesScreen(sesion = sesion)
        Screen.Diagnostico -> DiagnosticoS10Screen(sesion = sesion, viewModel = authViewModel)
        Screen.AcercaDe -> AcercaDeScreen()
    }
}