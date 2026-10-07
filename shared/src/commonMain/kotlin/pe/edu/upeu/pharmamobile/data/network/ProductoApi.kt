package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pe.edu.upeu.pharmamobile.data.model.PaginaResponseDto
import pe.edu.upeu.pharmamobile.data.model.ProductoRequestDto
import pe.edu.upeu.pharmamobile.data.model.ProductoResponseDto
import pe.edu.upeu.pharmamobile.getPlatform

class ProductoApi(private val client: HttpClient) {

    private val baseUrl = if (getPlatform().name.contains("Android")) {
        "http://10.0.2.2:8080/api/v1"
    } else {
        "http://localhost:8080/api/v1"
    }

    suspend fun listar(pagina: Int = 0, tamanio: Int = 20): PaginaResponseDto<ProductoResponseDto> {
        return client.get("$baseUrl/productos") {
            url {
                parameters.append("pagina", pagina.toString())
                parameters.append("tamanio", tamanio.toString())
            }
        }.body()
    }

    suspend fun obtener(id: Long): ProductoResponseDto {
        return client.get("$baseUrl/productos/$id").body()
    }

    suspend fun crear(request: ProductoRequestDto): ProductoResponseDto {
        return client.post("$baseUrl/productos") {
            setBody(request)
        }.body()
    }

    suspend fun actualizar(id: Long, request: ProductoRequestDto): ProductoResponseDto {
        return client.put("$baseUrl/productos/$id") {
            setBody(request)
        }.body()
    }

    suspend fun eliminar(id: Long) {
        client.delete("$baseUrl/productos/$id")
    }
}
