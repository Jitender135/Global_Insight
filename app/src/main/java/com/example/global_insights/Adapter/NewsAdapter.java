package com.example.global_insights.Adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.global_insights.BookmarkManager;
import com.example.global_insights.R;
import com.example.global_insights.ReportNewsActivity;
import com.example.global_insights.model.Article;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private static final int VIEW_TYPE_STANDARD = 0;
    private static final int VIEW_TYPE_INSHORTS = 1;

    private Context context;
    private List<Article> newsList;
    private boolean isDarkMode = false;
    private boolean isInshortsStyle = false;

    public NewsAdapter(Context context, List<Article> newsList) {
        this.context = context;
        this.newsList = newsList;
    }

    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
        notifyDataSetChanged();
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public void setInshortsStyle(boolean inshortsStyle) {
        this.isInshortsStyle = inshortsStyle;
        notifyDataSetChanged();
    }

    public boolean isInshortsStyle() {
        return isInshortsStyle;
    }

    @Override
    public int getItemViewType(int position) {
        return isInshortsStyle ? VIEW_TYPE_INSHORTS : VIEW_TYPE_STANDARD;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == VIEW_TYPE_INSHORTS) ? R.layout.item_news_inshorts : R.layout.item_news_card;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        Article news = newsList.get(position);

        holder.title.setText(news.getTitle());

        String desc = news.getDescription();
        if (desc == null || desc.trim().isEmpty() || desc.trim().equalsIgnoreCase("comments") || desc.contains("[Removed]")) {
            holder.description.setVisibility(View.GONE);
        } else {
            holder.description.setText(desc.trim());
            holder.description.setVisibility(View.VISIBLE);
        }

        String sourceName = news.getSource() != null && news.getSource().getName() != null && !news.getSource().getName().isEmpty() ? news.getSource().getName() : "Global News";
        holder.sourceTag.setText(sourceName);

        String publishedAt = news.getPublishedAt();
        String timeAgo = getRelativeTimeSpan(publishedAt);
        if (holder.publishedTime != null) {
            if (timeAgo != null && !timeAgo.isEmpty()) {
                holder.publishedTime.setText(timeAgo);
                holder.publishedTime.setVisibility(View.VISIBLE);
            } else {
                holder.publishedTime.setVisibility(View.GONE);
            }
        }

        // Image Loading using Glide: Show image if valid and loaded, otherwise collapse newsImage (View.GONE)
        if (holder.newsImage != null) {
            String imageUrl = news.getUrlToImage();
            if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.equalsIgnoreCase("null") && !imageUrl.contains("[Removed]")) {
                String finalUrl = imageUrl.trim();
                holder.newsImage.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(context)
                        .load(finalUrl)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                holder.newsImage.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                holder.newsImage.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(holder.newsImage);
            } else {
                holder.newsImage.setImageDrawable(null);
                holder.newsImage.setVisibility(View.GONE);
            }
        }

        // Footer Metadata binding (Inshorts layout)
        if (holder.newsFooter != null) {
            String timeStr = (timeAgo == null || timeAgo.isEmpty()) ? "recently" : timeAgo;
            String authorStr = (news.getAuthor() != null && !news.getAuthor().trim().isEmpty() && !news.getAuthor().contains("http")) ? news.getAuthor().trim() : null;

            StringBuilder footerSb = new StringBuilder(timeStr);
            if (authorStr != null) footerSb.append(" | ").append(authorStr);
            footerSb.append(" | ").append(sourceName);

            holder.newsFooter.setText(footerSb.toString());
        }

        // Share Icon Action
        if (holder.shareIcon != null) {
            holder.shareIcon.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, news.getTitle());
                String shareBody = news.getTitle() + (news.getUrl() != null ? "\n\nRead more at: " + news.getUrl() : "");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                context.startActivity(Intent.createChooser(shareIntent, "Share Article via"));
            });
        }

        // Audio Button Action (Text-To-Speech)
        if (holder.audioButton != null) {
            holder.audioButton.setOnClickListener(v -> {
                if (audioClickListener != null) {
                    audioClickListener.onAudioClick(news);
                }
            });
        }

        // Selective Dark Card styling
        if (isDarkMode) {
            holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#1E222A"));
            holder.title.setTextColor(android.graphics.Color.WHITE);
            holder.description.setTextColor(android.graphics.Color.parseColor("#D0D4DC"));
            holder.readMore.setTextColor(android.graphics.Color.parseColor("#64B5F6"));
            if (holder.publishedTime != null) holder.publishedTime.setTextColor(android.graphics.Color.parseColor("#A0AAB8"));
            if (holder.newsFooter != null) holder.newsFooter.setTextColor(android.graphics.Color.parseColor("#A0AAB8"));
            if (holder.shareIcon != null) holder.shareIcon.setColorFilter(android.graphics.Color.WHITE);
        } else {
            holder.cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
            holder.title.setTextColor(android.graphics.Color.BLACK);
            holder.description.setTextColor(android.graphics.Color.parseColor("#757575"));
            holder.readMore.setTextColor(android.graphics.Color.parseColor("#1A3D90"));
            if (holder.publishedTime != null) holder.publishedTime.setTextColor(android.graphics.Color.parseColor("#757575"));
            if (holder.newsFooter != null) holder.newsFooter.setTextColor(android.graphics.Color.parseColor("#888888"));
            if (holder.shareIcon != null) holder.shareIcon.setColorFilter(android.graphics.Color.BLACK);
        }

        // Bookmark status check & listener
        boolean isBookmarked = BookmarkManager.isBookmarked(context, news);
        holder.bookmarkIcon.setImageResource(isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

        holder.bookmarkIcon.setOnClickListener(v -> {
            boolean nowBookmarked = BookmarkManager.toggleBookmark(context, news);
            holder.bookmarkIcon.setImageResource(nowBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            Toast.makeText(context, nowBookmarked ? "Article Bookmarked!" : "Removed from Bookmarks", Toast.LENGTH_SHORT).show();
        });

        // "Read More" opens the article in browser
        holder.readMore.setOnClickListener(v -> {
            if (news.getUrl() != null && !news.getUrl().isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getUrl()));
                context.startActivity(browserIntent);
            }
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

    public static String getRelativeTimeSpan(String publishedAt) {
        if (publishedAt == null || publishedAt.trim().isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(publishedAt);
            if (date == null) return "";

            long timeMs = date.getTime();
            long nowMs = System.currentTimeMillis();
            long diffMs = nowMs - timeMs;

            if (diffMs < 0) {
                return "Just now";
            }

            long diffSec = diffMs / 1000;
            long diffMin = diffSec / 60;
            long diffHour = diffMin / 60;
            long diffDay = diffHour / 24;
            long diffWeek = diffDay / 7;
            long diffMonth = diffDay / 30;
            long diffYear = diffDay / 365;

            if (diffMin < 1) {
                return "Just now";
            } else if (diffMin < 60) {
                return diffMin + (diffMin == 1 ? " min ago" : " mins ago");
            } else if (diffHour < 24) {
                return diffHour + (diffHour == 1 ? " hour ago" : " hours ago");
            } else if (diffDay < 7) {
                return diffDay + (diffDay == 1 ? " day ago" : " days ago");
            } else if (diffWeek < 4) {
                return diffWeek + (diffWeek == 1 ? " week ago" : " weeks ago");
            } else if (diffMonth < 12) {
                return diffMonth + (diffMonth == 1 ? " month ago" : " months ago");
            } else {
                return diffYear + (diffYear == 1 ? " year ago" : " years ago");
            }
        } catch (Exception e) {
            try {
                SimpleDateFormat sdfFallback = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdfFallback.parse(publishedAt);
                if (date != null) {
                    long diffMs = System.currentTimeMillis() - date.getTime();
                    long diffDay = diffMs / (1000 * 60 * 60 * 24);
                    if (diffDay <= 0) return "Today";
                    if (diffDay < 7) return diffDay + (diffDay == 1 ? " day ago" : " days ago");
                    long diffWeek = diffDay / 7;
                    return diffWeek + (diffWeek == 1 ? " week ago" : " weeks ago");
                }
            } catch (Exception ignored) {}
            return "";
        }
    }

    public interface OnAudioClickListener {
        void onAudioClick(Article article);
    }

    private OnAudioClickListener audioClickListener;

    public void setOnAudioClickListener(OnAudioClickListener listener) {
        this.audioClickListener = listener;
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        androidx.cardview.widget.CardView cardView;
        TextView title, description, readMore, sourceTag, publishedTime, newsFooter;
        ImageView bookmarkIcon, newsImage, shareIcon, audioButton;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            title = itemView.findViewById(R.id.newsTitle);
            description = itemView.findViewById(R.id.newsDescription);
            readMore = itemView.findViewById(R.id.readMore);
            sourceTag = itemView.findViewById(R.id.sourceTag);
            publishedTime = itemView.findViewById(R.id.publishedTime);
            newsFooter = itemView.findViewById(R.id.newsFooter);
            bookmarkIcon = itemView.findViewById(R.id.bookmarkIcon);
            newsImage = itemView.findViewById(R.id.newsImage);
            shareIcon = itemView.findViewById(R.id.shareIcon);
            audioButton = itemView.findViewById(R.id.audioButton);
        }
    }
}
