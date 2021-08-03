package com.mrx.clashRoyal

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ChestAdapter(
    context: Context,
    private val chestData: LinkedList<ClashRoyaleChestHelper.Chest>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mInflate = LayoutInflater.from(context)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chestNum: TextView = itemView.findViewById(R.id.tvChestNum)
        val chestName: TextView = itemView.findViewById(R.id.tvChestName)
        val chestIMG: ImageView = itemView.findViewById(R.id.imgChestIMG)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(mInflate.inflate(R.layout.chest_list, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mHolder = holder as ViewHolder
        val chest = chestData[position]
        mHolder.chestIMG.setImageBitmap(chest.chestIMG)
        mHolder.chestNum.text = chest.chestNum
        mHolder.chestName.text = chest.chestName
    }

    override fun getItemCount() = chestData.size
}