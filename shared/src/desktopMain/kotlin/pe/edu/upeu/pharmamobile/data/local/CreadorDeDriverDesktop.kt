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
        } else {
            driver.execute(null, """
                CREATE TABLE IF NOT EXISTS metadata_sincronizacion (
                    clave TEXT NOT NULL PRIMARY KEY,
                    valor TEXT NOT NULL
                );
            """.trimIndent(), 0)

            val columnas = driver.executeQuery(null, "PRAGMA table_info(producto);", { cursor ->
                val lista = mutableListOf<String>()
                while (cursor.next().value) {
                    lista.add(cursor.getString(1) ?: "")
                }
                app.cash.sqldelight.db.QueryResult.Value(lista)
            }, 0).value

            if (!columnas.contains("intentos")) {
                driver.execute(null, "ALTER TABLE producto ADD COLUMN intentos INTEGER NOT NULL DEFAULT 0;", 0)
            }
            if (!columnas.contains("ultimoError")) {
                driver.execute(null, "ALTER TABLE producto ADD COLUMN ultimoError TEXT;", 0)
            }
            if (!columnas.contains("falloPermanente")) {
                driver.execute(null, "ALTER TABLE producto ADD COLUMN falloPermanente INTEGER NOT NULL DEFAULT 0;", 0)
            }
            if (!columnas.contains("proximoReintentoEpochMillis")) {
                driver.execute(null, "ALTER TABLE producto ADD COLUMN proximoReintentoEpochMillis INTEGER NOT NULL DEFAULT 0;", 0)
            }
            if (!columnas.contains("claveIdempotencia")) {
                driver.execute(null, "ALTER TABLE producto ADD COLUMN claveIdempotencia TEXT;", 0)
            }
        }
        return driver
    }
}
