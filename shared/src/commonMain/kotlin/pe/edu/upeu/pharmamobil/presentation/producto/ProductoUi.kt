package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.platform.formatearSoles
import pe.edu.upeu.pharmamobil.domain.model.Producto


data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precio: String,
    val stock: String,
    val activo: Boolean,
    val requiereReposicion: Boolean
)

fun Producto.aUi(): ProductoUi = ProductoUi(
    id = id,
    nombre = nombre,
    precio = formatearSoles(precio),
    stock = "$stock u.",
    activo = estado,
    requiereReposicion = requiereReposicion
)
