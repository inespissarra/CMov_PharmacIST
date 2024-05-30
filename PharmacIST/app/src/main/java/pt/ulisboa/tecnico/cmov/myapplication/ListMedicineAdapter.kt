package pt.ulisboa.tecnico.cmov.pharmacist

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ListMedicineAdapter(private val context: Context, private var dataList: Map<MedicineMetaData, Int>, private var pharmacyName : String)
    : RecyclerView.Adapter<ListMedicineAdapter.MedicineViewHolder>() {
    var onItemClick : ((MedicineMetaData) -> Unit)? = null
    private var medicineStockList: ArrayList<Pair<MedicineMetaData, Int>> =
        dataList.map { Pair(it.key, it.value) }.toCollection(ArrayList())
    private var pharmacy : String = pharmacyName

    inner class MedicineViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val recImage: ImageView
        val recName: TextView
        val stockNumber: TextView
        val buyButton: ImageButton

        init {
            recImage = itemView.findViewById(R.id.recMedicineImage)
            recName = itemView.findViewById(R.id.recMedicineName)
            stockNumber = itemView.findViewById(R.id.stockAmount)
            buyButton = itemView.findViewById(R.id.buyButton)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.recycler_buy_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun getItemCount(): Int {
        return medicineStockList.size
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = medicineStockList[position].first
        val amount = medicineStockList[position].second

        Glide.with(context).load(medicine.image).into(holder.recImage)

        holder.recName.text = medicine.name
        holder.stockNumber.text = amount.toString()

        holder.buyButton.setOnClickListener {
            val intent = Intent(context, BuyMedicineActivity::class.java)
            intent.putExtra("medicine", medicine)
            intent.putExtra("stock", amount)
            intent.putExtra("pharmacyName", pharmacy)
            context.startActivity(intent)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(medicine)
        }
    }

    fun addMedicineStock(medicine: MedicineMetaData, stock: Int) {
        medicineStockList.add(Pair(medicine, stock))
    }

    fun setMedicineStockList(dataList: Map<MedicineMetaData, Int>) {
        this.medicineStockList = dataList.map { Pair(it.key, it.value) }.toCollection(ArrayList())
    }
}