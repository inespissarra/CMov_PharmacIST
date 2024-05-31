package pt.ulisboa.tecnico.cmov.pharmacist

import android.os.Parcel
import android.os.Parcelable

data class MedicinePharmacyDBEntryData(
    var medicineMetaData: MedicineMetaData? = null,
    var pharmacyMap: HashMap<String, Pair<PharmacyMetaData, Int>>? = null, var closestPharmacy: PharmacyMetaData? = null,
    var closestDistance: Double? = null) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readTypedObject(MedicineMetaData.CREATOR),
        readPharmacyMap(parcel),
        parcel.readTypedObject(PharmacyMetaData.CREATOR),
        parcel.readValue(Double::class.java.classLoader) as? Double
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedObject(medicineMetaData, flags)
        writePharmacyMap(parcel, pharmacyMap)
        parcel.writeTypedObject(closestPharmacy, flags)
        parcel.writeValue(closestDistance)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MedicinePharmacyDBEntryData> {
        override fun createFromParcel(parcel: Parcel): MedicinePharmacyDBEntryData {
            return MedicinePharmacyDBEntryData(parcel)
        }

        override fun newArray(size: Int): Array<MedicinePharmacyDBEntryData?> {
            return arrayOfNulls(size)
        }

        private fun readPharmacyMap(parcel: Parcel) : HashMap<String, Pair<PharmacyMetaData, Int>>? {
            val size = parcel.readInt()
            if (size == -1) return null
            val map = HashMap<String, Pair<PharmacyMetaData, Int>>(size)
            repeat(size) {
                val key = parcel.readString() ?: return@repeat
                val value1 = parcel.readParcelable<PharmacyMetaData>(PharmacyMetaData::class.java.classLoader)
                val value2 = parcel.readInt()
                if (value1 != null) {
                    map[key] = Pair(value1, value2)
                }
            }
            return map
        }

        private fun writePharmacyMap(parcel: Parcel, map: HashMap<String, Pair<PharmacyMetaData, Int>>?) {
            if (map == null) {
                parcel.writeInt(-1)
                return
            }
            parcel.writeInt(map.size)
            for ((key, value) in map) {
                parcel.writeString(key)
                parcel.writeParcelable(value.first, 0)
                parcel.writeInt(value.second)
            }
        }
    }
}