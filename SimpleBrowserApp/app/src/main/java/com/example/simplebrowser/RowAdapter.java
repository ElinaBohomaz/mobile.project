package com.example.simplebrowser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RowAdapter extends RecyclerView.Adapter<RowAdapter.VH> {

    private final List<AppDb.Row> items = new ArrayList<>();
    private final OnClick onClick;
    private final OnClick onDelete;

    public RowAdapter(OnClick onClick, OnClick onDelete) {
        this.onClick = onClick;
        this.onDelete = onDelete;
    }

    public void set(List<AppDb.Row> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int position) {
        AppDb.Row it = items.get(position);

        h.title.setText(it.title);
        h.url.setText(it.url);

        h.badge.setText(it.isBookmark
                ? h.itemView.getContext().getString(R.string.badge_bookmark)
                : h.itemView.getContext().getString(R.string.badge_history));

        h.badge.setVisibility(View.VISIBLE);

        h.itemView.setOnClickListener(v -> onClick.run(it));
        h.btnDelete.setOnClickListener(v -> onDelete.run(it));

        h.itemView.setAlpha(0f);
        h.itemView.setTranslationY(10f);
        h.itemView.animate().alpha(1f).translationY(0f).setDuration(180).start();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView url;
        final TextView badge;
        final MaterialButton btnDelete;

        VH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.rowTitle);
            url = itemView.findViewById(R.id.rowUrl);
            badge = itemView.findViewById(R.id.rowBadge);
            btnDelete = itemView.findViewById(R.id.rowDelete);
        }
    }

    public interface OnClick {
        void run(AppDb.Row item);
    }
}
