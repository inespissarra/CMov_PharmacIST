package pt.ulisboa.tecnico.cmov.pharmacist

import android.os.Parcel
import android.os.Parcelable

data class MedicineMetaData(var name: String? = null, var image: String? = null,
    var description: String? = null, var barcode: String? = null) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(image)
        parcel.writeString(description)
        parcel.writeString(barcode)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MedicineMetaData> {
        override fun createFromParcel(parcel: Parcel): MedicineMetaData {
            return MedicineMetaData(parcel)
        }

        override fun newArray(size: Int): Array<MedicineMetaData?> {
            return arrayOfNulls(size)
        }
    }
}