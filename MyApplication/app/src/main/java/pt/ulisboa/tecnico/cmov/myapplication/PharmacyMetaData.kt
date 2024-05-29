package pt.ulisboa.tecnico.cmov.myapplication

import android.os.Parcel
import android.os.Parcelable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PharmacyMetaData(var name: String? = null, var latitude: Double? = null,
                            var longitude: Double? = null, var picture: String? = null,
                            var locationName: String? = null) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readValue(Double::class.java.classLoader) as Double?,
        parcel.readValue(Double::class.java.classLoader) as Double?,
        parcel.readString(),
        parcel.readString()
    ){
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeValue(latitude)
        parcel.writeValue(longitude)
        parcel.writeString(picture)
        parcel.writeString(locationName)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getDistance(latitude: Double, longitude: Double): Double {
        val R = 6371e3
        val lat1 = Math.toRadians(this.latitude!!)
        val lat2 = Math.toRadians(latitude)
        val deltaLat = Math.toRadians(latitude - this.latitude!!)
        val deltaLng = Math.toRadians(longitude - this.longitude!!)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLng / 2) * sin(deltaLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
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