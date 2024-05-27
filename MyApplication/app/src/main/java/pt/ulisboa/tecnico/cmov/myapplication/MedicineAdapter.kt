package pt.ulisboa.tecnico.cmov.myapplication

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

class MedicineAdapter(private val context: Context, private var dataList: ArrayList<MedicineMetaData>)
    : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {
    var onItemClick : ((MedicineMetaData) -> Unit)? = null

    inner class MedicineViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val recImage: ImageView = itemView.findViewById(R.id.recMedicineImage)
        val recName: TextView = itemView.findViewById(R.id.recMedicineName)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = dataList[position]

        if (checkConnectivity(context) == 2) {
            Glide.with(context).load(medicine.image).into(holder.recImage)
        } else {
            Glide.with(context).load(R.drawable.placeholder).into(holder.recImage)
        }
        holder.recName.text = medicine.name?.capitalizeFirstLetter()

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(medicine)
        }

        holder.recImage.setOnClickListener {
            if (checkConnectivity(context) != 0) {
                Glide.with(context).load(medicine.image).into(holder.recImage)
            }
        }
    }

    private fun String.capitalizeFirstLetter(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    fun setMedicineList(searchList: ArrayList<MedicineMetaData>) {
        dataList = searchList
    }

    /**
     * Checks what type of connectivity the user has
     *
     * @param context The context of the caller
     * @return Int: 0 -> No connectivity; 1 -> Mobile Data; WiFi or stronger -> 2
     */
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



