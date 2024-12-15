package com.xiaoc.warlock.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.xiaoc.warlock.R;

import java.util.ArrayList;
import java.util.List;

public class InfoAdapter extends RecyclerView.Adapter<InfoAdapter.ViewHolder> {
    private final List<InfoItem> items = new ArrayList<>();

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView titleText;
        private final ImageView expandIcon;
        private final View headerLayout;
        private final View detailsLayout;
        private final LinearLayout detailsList;

        public ViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            titleText = view.findViewById(R.id.titleText);
            expandIcon = view.findViewById(R.id.expandIcon);
            headerLayout = view.findViewById(R.id.headerLayout);
            detailsLayout = view.findViewById(R.id.detailsLayout);
            detailsList = view.findViewById(R.id.detailsList);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_info_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InfoItem item = items.get(position);

        holder.titleText.setText(item.getTitle());

        // 设置展开/收起状态
        holder.detailsLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
        holder.expandIcon.setRotation(item.isExpanded() ? 180 : 0);

        // 点击事件
        holder.headerLayout.setOnClickListener(v -> {
            item.setExpanded(!item.isExpanded());
            notifyItemChanged(position);
        });

        // 填充详情列表
        holder.detailsList.removeAllViews();
        for (InfoItem.DetailItem detail : item.getDetails()) {
            View detailView = LayoutInflater.from(holder.detailsList.getContext())
                    .inflate(R.layout.item_detail, holder.detailsList, false);

            TextView keyText = detailView.findViewById(R.id.keyText);
            TextView valueText = detailView.findViewById(R.id.valueText);

            keyText.setText(detail.getKey());
            valueText.setText(detail.getValue());

            holder.detailsList.addView(detailView);
        }
    }

    public void addItem(InfoItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }
    public void setItems(List<InfoItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}