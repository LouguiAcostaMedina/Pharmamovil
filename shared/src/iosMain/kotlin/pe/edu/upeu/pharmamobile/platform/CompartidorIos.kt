package pe.edu.upeu.pharmamobile.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor

class CompartidorIos : Compartidor {
    override fun compartir(texto: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val window = UIApplication.sharedApplication.keyWindow
            val rootViewController = window?.rootViewController
            
            if (rootViewController != null) {
                val activityViewController = UIActivityViewController(
                    activityItems = listOf(texto),
                    applicationActivities = null
                )
                
                // For iPad support
                val popover = activityViewController.popoverPresentationController
                if (popover != null) {
                    popover.sourceView = rootViewController.view
                    popover.sourceRect = rootViewController.view.bounds
                }
                
                rootViewController.presentViewController(
                    activityViewController,
                    animated = true,
                    completion = null
                )
            }
        }
    }
}
