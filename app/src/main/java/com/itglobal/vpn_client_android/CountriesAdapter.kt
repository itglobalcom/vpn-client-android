package com.itglobal.vpn_client_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.itglobal.vpn_client_android.extensions.loadFlag
import com.itglobal.vpn_client_android.models.Country
import kotlinx.android.synthetic.main.item_country.view.*

class CountriesAdapter(private val serverClickListener: (Country) -> Unit) :
    RecyclerView.Adapter<CountriesAdapter.CountriesViewHolder>() {

    private var items: MutableList<Country> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountriesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country, parent, false)
        return CountriesViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CountriesViewHolder, position: Int) {
        items.getOrNull(position)?.let { item ->
            holder.apply {
                country.text = item.name
                details.text = item.code
                bind(item, serverClickListener)
                icon.loadFlag(item.flag)
            }
        }
    }

    fun setData(data: List<Country>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class CountriesViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.ivIcon
        val signal: ImageView = view.ivSignal
        val country: TextView = view.tvCountryName
        val details: TextView = view.tvDetails

        fun bind(country: Country, listener: (Country) -> Unit) {
            view.setOnClickListener { listener(country) }
        }
    }

}