package pt.ulisboa.tecnico.cmov.pharmacist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class FavoritePharmaciesRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase = dbHelper.writableDatabase

    fun insertOrUpdate(name: String) {
        val contentValues = ContentValues().apply {
            put(DatabaseHelper.COLUMN_FAVORITE_PHARMACY_NAME, name)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_FAVORITE_PHARMACY, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun isFavoritePharmacy(name: String): Boolean {
        val cursor = db.query(
            DatabaseHelper.TABLE_FAVORITE_PHARMACY,
            null,
            "${DatabaseHelper.COLUMN_FAVORITE_PHARMACY_NAME} = ?",
            arrayOf(name),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return true
            }
        }
        return false
    }


    fun deletePharmacy(name: String) {
        db.delete(DatabaseHelper.TABLE_FAVORITE_PHARMACY, "${DatabaseHelper.COLUMN_FAVORITE_PHARMACY_NAME} = ?", arrayOf(name))
    }

    fun clearPharmacies() {
        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_FAVORITE_PHARMACY}")
    }
}