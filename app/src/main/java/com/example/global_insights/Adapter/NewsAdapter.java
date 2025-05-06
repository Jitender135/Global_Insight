package com.example.global_insights.Adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.global_insights.R;
import com.example.global_insights.ReportNewsActivity;
import com.example.global_insights.model.Article;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private Context context;
    private List<Article> newsList;

    public NewsAdapter(Context context, List<Article> newsList) {
        this.context = context;
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_news_card, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        Article news = newsList.get(position);

        holder.title.setText(news.getTitle());
        holder.description.setText(news.getDescription());
        holder.sourceTag.setText(news.getSource() != null ? news.getSource().getName() : "Trusted");

        // "Read More" opens the article in browser
        holder.readMore.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getUrl()));
            context.startActivity(browserIntent);
        });

        // Report button logic: Navigate to ReportNewsActivity and pass article details
        holder.reportButton.setOnClickListener(v -> {
            Intent reportIntent = new Intent(context, ReportNewsActivity.class);
            reportIntent.putExtra("article_id", news.getId());  // Pass article ID
            reportIntent.putExtra("article_title", news.getTitle());  // Pass article title
            context.startActivity(reportIntent);
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    public void removeItem(int position) {
        newsList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, newsList.size());
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, readMore, sourceTag, reportButton;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.newsTitle);
            description = itemView.findViewById(R.id.newsDescription);
            readMore = itemView.findViewById(R.id.readMore);
            sourceTag = itemView.findViewById(R.id.sourceTag);
            reportButton = itemView.findViewById(R.id.reportButton);
        }
    }
}
