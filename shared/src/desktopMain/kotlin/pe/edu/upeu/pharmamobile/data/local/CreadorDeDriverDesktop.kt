package pe.edu.upeu.pharmamobile.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.edu.upeu.pharmamobile.db.PharmaDb
import java.io.File

class CreadorDeDriverDesktop : CreadorDeDriver {

    override fun crear(): SqlDriver {
        val appDir = File(System.getProperty("user.home"), ".pharmamobile")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        val dbFile = File(appDir, "pharma.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (!dbFile.exists() || dbFile.length() == 0L) {
            PharmaDb.Schema.create(driver)
        }
        return driver
    }
}
