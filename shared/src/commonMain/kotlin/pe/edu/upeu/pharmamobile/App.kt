package pe.edu.upeu.pharmamobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import pe.edu.upeu.pharmamobile.di.appModule
import pe.edu.upeu.pharmamobile.presentation.navigation.*
import pe.edu.upeu.pharmamobile.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobile.presentation.theme.PharmaMobilTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        var isDarkMode by remember { mutableStateOf(false) }

        PharmaMobilTheme(darkTheme = isDarkMode) {
            var pantallaActual by remember { mutableStateOf<Screen>(Screen.Inicio) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            val pantallas = listOf(
                Screen.Inicio,
                Screen.Productos,
                Screen.Clientes,
                Screen.Pedidos
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 600.dp

                if (isCompact) {
                    // Responsive Mobile Layout: ModalNavigationDrawer with top app bar
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "PharmaMobil 💊",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isDarkMode) "Modo Oscuro" else "Modo Claro",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = { isDarkMode = it }
                                    )
                                }
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = pantallaActual.title,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Abrir Menú"
                                            )
                                        }
                                    },
                                    actions = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                contentDescription = "Alternar Tema",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Switch(
                                                checked = isDarkMode,
                                                onCheckedChange = { isDarkMode = it }
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
                                when (pantallaActual) {
                                    Screen.Inicio -> InicioScreen()
                                    Screen.Productos -> ProductoScreen()
                                    Screen.Clientes -> ClientesScreen()
                                    Screen.Pedidos -> PedidosScreen()
                                }
                            }
                        }
                    }
                } else {
                    // Responsive Tablet & Desktop Layout: NavigationRail side bar
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            header = {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "💊",
                                    style = MaterialTheme.typography.headlineMedium
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
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = pantallaActual.title,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    actions = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 16.dp)
                                        ) {
                                            Text(
                                                text = if (isDarkMode) "Modo Oscuro" else "Modo Claro",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Switch(
                                                checked = isDarkMode,
                                                onCheckedChange = { isDarkMode = it }
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
                                when (pantallaActual) {
                                    Screen.Inicio -> InicioScreen()
                                    Screen.Productos -> ProductoScreen()
                                    Screen.Clientes -> ClientesScreen()
                                    Screen.Pedidos -> PedidosScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}