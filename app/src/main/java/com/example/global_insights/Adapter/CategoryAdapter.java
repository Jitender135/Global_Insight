package com.example.global_insights.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.global_insights.R;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public static class CategoryItem {
        public String name;
        public String apiKey;
        public int imageResId;

        public CategoryItem(String name, String apiKey, int imageResId) {
            this.name = name;
            this.apiKey = apiKey;
            this.imageResId = imageResId;
        }
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem item);
    }

    private Context context;
    private List<CategoryItem> categoryList;
    private OnCategoryClickListener listener;

    public CategoryAdapter(Context context, List<CategoryItem> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem item = categoryList.get(position);
        holder.name.setText(item.name);
        if (item.imageResId != 0) {
            holder.image.setImageResource(item.imageResId);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView image;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.categoryName);
            image = itemView.findViewById(R.id.categoryImage);
        }
    }
}
