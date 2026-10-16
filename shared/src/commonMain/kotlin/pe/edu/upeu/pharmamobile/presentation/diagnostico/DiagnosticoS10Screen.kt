package pe.edu.upeu.pharmamobile.presentation.diagnostico

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobile.data.network.ResultadoDiagnostico
import pe.edu.upeu.pharmamobile.domain.model.Sesion
import pe.edu.upeu.pharmamobile.presentation.auth.AuthViewModel

@Composable
fun DiagnosticoS10Screen(
    sesion: Sesion?,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val resultado by viewModel.resultadoDiagnostico.collectAsStateWithLifecycle()
    val cargando by viewModel.diagnosticoCargando.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Panel de Diagnóstico S10 — Validación de Token y Permisos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Usuario actual: ${sesion?.usuario ?: "Sin sesión"} (${sesion?.nombreCompleto ?: "ANÓNIMO"})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Permisos activos: ${sesion?.permisos?.joinToString(", ") ?: "Ninguno"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Indicador de carga
            if (cargando) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Tarjeta de Resultado del Diagnóstico
            resultado?.let { res ->
                item {
                    TarjetaResultado(res = res, onCerrar = { viewModel.limpiarResultadoDiagnostico() })
                }
            }

            // Sección 1: Los Seis Escenarios de Token
            item {
                Text(
                    text = "1. Seis Escenarios de Token (S10)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.ejecutarEscenarioDiagnostico(1) },
                                enabled = !cargando,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("E01: Token Válido", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.ejecutarEscenarioDiagnostico(2) },
                                enabled = !cargando,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("E02: Token Expirado", fontSize = 12.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.ejecutarEscenarioDiagnostico(3) },
                                enabled = !cargando,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("E03: Token Alterado", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.ejecutarEscenarioDiagnostico(4) },
                                enabled = !cargando,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("E04: Sin Cabecera", fontSize = 12.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.ejecutarEscenarioDiagnostico(5) },
                                enabled = !cargando,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("E05: Prefijo 'bearer'", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.ejecutarEscenarioDiagnostico(6) },
                                enabled = !cargando,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("E06: Deshabilitado", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Sección 2: Matriz de 18 Resultados
            item {
                Text(
                    text = "2. Matriz de Permisos por Rol (6 Operaciones)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.ejecutarOperacionMatriz(1) },
                                enabled = !cargando,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("1. Listar Productos", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.ejecutarOperacionMatriz(2) },
                                enabled = !cargando,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("2. Registrar Producto", fontSize = 11.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.ejecutarOperacionMatriz(3) },
                                enabled = !cargando,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("3. Listar Clientes", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.ejecutarOperacionMatriz(4) },
                                enabled = !cargando,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("4. Registrar Venta", fontSize = 11.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.ejecutarOperacionMatriz(5) },
                                enabled = !cargando,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("5. Ver Reportes", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.ejecutarOperacionMatriz(6) },
                                enabled = !cargando,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("6. Consultar /auth/me", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TarjetaResultado(
    res: ResultadoDiagnostico,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorContenedor = when (res.codigoHttp) {
        200, 201 -> MaterialTheme.colorScheme.primaryContainer
        401 -> MaterialTheme.colorScheme.errorContainer
        403 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val colorTexto = when (res.codigoHttp) {
        200, 201 -> MaterialTheme.colorScheme.onPrimaryContainer
        401 -> MaterialTheme.colorScheme.onErrorContainer
        403 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorContenedor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = res.escenario,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (res.codigoHttp) {
                        200, 201 -> Color(0xFF2E7D32)
                        401 -> Color(0xFFC62828)
                        403 -> Color(0xFFE65100)
                        else -> Color(0xFF616161)
                    }
                ) {
                    Text(
                        text = "HTTP ${res.codigoHttp}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = colorTexto.copy(alpha = 0.3f))

            Text(
                text = "Usuario: ${res.usuario}  |  Petición: ${res.metodo} ${res.ruta}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorTexto
            )

            Text(
                text = "Mensaje Servidor: \"${res.mensaje}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = colorTexto
            )

            Text(
                text = "Estado de Sesión Posterior: ${res.estadoSesion}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colorTexto
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCerrar) {
                    Text("Ocultar", color = colorTexto)
                }
            }
        }
    }
}
