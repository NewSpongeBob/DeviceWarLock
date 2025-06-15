package com.xiaoc.warlock.ui.adapter;

import android.graphics.Color;
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
import com.xiaoc.warlock.Util.ClipboardUtil;

import java.util.ArrayList;
import java.util.List;

public class InfoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<InfoItem> items;
    private final boolean isEnvironmentInfo;
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_EMPTY = 1;

    public InfoAdapter(boolean isEnvironmentInfo) {
        this.isEnvironmentInfo = isEnvironmentInfo;
        this.items = new ArrayList<>();
    }
    
    public InfoAdapter(List<InfoItem> items) {
        this.isEnvironmentInfo = false;
        this.items = items;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (isEnvironmentInfo && viewType == TYPE_EMPTY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_empty_state, parent, false);
            return new EmptyViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_info_card, parent, false);
        return new InfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyViewHolder && isEnvironmentInfo) {
            ((EmptyViewHolder) holder).bind();
        } else if (holder instanceof InfoViewHolder) {
            ((InfoViewHolder) holder).bind(items.get(position), isEnvironmentInfo);
        }
    }

    @Override
    public int getItemCount() {
        return isEnvironmentInfo && items.isEmpty() ? 1 : items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (isEnvironmentInfo && items.isEmpty()) ? TYPE_EMPTY : TYPE_NORMAL;
    }

    public void addItem(InfoItem item) {
        boolean wasEmpty = items.isEmpty();
        items.add(item);

        if (wasEmpty && isEnvironmentInfo) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(items.size() - 1);
        }
    }

    public void setItems(List<InfoItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class InfoViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView titleText;
        private final ImageView expandIcon;
        private final View headerLayout;
        private final View detailsLayout;
        private final LinearLayout detailsList;

        InfoViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            titleText = view.findViewById(R.id.titleText);
            expandIcon = view.findViewById(R.id.expandIcon);
            headerLayout = view.findViewById(R.id.headerLayout);
            detailsLayout = view.findViewById(R.id.detailsLayout);
            detailsList = view.findViewById(R.id.detailsList);
        }

        void bind(InfoItem item, boolean isEnvironmentInfo) {
            titleText.setText(item.getTitle());

            detailsLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
            expandIcon.setRotation(item.isExpanded() ? 180 : 0);

            headerLayout.setOnClickListener(v -> {
                item.setExpanded(!item.isExpanded());
                detailsLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
                expandIcon.animate().rotation(item.isExpanded() ? 180 : 0).setDuration(200).start();
            });

            detailsList.removeAllViews();
            for (InfoItem.DetailItem detail : item.getDetails()) {
                View detailView = LayoutInflater.from(detailsList.getContext())
                        .inflate(R.layout.item_detail, detailsList, false);

                TextView keyText = detailView.findViewById(R.id.keyText);
                TextView valueText = detailView.findViewById(R.id.valueText);

                keyText.setText(detail.getKey());
                valueText.setText(detail.getValue());

                detailsList.addView(detailView);
            }

            cardView.setOnLongClickListener(v -> {
                ClipboardUtil.copyInfoItemToClipboard(v.getContext(), item, isEnvironmentInfo);
                return true;
            });

            cardView.setClickable(true);
            cardView.setFocusable(true);
            cardView.setRippleColorResource(R.color.ripple_color);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        private final ImageView emptyStateIcon;
        private final TextView emptyStateTitle;
        private final TextView emptyStateDescription;

        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            emptyStateIcon = itemView.findViewById(R.id.emptyStateIcon);
            emptyStateTitle = itemView.findViewById(R.id.emptyStateTitle);
            emptyStateDescription = itemView.findViewById(R.id.emptyStateDescription);
        }

        void bind() {
            emptyStateIcon.setImageResource(R.drawable.ic_security);
            emptyStateIcon.setColorFilter(Color.parseColor("#4CAF50"));
            emptyStateTitle.setText("环境安全");
            emptyStateDescription.setText("未检测到任何安全风险\n您的设备环境运行正常");
        }
    }
}