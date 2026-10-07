package pe.edu.upeu.pharmamobile.presentation.producto

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.domain.usecase.RegistrarProductoUseCase

import pe.edu.upeu.pharmamobile.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.ObtenerProductoUseCase
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Paso 1: Fakes del Repositorio

    open class FakeRepoBase : ProductoRepository {
        var llamadasRegistrar = 0
        override suspend fun registrar(p: Producto): Producto {
            llamadasRegistrar++
            return p
        }
        override suspend fun listar(): List<Producto> = emptyList()
        override suspend fun obtener(id: Long): Producto = Producto(1, "", 0.0)
        override suspend fun actualizar(p: Producto): Producto = p
        override suspend fun eliminar(id: Long) {}
    }

    class FakeRepoVacio : FakeRepoBase()

    class FakeRepoLleno : FakeRepoBase() {
        override suspend fun listar(): List<Producto> {
            return listOf(
                Producto(1, "Paracetamol", 5.0, 100),
                Producto(2, "Ibuprofeno", 8.0, 50),
                Producto(3, "Amoxicilina", 15.0, 20)
            )
        }
    }

    class FakeRepoError : FakeRepoBase() {
        override suspend fun listar(): List<Producto> {
            throw Exception("Error simulado")
        }
    }

    class FakeRepoValidacion : FakeRepoBase() {
        override suspend fun registrar(p: Producto): Producto {
            throw pe.edu.upeu.pharmamobile.domain.model.ErrorApi.Validacion(
                "Errores de validación",
                mapOf("nombre" to "El nombre debe tener al menos 3 caracteres", "precio" to "El precio debe ser mayor que cero")
            )
        }
    }

    class FakeRepoEliminar : FakeRepoBase() {
        var eliminarInvocado = false
        override suspend fun eliminar(id: Long) {
            eliminarInvocado = true
        }
    }

    private fun createViewModel(repo: ProductoRepository): ProductoViewModel {
        return ProductoViewModel(
            listarProductosUseCase = ListarProductosUseCase(repo),
            registrarProductoUseCase = RegistrarProductoUseCase(repo),
            actualizarProductoUseCase = ActualizarProductoUseCase(repo),
            eliminarProductoUseCase = EliminarProductoUseCase(repo),
            obtenerProductoUseCase = ObtenerProductoUseCase(repo)
        )
    }

    // Paso 2: Pruebas Unitarias

    @Test
    fun debe_mostrar_SinProductos_cuando_repositorio_esta_vacio() = runTest {
        val repo = FakeRepoVacio()
        val viewModel = createViewModel(repo)

        viewModel.cargarProductos()
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.SinProductos, "La fase debería ser SinProductos")
    }

    @Test
    fun debe_mostrar_ConProductos_cuando_repositorio_tiene_datos() = runTest {
        val repo = FakeRepoLleno()
        val viewModel = createViewModel(repo)

        viewModel.cargarProductos()
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.ConProductos, "La fase debería ser ConProductos")
        assertEquals(3, (fase as ProductoUiState.Fase.ConProductos).lista.size)
    }

    @Test
    fun debe_mostrar_Error_cuando_repositorio_falla() = runTest {
        val repo = FakeRepoError()
        val viewModel = createViewModel(repo)

        viewModel.cargarProductos()
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.Error, "La fase debería ser Error")
    }

    @Test
    fun debe_mostrar_errores_de_validacion_al_registrar_invalido() = runTest {
        val repo = FakeRepoValidacion()
        val viewModel = createViewModel(repo)

        viewModel.onNombreChange("Te")
        viewModel.onPrecioChange("-5.0")
        
        viewModel.registrarProducto()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        val operacion = uiState.operacion
        
        assertTrue(operacion is ProductoUiState.Operacion.Fallida, "La operación debería ser Fallida")
        assertEquals("El nombre debe tener al menos 3 caracteres", uiState.nombreError)
        assertEquals("El precio debe ser mayor que cero", uiState.precioError)
    }

    @Test
    fun debe_cambiar_estados_correctamente_al_eliminar() = runTest {
        val repo = FakeRepoEliminar()
        val viewModel = createViewModel(repo)

        viewModel.eliminarProducto(1L)
        
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        val operacion = uiState.operacion
        
        assertTrue(operacion is ProductoUiState.Operacion.Inactiva, "La operación debería ser Inactiva después de terminar")
        assertTrue(repo.eliminarInvocado, "El repositorio de eliminar debería haber sido invocado")
    }

    class FakeRepoNoEncontrado : FakeRepoBase() {
        override suspend fun obtener(id: Long): Producto {
            throw ErrorApi.NoEncontrado()
        }
    }

    @Test
    fun debe_mostrar_NoEncontrado_al_buscar_id_inexistente() = runTest {
        val repo = FakeRepoNoEncontrado()
        val viewModel = createViewModel(repo)

        viewModel.buscarPorId("999999")
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.Error, "La fase debería ser Error")
        assertEquals("Recurso no encontrado", (fase as ProductoUiState.Fase.Error).mensaje)
    }

    class FakeRepoCancelacion : FakeRepoBase() {
        override suspend fun listar(): List<Producto> {
            throw CancellationException("Operación cancelada")
        }
    }

    @Test
    fun debe_propagar_CancellationException_sin_marcar_fase_de_error() = runTest {
        val repo = FakeRepoCancelacion()
        val viewModel = createViewModel(repo)

        viewModel.cargarProductos()
        advanceUntilIdle()
        
        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.Cargando, "La fase debería seguir siendo Cargando, no un Error")
    }
}
