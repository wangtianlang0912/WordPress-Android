package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BigTitle

class BigTitleViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_big_title_item
) {
    private val text = itemView.findViewById<TextView>(id.text)
    fun bind(item: BigTitle) {
        text.setText(item.textResource)
    }
}
