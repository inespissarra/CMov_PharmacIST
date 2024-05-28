package pt.ulisboa.tecnico.cmov.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class MedicineRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase = dbHelper.writableDatabase

    fun insertOrUpdate(medicine: MedicineMetaData) {
        val contentValues = ContentValues().apply {
            put(DatabaseHelper.COLUMN_MEDICINE_NAME, medicine.name)
            put(DatabaseHelper.COLUMN_MEDICINE_IMAGE, medicine.image)
            //put(DatabaseHelper.COLUMN_MEDICINE_STOCK, medicine.stock)
            //put(DatabaseHelper.COLUMN_MEDICINE_PHARMACY, medicine.pharmacy)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_MEDICINE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getMedicine(name: String): MedicineMetaData? {
        val cursor = db.query(
            DatabaseHelper.TABLE_MEDICINE,
            null,
            "${DatabaseHelper.COLUMN_MEDICINE_NAME} = ?",
            arrayOf(name),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return MedicineMetaData(
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEDICINE_NAME)),
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEDICINE_IMAGE)),
                    //it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEDICINE_STOCK)),
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEDICINE_PHARMACY))
                )
            }
        }
        return null
    }

    fun deleteMedicine(name: String) {
        db.delete(DatabaseHelper.TABLE_MEDICINE, "${DatabaseHelper.COLUMN_MEDICINE_NAME} = ?", arrayOf(name))
    }

    fun clearMedicines() {
        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_MEDICINE}")
    }
}