package pe.edu.upeu.pharmamobile.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class AlmacenSeguroIos : AlmacenSeguro {

    private val servicio = "pe.edu.upeu.pharmamobile"
    private val cuenta = "jwt"

    private fun consultaBase(): NSMutableDictionary {
        val dict = NSMutableDictionary()
        dict.setObject(kSecClassGenericPassword, kSecClass)
        dict.setObject(servicio, kSecAttrService)
        dict.setObject(cuenta, kSecAttrAccount)
        return dict
    }

    override fun guardarToken(token: String) {
        borrarToken()
        val datos: NSData? = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        if (datos != null) {
            val consulta = consultaBase()
            consulta.setObject(datos, kSecValueData)
            SecItemAdd(consulta, null)
        }
    }

    override fun leerToken(): String? {
        val consulta = consultaBase()
        consulta.setObject(kSecMatchLimitOne, kSecMatchLimit)
        consulta.setObject(kCFBooleanTrue, kSecReturnData)

        memScoped {
            val resultado = alloc<CFTypeRefVar>()
            val estado = SecItemCopyMatching(consulta, resultado.ptr)
            if (estado == errSecSuccess) {
                val datos = resultado.value as? NSData ?: return null
                return NSString.create(data = datos, encoding = NSUTF8StringEncoding) as String?
            }
        }
        return null
    }

    override fun borrarToken() {
        val consulta = consultaBase()
        SecItemDelete(consulta)
    }
}
