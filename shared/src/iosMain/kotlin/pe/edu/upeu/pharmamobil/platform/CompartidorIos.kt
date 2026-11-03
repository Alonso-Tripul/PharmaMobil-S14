package pe.edu.upeu.pharmamobil.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.CoreGraphics.CGRectMake
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor

/** Presenta desde la ventana activa y configura el popover para iPad. */
@OptIn(ExperimentalForeignApi::class)
class CompartidorIos : Compartidor {
    override fun compartir(texto: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val ventana = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .filter { it.activationState == UISceneActivationStateForegroundActive }
                .flatMap { it.windows.filterIsInstance<platform.UIKit.UIWindow>() }
                .firstOrNull { it.isKeyWindow() }
                ?: UIApplication.sharedApplication.keyWindow
            val raiz = ventana?.rootViewController
                ?: return@dispatch_async
            val presentador = visible(raiz)
            val selector = UIActivityViewController(
                activityItems = listOf(texto),
                applicationActivities = null
            )
            selector.popoverPresentationController?.let { popover ->
                popover.sourceView = presentador.view
                presentador.view.bounds.useContents {
                    popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 1.0, 1.0)
                }
                popover.permittedArrowDirections = 0uL
            }
            presentador.presentViewController(selector, animated = true, completion = null)
        }
    }

    private fun visible(controlador: UIViewController): UIViewController {
        controlador.presentedViewController?.let { return visible(it) }
        if (controlador is UINavigationController) {
            controlador.visibleViewController?.let { return visible(it) }
        }
        if (controlador is UITabBarController) {
            controlador.selectedViewController?.let { return visible(it) }
        }
        return controlador
    }
}
