package app.ind.currancyconvodk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val historyList: List<Pair<String, String>>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val (fromTo, result) = historyList[position]
        val (fromCurrency, toCurrency) = fromTo.split(" to ")
        val (fromPrice, toPrice) = result.split(" to ")

        holder.tvFrom.text = fromCurrency
        holder.tvTo.text = toCurrency
        holder.tvFromPrice.text = fromPrice
        holder.tvToPrice.text = toPrice
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFrom: TextView = itemView.findViewById(R.id.tvfrom)
        val tvTo: TextView = itemView.findViewById(R.id.tvTo)
        val tvFromPrice: TextView = itemView.findViewById(R.id.fromPrice)
        val tvToPrice: TextView = itemView.findViewById(R.id.toPrice)
    }
}
