package pt.ulisboa.tecnico.cmov.pharmacist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class MedicineWithNotificationRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase = dbHelper.writableDatabase

    fun insertOrUpdate(name: String) {
        val contentValues = ContentValues().apply {
            put(DatabaseHelper.COLUMN_MEDICINE_WITH_NOTIFICATION_NAME, name)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_MEDICINE_WITH_NOTIFICATION, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun isMedicineWithNotification(name: String): Boolean {
        val cursor = db.query(
            DatabaseHelper.TABLE_MEDICINE_WITH_NOTIFICATION,
            null,
            "${DatabaseHelper.COLUMN_MEDICINE_WITH_NOTIFICATION_NAME} = ?",
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

    fun getMedicinesWithNotification(): List<String> {
        val favoritePharmacies = mutableListOf<String>()
        val cursor = db.query(
            DatabaseHelper.TABLE_MEDICINE_WITH_NOTIFICATION,
            arrayOf(DatabaseHelper.COLUMN_MEDICINE_WITH_NOTIFICATION_NAME),
            null,
            null,
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEDICINE_WITH_NOTIFICATION_NAME))
                    favoritePharmacies.add(name)
                } while (it.moveToNext())
            }
        }
        return favoritePharmacies
    }


    fun deleteMedicine(name: String) {
        db.delete(DatabaseHelper.TABLE_MEDICINE_WITH_NOTIFICATION, "${DatabaseHelper.COLUMN_MEDICINE_WITH_NOTIFICATION_NAME} = ?", arrayOf(name))
    }

    fun clearMedicines() {
        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_MEDICINE_WITH_NOTIFICATION}")
    }
}