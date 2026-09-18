package com.example.global_insights.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.global_insights.R;
import com.example.global_insights.model.SavedLocation;

import java.util.List;
import java.util.Locale;

public class SavedLocationsAdapter extends RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder> {

    public interface OnLocationInteractionListener {
        void onSelectLocation(SavedLocation location);
        void onDeleteLocation(SavedLocation location);
    }

    private Context context;
    private List<SavedLocation> locations;
    private String activeLocationId;
    private OnLocationInteractionListener listener;

    public SavedLocationsAdapter(Context context, List<SavedLocation> locations, String activeLocationId, OnLocationInteractionListener listener) {
        this.context = context;
        this.locations = locations;
        this.activeLocationId = activeLocationId;
        this.listener = listener;
    }

    public void updateList(List<SavedLocation> newLocations, String activeId) {
        this.locations = newLocations;
        this.activeLocationId = activeId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedLocation loc = locations.get(position);

        holder.tvIcon.setText(loc.getTagIcon());
        holder.tvLabel.setText(loc.getLabel());
        holder.tvAddress.setText(loc.getAddressLine());

        // Distance badge
        double dist = loc.getDistanceFromCurrentKm();
        if (dist >= 0) {
            holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km away", dist));
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // Active indicator
        boolean isActive = activeLocationId != null && activeLocationId.equals(loc.getId());
        if (isActive) {
            if (holder.layoutContainer != null) {
                holder.layoutContainer.setBackgroundResource(R.drawable.bg_saved_location_card_active);
            }
            if (holder.ivActive != null) {
                holder.ivActive.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.layoutContainer != null) {
                holder.layoutContainer.setBackgroundResource(R.drawable.bg_saved_location_card_normal);
            }
            if (holder.ivActive != null) {
                holder.ivActive.setVisibility(View.GONE);
            }
        }

        holder.card.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelectLocation(loc);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteLocation(loc);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations != null ? locations.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        View layoutContainer;
        TextView tvIcon, tvLabel, tvAddress, tvDistance;
        ImageView btnDelete, ivActive;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardSavedLocation);
            layoutContainer = itemView.findViewById(R.id.layoutSavedLocationItemContainer);
            tvIcon = itemView.findViewById(R.id.tvLocationIcon);
            tvLabel = itemView.findViewById(R.id.tvLocationLabel);
            tvAddress = itemView.findViewById(R.id.tvLocationAddress);
            tvDistance = itemView.findViewById(R.id.tvDistanceBadge);
            btnDelete = itemView.findViewById(R.id.btnDeleteSavedLocation);
            ivActive = itemView.findViewById(R.id.ivActiveIndicator);
        }
    }
}
