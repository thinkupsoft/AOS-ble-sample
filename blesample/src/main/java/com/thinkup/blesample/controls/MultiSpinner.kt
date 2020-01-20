package com.thinkup.blesample.controls

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import com.thinkup.blesample.R

class MultiSpinner(context: Context, attributeSet: AttributeSet) : Spinner(context, attributeSet) {

    var list: List<Any>? = null
    var selected = BooleanArray(0)
    lateinit var adapter: MultiAdapter

    fun setItems(items: List<Any>) {
        this.list = items
        // all selected by default
        selected = BooleanArray(items.size)
        for (i in selected.indices) selected[i] = true
        // all text on the spinner
        adapter = MultiAdapter(context, items.map { it.toString() })
        setAdapter(adapter)
    }

    fun getSelecteds(): List<Any> {
        val selecteds = mutableListOf<Any>()
        if (::adapter.isInitialized) {
            adapter.indexes.forEach {
                selecteds.add(list!![it])
            }
        }
        return selecteds
    }

    class MultiAdapter(context: Context, private val items: List<String>) : ArrayAdapter<String>(context, 0, items) {

        val indexes = mutableListOf<Int>()

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getCustomView(position, convertView)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getCustomView(position, convertView)
        }

        private fun getCustomView(position: Int, convertView: View?): View {
            var holder: ViewHolder
            var converedtView: View? = null

            if (convertView == null) {
                val layoutInflator = LayoutInflater.from(context)
                converedtView = layoutInflator.inflate(R.layout.check_spinner_item, null)
                holder = ViewHolder()
                holder.mCheckBox = converedtView.findViewById(R.id.check) as CheckBox
                converedtView.tag = holder
            } else {
                converedtView = convertView
                holder = converedtView.tag as ViewHolder
            }
            holder.mCheckBox?.setOnCheckedChangeListener{_,_->}
            holder.mCheckBox?.text = items[position]
            holder.mCheckBox?.isChecked = indexes.contains(position)
            holder.mCheckBox?.setOnCheckedChangeListener { compoundButton, b ->
                if (b) indexes.add(position)
                else indexes.remove(position)
            }

            return converedtView!!
        }

        private class ViewHolder {
            var mCheckBox: CheckBox? = null
        }
    }
}