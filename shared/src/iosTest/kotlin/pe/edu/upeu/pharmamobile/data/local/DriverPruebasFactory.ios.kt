package pe.edu.upeu.pharmamobile.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import pe.edu.upeu.pharmamobile.db.PharmaDb

actual fun crearDriverPruebas(): SqlDriver {
    return NativeSqliteDriver(PharmaDb.Schema, "test.db", inMemory = true)
}
