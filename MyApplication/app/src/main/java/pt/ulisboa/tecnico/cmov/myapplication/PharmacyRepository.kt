package pt.ulisboa.tecnico.cmov.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class PharmacyRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase = dbHelper.writableDatabase

    fun insertOrUpdate(pharmacy: PharmacyMetaData) {
        val contentValues = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PHARMACY_NAME, pharmacy.name)
            put(DatabaseHelper.COLUMN_PHARMACY_LATITUDE, pharmacy.latitude)
            put(DatabaseHelper.COLUMN_PHARMACY_LONGITUDE, pharmacy.longitude)
            put(DatabaseHelper.COLUMN_PHARMACY_PICTURE, pharmacy.picture)
            put(DatabaseHelper.COLUMN_PHARMACY_LOCATION_NAME, pharmacy.locationName)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_PHARMACY, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getPharmacy(name: String): PharmacyMetaData? {
        val cursor = db.query(
            DatabaseHelper.TABLE_PHARMACY,
            null,
            "${DatabaseHelper.COLUMN_PHARMACY_NAME} COLLATE NOCASE = ?",
            arrayOf(name),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return PharmacyMetaData(
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_NAME)),
                    it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_LATITUDE)),
                    it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_LONGITUDE)),
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_PICTURE)),
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_LOCATION_NAME))
                )
            }
        }
        return null
    }

    fun getNearbyPharmacies(centerLat: Double, centerLng: Double, radius: Double): ArrayList<PharmacyMetaData> {
        val minLat = centerLat - radius
        val maxLat = centerLat + radius
        val minLng = centerLng - radius
        val maxLng = centerLng + radius

        val query = """
            SELECT * FROM ${DatabaseHelper.TABLE_PHARMACY}
            WHERE ${DatabaseHelper.COLUMN_PHARMACY_LATITUDE} BETWEEN ? AND ?
            AND ${DatabaseHelper.COLUMN_PHARMACY_LONGITUDE} BETWEEN ? AND ?
        """

        val cursor = db.rawQuery(query, arrayOf(minLat.toString(), maxLat.toString(), minLng.toString(), maxLng.toString()))
        val pharmacies = ArrayList<PharmacyMetaData>()

        cursor.use {
            while (it.moveToNext()) {
                val pharmacy = PharmacyMetaData(
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_NAME)),
                    it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_LATITUDE)),
                    it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_LONGITUDE)),
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_PICTURE)),
                    it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHARMACY_LOCATION_NAME))
                )
                pharmacies.add(pharmacy)
            }
        }

        return pharmacies
    }

    fun deletePharmacy(name: String) {
        db.delete(DatabaseHelper.TABLE_PHARMACY, "${DatabaseHelper.COLUMN_PHARMACY_NAME} = ?", arrayOf(name))
    }

    fun clearPharmacies() {
        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_PHARMACY}")
    }
}