package pe.edu.upeu.pharmamobile.android

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pe.edu.upeu.pharmamobile.di.appModule
import pe.edu.upeu.pharmamobile.di.platformModule

class PharmaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PharmaApplication)
            modules(appModule, platformModule)
        }
    }
}
