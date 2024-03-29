package com.jehutyno.yomikata.filechooser;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jehutyno.yomikata.R;

class ItemHolder extends RecyclerView.ViewHolder {
    private ImageView ivItemIcon;
    private TextView tvItemName;
    private OnItemClickListener itemClickListener;

    interface OnItemClickListener {
        void onItemClick(Item item);
    }

    ItemHolder(View itemView, OnItemClickListener itemClickListener) {
        super(itemView);

        this.itemClickListener = itemClickListener;

        ivItemIcon = itemView.findViewById(R.id.item_icon_imageview);
        tvItemName = itemView.findViewById(R.id.item_name_textview);
    }

    void bind(final Item item) {
        tvItemName.setText(item.getName());
        ivItemIcon.setImageDrawable(item.getIcon());
        itemView.setOnClickListener(view -> itemClickListener.onItemClick(item));
    }
}
