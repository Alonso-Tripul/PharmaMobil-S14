@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package pe.edu.upeu.pharmamobil.platform

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

class AlmacenSeguroIos : AlmacenSeguro {
    // Convierte referencias CF sin transferir la propiedad de las constantes de Security.
    private fun puente(value: CFTypeRef?): Any = CFBridgingRelease(CFRetain(value))!!
    private fun clave(value: CFStringRef?): NSCopyingProtocol = puente(value) as NSString
    private fun consulta(): NSMutableDictionary = NSMutableDictionary().apply {
        setObject(puente(kSecClassGenericPassword), forKey = clave(kSecClass))
        setObject("pe.edu.upeu.pharmamobil.sesion", forKey = clave(kSecAttrService))
        setObject("jwt", forKey = clave(kSecAttrAccount))
    }
    private fun ref(dic: NSDictionary): CFDictionaryRef = CFBridgingRetain(dic)!!.reinterpret()
    override fun guardarToken(token: String) {
        val datos = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
        val query = ref(consulta())
        val valores = ref(NSMutableDictionary().apply { setObject(datos, forKey = clave(kSecValueData)) })
        try {
            val status = SecItemUpdate(query, valores)
            if (status == errSecItemNotFound) {
                val nuevo = ref(consulta().apply {
                    setObject(datos, forKey = clave(kSecValueData))
                    setObject(puente(kSecAttrAccessibleWhenUnlockedThisDeviceOnly), forKey = clave(kSecAttrAccessible))
                })
                try { check(SecItemAdd(nuevo, null) == errSecSuccess) { "No se pudo guardar la sesión." } }
                finally { CFRelease(nuevo) }
            } else check(status == errSecSuccess) { "No se pudo actualizar la sesión." }
        } finally { CFRelease(query); CFRelease(valores) }
    }
    override fun leerToken(): String? = memScoped {
        val query = ref(consulta().apply {
            setObject(puente(kCFBooleanTrue), forKey = clave(kSecReturnData))
            setObject(puente(kSecMatchLimitOne), forKey = clave(kSecMatchLimit))
        })
        val salida = alloc<CFTypeRefVar>(); salida.value = null
        try {
            val status = SecItemCopyMatching(query, salida.ptr)
            if (status == errSecItemNotFound) return@memScoped null
            check(status == errSecSuccess) { "No se pudo recuperar la sesión." }
            val datos = CFBridgingRelease(salida.value) as NSData
            NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
        } finally { CFRelease(query) }
    }
    override fun borrarToken() {
        val query = ref(consulta())
        try { val status = SecItemDelete(query); check(status == errSecSuccess || status == errSecItemNotFound) }
        finally { CFRelease(query) }
    }
}
