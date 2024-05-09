package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MedicineAdapter(private val context: Context, private var dataList: List<MedicineMetaData>)
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
        Glide.with(context).load(medicine.image).into(holder.recImage)
        holder.recName.text = medicine.name?.capitalizeFirstLetter()

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(medicine)
        }
    }

    private fun String.capitalizeFirstLetter(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    fun setMedicineList(searchList: List<MedicineMetaData>) {
        dataList = searchList
        notifyDataSetChanged() // TODO: nao ta a funcionar, o eventChangeListener nao listen este notify
    }

}



