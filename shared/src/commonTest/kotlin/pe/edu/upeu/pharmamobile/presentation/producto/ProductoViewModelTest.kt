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

    private fun createViewModel(repo: ProductoRepository): ProductoViewModel {
        return ProductoViewModel(
            listarProductosUseCase = ListarProductosUseCase(repo),
            registrarProductoUseCase = RegistrarProductoUseCase(repo),
            actualizarProductoUseCase = ActualizarProductoUseCase(repo),
            eliminarProductoUseCase = EliminarProductoUseCase(repo)
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
}
