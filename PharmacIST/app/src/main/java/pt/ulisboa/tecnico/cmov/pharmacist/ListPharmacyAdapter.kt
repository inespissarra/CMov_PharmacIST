package pt.ulisboa.tecnico.cmov.pharmacist

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

class ListPharmacyAdapter(private val context: Context,
                          private var dataList: ArrayList<Pair<PharmacyMetaData, Pair<Double, Int>>>)
    : RecyclerView.Adapter<ListPharmacyAdapter.PharmacyListViewHolder>() {
    var onItemClick : ((PharmacyMetaData) -> Unit)? = null

    inner class PharmacyListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val recPharmacyImage: ImageView = itemView.findViewById(R.id.recPharmacyImage)
        val recPharmacyName: TextView = itemView.findViewById(R.id.recPharmacyName)
        val recPharmacyStock: TextView = itemView.findViewById(R.id.recPharmacyStock)
        val recPharmacyLocation: TextView = itemView.findViewById(R.id.recPharmacyLocation)
        val recPharmacyDistance: TextView = itemView.findViewById(R.id.recPharmacyDistance)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmacyListViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_pharmacy, parent, false)
        return PharmacyListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: PharmacyListViewHolder, position: Int) {
        val pharmacyPair = dataList[position]
        val pharmacyMetaData = pharmacyPair.first
        val distanceStockPair = pharmacyPair.second
        val distance = distanceStockPair.first
        val stock = distanceStockPair.second

        if (checkConnectivity(context) == 2) {
            Glide.with(context).load(pharmacyMetaData.picture).into(holder.recPharmacyImage)
        } else {
            Glide.with(context).load(R.drawable.placeholder).into(holder.recPharmacyImage)
        }
        if (pharmacyMetaData.name != null) {
            holder.recPharmacyName.text = pharmacyMetaData.name?.capitalizeFirstLetter()
        } else {
            holder.recPharmacyName.text = context.getString(R.string.name_not_found)
        }
        holder.recPharmacyStock.text = stock.toString()

        if (pharmacyMetaData.locationName != null) {
            holder.recPharmacyLocation.text = pharmacyMetaData.locationName
        } else {
            holder.recPharmacyLocation.text = context.getString(R.string.location_not_found)
        }

        holder.recPharmacyDistance.text =
            context.getString(R.string.distance, String.format(Locale.US, "%.1f", distance))

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(pharmacyMetaData)
        }

        holder.recPharmacyImage.setOnClickListener {
            if (checkConnectivity(context) != 0) {
                Glide.with(context).load(pharmacyMetaData.picture).into(holder.recPharmacyImage)
            }
        }
    }

    private fun String.capitalizeFirstLetter(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    fun addPharmacyToList(data: Pair<PharmacyMetaData, Pair<Double, Int>>) {
        this.dataList.add(data)
    }

    fun setPharmacyDataList(dataList: ArrayList<Pair<PharmacyMetaData, Pair<Double, Int>>>) {
        this.dataList = dataList
    }

    private fun checkConnectivity(context: Context): Int {
        //
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return 1
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return 2
            }
        }
        return 0
    }
}