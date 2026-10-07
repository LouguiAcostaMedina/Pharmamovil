package pe.edu.upeu.pharmamobile.platform

import android.content.Context
import android.content.Intent
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor

class CompartidorAndroid(private val context: Context) : Compartidor {
    override fun compartir(texto: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, texto)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
