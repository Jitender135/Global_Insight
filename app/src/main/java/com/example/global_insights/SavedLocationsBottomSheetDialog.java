package com.example.global_insights;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.global_insights.Adapter.SavedLocationsAdapter;
import com.example.global_insights.model.SavedLocation;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class SavedLocationsBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnLocationChangeListener {
        /**
         * Triggered when a location is selected.
         * @param location selected SavedLocation, or null for Live GPS mode
         */
        void onLocationChanged(@Nullable SavedLocation location);
        void onSaveCurrentSpotRequested();
        void onSearchCustomAreaRequested();
    }

    private OnLocationChangeListener listener;
    private SavedLocationsAdapter adapter;
    private RecyclerView rvSavedLocations;
    private TextView tvEmptySavedLocations;
    private TextView tvLiveGpsActiveBadge;
    private CardView cardLiveGpsOption;

    public static SavedLocationsBottomSheetDialog newInstance(OnLocationChangeListener listener) {
        SavedLocationsBottomSheetDialog dialog = new SavedLocationsBottomSheetDialog();
        dialog.listener = listener;
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_saved_locations, container, false);

        ImageView btnClose = view.findViewById(R.id.btnCloseBottomSheet);
        btnClose.setOnClickListener(v -> dismiss());

        cardLiveGpsOption = view.findViewById(R.id.cardLiveGpsOption);
        tvLiveGpsActiveBadge = view.findViewById(R.id.tvLiveGpsActiveBadge);
        rvSavedLocations = view.findViewById(R.id.rvSavedLocations);
        tvEmptySavedLocations = view.findViewById(R.id.tvEmptySavedLocations);
        TextView btnQuickSaveSpot = view.findViewById(R.id.btnQuickSaveSpot);
        View btnSearchCustomArea = view.findViewById(R.id.btnSearchCustomArea);

        rvSavedLocations.setLayoutManager(new LinearLayoutManager(getContext()));

        refreshUI();

        // Live GPS Card Clicked
        cardLiveGpsOption.setOnClickListener(v -> {
            if (getContext() != null) {
                SavedLocationManager.setActiveToLiveGps(getContext());
                if (listener != null) {
                    listener.onLocationChanged(null);
                }
                dismiss();
            }
        });

        // Quick Save Current Spot Clicked
        btnQuickSaveSpot.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onSaveCurrentSpotRequested();
            }
        });

        // Search Custom Area Clicked
        if (btnSearchCustomArea != null) {
            btnSearchCustomArea.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onSearchCustomAreaRequested();
                }
            });
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        }
    }

    private void refreshUI() {
        if (getContext() == null) return;

        boolean isLiveGps = SavedLocationManager.isLiveGpsActive(getContext());
        SavedLocation activeSaved = SavedLocationManager.getActiveSavedLocation(getContext());
        String activeId = activeSaved != null ? activeSaved.getId() : "";

        View layoutLiveGpsContainer = getView() != null ? getView().findViewById(R.id.layoutLiveGpsCardContainer) : null;
        if (isLiveGps) {
            tvLiveGpsActiveBadge.setVisibility(View.VISIBLE);
            if (layoutLiveGpsContainer != null) {
                layoutLiveGpsContainer.setBackgroundResource(R.drawable.bg_saved_location_card_active);
            }
        } else {
            tvLiveGpsActiveBadge.setVisibility(View.GONE);
            if (layoutLiveGpsContainer != null) {
                layoutLiveGpsContainer.setBackgroundResource(R.drawable.bg_saved_location_card_normal);
            }
        }

        List<SavedLocation> savedList = SavedLocationManager.getSavedLocations(getContext());
        if (savedList.isEmpty()) {
            tvEmptySavedLocations.setVisibility(View.VISIBLE);
            rvSavedLocations.setVisibility(View.GONE);
        } else {
            tvEmptySavedLocations.setVisibility(View.GONE);
            rvSavedLocations.setVisibility(View.VISIBLE);

            adapter = new SavedLocationsAdapter(getContext(), savedList, activeId, new SavedLocationsAdapter.OnLocationInteractionListener() {
                @Override
                public void onSelectLocation(SavedLocation location) {
                    SavedLocationManager.setActiveSavedLocation(getContext(), location);
                    if (listener != null) {
                        listener.onLocationChanged(location);
                    }
                    dismiss();
                }

                @Override
                public void onDeleteLocation(SavedLocation location) {
                    SavedLocationManager.deleteLocation(getContext(), location.getId());
                    Toast.makeText(getContext(), "Deleted " + location.getLabel(), Toast.LENGTH_SHORT).show();
                    refreshUI();
                }
            });
            rvSavedLocations.setAdapter(adapter);
        }
    }
}
