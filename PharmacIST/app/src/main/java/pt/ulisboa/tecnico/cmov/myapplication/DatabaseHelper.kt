package pt.ulisboa.tecnico.cmov.pharmacist
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 3
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
        const val COLUMN_MEDICINE_DESCRIPTION = "description"
        const val COLUMN_MEDICINE_BARCODE = "barcode"

        const val TABLE_FAVORITE_PHARMACY = "favoritePharmacy"
        const val COLUMN_FAVORITE_PHARMACY_NAME = "name"
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
                $COLUMN_MEDICINE_DESCRIPTION TEXT,
                $COLUMN_MEDICINE_BARCODE TEXT
            )
        """
        val createFavoritePharmacyTable = """
            CREATE TABLE $TABLE_FAVORITE_PHARMACY (
                $COLUMN_FAVORITE_PHARMACY_NAME TEXT PRIMARY KEY
            )
        """
        db.execSQL(createPharmacyTable)
        db.execSQL(createMedicineTable)
        db.execSQL(createFavoritePharmacyTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PHARMACY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICINE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITE_PHARMACY")
        onCreate(db)
    }
}