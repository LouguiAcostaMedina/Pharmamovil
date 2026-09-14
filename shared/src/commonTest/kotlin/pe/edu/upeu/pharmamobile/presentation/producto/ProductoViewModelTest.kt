package pe.edu.upeu.pharmamobile.presentation.producto

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.domain.usecase.RegistrarProductoUseCase

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

    class FakeRepoVacio : ProductoRepository {
        var llamadasRegistrar = 0
        override suspend fun registrar(p: Producto): Producto {
            llamadasRegistrar++
            return p
        }
        override suspend fun listar(): List<Producto> {
            return emptyList()
        }
    }

    class FakeRepoLleno : ProductoRepository {
        var llamadasRegistrar = 0
        override suspend fun registrar(p: Producto): Producto {
            llamadasRegistrar++
            return p
        }
        override suspend fun listar(): List<Producto> {
            return listOf(
                Producto(1, "Paracetamol", 5.0, 100),
                Producto(2, "Ibuprofeno", 8.0, 50),
                Producto(3, "Amoxicilina", 15.0, 20)
            )
        }
    }

    class FakeRepoError : ProductoRepository {
        var llamadasRegistrar = 0
        override suspend fun registrar(p: Producto): Producto {
            llamadasRegistrar++
            return p
        }
        override suspend fun listar(): List<Producto> {
            throw Exception("Error simulado")
        }
    }

    // Paso 2: Pruebas Unitarias

    @Test
    fun debe_mostrar_SinProductos_cuando_repositorio_esta_vacio() = runTest {
        val repo = FakeRepoVacio()
        val useCase = RegistrarProductoUseCase(repo)
        val viewModel = ProductoViewModel(useCase, repo)

        viewModel.cargarProductos()
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.SinProductos, "La fase debería ser SinProductos")
    }

    @Test
    fun debe_mostrar_ConProductos_cuando_repositorio_tiene_datos() = runTest {
        val repo = FakeRepoLleno()
        val useCase = RegistrarProductoUseCase(repo)
        val viewModel = ProductoViewModel(useCase, repo)

        viewModel.cargarProductos()
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.ConProductos, "La fase debería ser ConProductos")
        assertEquals(3, (fase as ProductoUiState.Fase.ConProductos).lista.size)
    }

    @Test
    fun debe_mostrar_Error_cuando_repositorio_falla() = runTest {
        val repo = FakeRepoError()
        val useCase = RegistrarProductoUseCase(repo)
        val viewModel = ProductoViewModel(useCase, repo)

        viewModel.cargarProductos()
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.Error, "La fase debería ser Error")
    }

    @Test
    fun debe_bloquear_registro_con_precio_cero() = runTest {
        val repo = FakeRepoVacio()
        val useCase = RegistrarProductoUseCase(repo)
        val viewModel = ProductoViewModel(useCase, repo)
        
        // Esperamos a que termine la carga inicial del init block
        advanceUntilIdle()

        // Simulamos el llenado del formulario
        viewModel.onNombreChange("Vitamina C")
        viewModel.onPrecioChange("0") // Precio cero
        viewModel.onStockChange("10")
        
        viewModel.registrarProducto()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        
        // Verificamos que el registro haya fallado
        assertEquals(false, state.esExito, "El estado de éxito debería ser false")
        
        // Verificamos que se haya expuesto un mensaje de error (El caso de uso real devuelve "El precio debe ser mayor a 0")
        assertTrue(state.mensaje.isNotEmpty(), "Debería existir un mensaje de error expuesto al formulario")
        
        // Verificamos explícitamente que el repositorio NO fue llamado
        assertEquals(0, repo.llamadasRegistrar, "El contador llamadasRegistrar del repositorio debería ser exactamente 0")
    }
}
