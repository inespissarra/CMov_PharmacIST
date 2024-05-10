package pt.ulisboa.tecnico.cmov.myapplication

import android.os.Parcel
import android.os.Parcelable

data class PharmacyMetaData(var name: String? = null, var latitude: Double? = null, var longitude: Double? = null, var picture: String? = null) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString()
    ){
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeValue(latitude)
        parcel.writeValue(longitude)
        parcel.writeString(picture)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PharmacyMetaData> {
        override fun createFromParcel(parcel: Parcel): PharmacyMetaData {
            return PharmacyMetaData(parcel)
        }

        override fun newArray(size: Int): Array<PharmacyMetaData?> {
            return arrayOfNulls(size)
        }
    }
}