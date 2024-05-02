package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MedicineAdapter(private val context: Context, private var dataList: List<MedicineMetaData>): RecyclerView.Adapter<MedicineViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        Glide.with(context).load(dataList[position].image).into(holder.recImage)
        holder.recName.text = dataList[position].name
    }

    fun searchMedicineList(searchList: List<MedicineMetaData>) {
        dataList = searchList
        notifyDataSetChanged() // TODO: nao ta a funcionar, o eventChangeListener nao listen este notify
    }

}

class MedicineViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val recImage: ImageView = itemView.findViewById(R.id.recMedicineImage)
    val recName: TextView = itemView.findViewById(R.id.recMedicineName)

    /*init {
        recImage = itemView.findViewById(R.id.recMedicineImage)
        recName = itemView.findViewById(R.id.recMedicineName)
    }*/
}

