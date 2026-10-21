package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pe.edu.upeu.pharmamobile.data.model.PaginaResponseDto
import pe.edu.upeu.pharmamobile.data.model.ProductoRequestDto
import pe.edu.upeu.pharmamobile.data.model.ProductoResponseDto

class ProductoApi(private val client: HttpClient) {

    suspend fun listar(pagina: Int = 0, tamanio: Int = 20): PaginaResponseDto<ProductoResponseDto> {
        return client.get("productos") {
            url {
                parameters.append("pagina", pagina.toString())
                parameters.append("tamanio", tamanio.toString())
            }
        }.body()
    }

    suspend fun obtener(id: Long): ProductoResponseDto {
        return client.get("productos/$id").body()
    }

    suspend fun crear(request: ProductoRequestDto): ProductoResponseDto {
        return client.post("productos") {
            setBody(request)
        }.body()
    }

    suspend fun actualizar(id: Long, request: ProductoRequestDto): ProductoResponseDto {
        return client.put("productos/$id") {
            setBody(request)
        }.body()
    }

    suspend fun eliminar(id: Long) {
        client.delete("productos/$id")
    }
}
