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
                                                TextButton(
                                                    onClick = { isDarkMode = false },
                                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                    colors = ButtonDefaults.textButtonColors(
                                                        containerColor = if (!isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (!isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.LightMode, contentDescription = "Claro", modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Claro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(
                                                    onClick = { isDarkMode = true },
                                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                    colors = ButtonDefaults.textButtonColors(
                                                        containerColor = if (isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.DarkMode, contentDescription = "Oscuro", modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Oscuro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
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
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.padding(end = 16.dp).height(36.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { isDarkMode = false },
                                                    modifier = Modifier.size(36.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = if (!isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (!isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.LightMode, contentDescription = "Claro", modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(
                                                    onClick = { isDarkMode = true },
                                                    modifier = Modifier.size(36.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = if (isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.DarkMode, contentDescription = "Oscuro", modifier = Modifier.size(20.dp))
                                                }
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
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.padding(end = 16.dp).height(36.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { isDarkMode = false },
                                                    modifier = Modifier.size(36.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = if (!isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (!isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.LightMode, contentDescription = "Claro", modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(
                                                    onClick = { isDarkMode = true },
                                                    modifier = Modifier.size(36.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = if (isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        contentColor = if (isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(Icons.Default.DarkMode, contentDescription = "Oscuro", modifier = Modifier.size(20.dp))
                                                }
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