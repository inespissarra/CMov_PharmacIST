package pt.ulisboa.tecnico.cmov.myapplication
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "cache.db"

        const val TABLE_PHARMACY = "pharmacy"
        const val COLUMN_PHARMACY_NAME = "name"
        const val COLUMN_PHARMACY_LATITUDE = "latitude"
        const val COLUMN_PHARMACY_LONGITUDE = "longitude"
        const val COLUMN_PHARMACY_PICTURE = "picture"
        const val COLUMN_PHARMACY_LOCATION_NAME = "location_name"

        const val TABLE_MEDICINE = "medicine"
        const val COLUMN_MEDICINE_NAME = "name"
        const val COLUMN_MEDICINE_IMAGE = "image"
        const val COLUMN_MEDICINE_STOCK = "stock"
        const val COLUMN_MEDICINE_PHARMACY = "pharmacy"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createPharmacyTable = """
            CREATE TABLE $TABLE_PHARMACY (
                $COLUMN_PHARMACY_NAME TEXT PRIMARY KEY,
                $COLUMN_PHARMACY_LATITUDE REAL,
                $COLUMN_PHARMACY_LONGITUDE REAL,
                $COLUMN_PHARMACY_PICTURE TEXT,
                $COLUMN_PHARMACY_LOCATION_NAME TEXT
            )
        """
        val createMedicineTable = """
            CREATE TABLE $TABLE_MEDICINE (
                $COLUMN_MEDICINE_NAME TEXT PRIMARY KEY,
                $COLUMN_MEDICINE_IMAGE TEXT,
                $COLUMN_MEDICINE_STOCK INTEGER,
                $COLUMN_MEDICINE_PHARMACY TEXT
            )
        """
        db.execSQL(createPharmacyTable)
        db.execSQL(createMedicineTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PHARMACY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICINE")
        onCreate(db)
    }
}