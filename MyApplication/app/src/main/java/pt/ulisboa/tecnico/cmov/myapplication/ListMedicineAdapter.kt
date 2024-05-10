package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ListMedicineAdapter(private val context: Context, private var dataList: List<MedicineMetaData>)
    : RecyclerView.Adapter<ListMedicineAdapter.MedicineViewHolder>() {
    var onItemClick : ((MedicineMetaData) -> Unit)? = null

    class MedicineViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val recImage: ImageView
        val recName: TextView
        val stockNumber: TextView
        val buyButton: Button

        init {
            recImage = itemView.findViewById(R.id.recMedicineImage)
            recName = itemView.findViewById(R.id.recMedicineName)
            stockNumber = itemView.findViewById(R.id.stockNumber)
            buyButton = itemView.findViewById(R.id.buyButton)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.recycler_buy_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = dataList[position]
        Glide.with(context).load(medicine.image).into(holder.recImage)
        holder.recName.text = medicine.name
        holder.stockNumber.text = medicine.stock.toString()

        holder.buyButton.setOnClickListener {
            // TODO: Send intent to buy medicine
            val intent: Intent = Intent(context, BuyMedicineActivity::class.java)
            intent.putExtra("medicine", medicine.name)
            intent.putExtra("pharmacy", medicine.pharmacy)
            context.startActivity(intent)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(medicine)
        }
    }
}