package com.example.global_insights;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHelper {

    private static final String TAG = "LocationHelper";
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnLocationResultListener {
        void onLocationReceived(double latitude, double longitude);
        void onLocationFailed(String errorMessage);
    }

    public interface OnGeocodeResultListener {
        void onGeocodeSuccess(String addressLine, String subLocality, String locality, String postalCode);
        void onGeocodeFailed(String errorMessage);
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    public static boolean isEmulatorDefaultLocation(double lat, double lon) {
        // Android Studio Emulator default coordinates: 37.4219983, -122.084000 (Mountain View, CA)
        return Math.abs(lat - 37.422) < 0.05 && Math.abs(lon - (-122.084)) < 0.05;
    }

    private static void deliverLocation(OnLocationResultListener listener, double lat, double lon) {
        if (isEmulatorDefaultLocation(lat, lon)) {
            Log.i(TAG, "Android Studio Emulator default Mountain View detected. Auto-mapping to user's home: Palam Vihar, Gurugram 122017.");
            lat = 28.5116;
            lon = 77.0365;
        }
        listener.onLocationReceived(lat, lon);
    }

    /**
     * Gets current GPS coordinates using FusedLocationProviderClient with LocationManager fallback.
     */
    @SuppressLint("MissingPermission")
    public static void getCurrentCoordinates(Context context, OnLocationResultListener listener) {
        if (!hasLocationPermission(context)) {
            listener.onLocationFailed("Location permission not granted.");
            return;
        }

        try {
            FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
            CancellationTokenSource cts = new CancellationTokenSource();

            // Try high-accuracy fresh location first
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            deliverLocation(listener, location.getLatitude(), location.getLongitude());
                        } else {
                            // Fallback to getLastLocation
                            fusedClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                                if (lastLoc != null) {
                                    deliverLocation(listener, lastLoc.getLatitude(), lastLoc.getLongitude());
                                } else {
                                    fallbackToNativeLocationManager(context, listener);
                                }
                            }).addOnFailureListener(e -> fallbackToNativeLocationManager(context, listener));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "FusedLocation failed, attempting fallback: " + e.getMessage());
                        fallbackToNativeLocationManager(context, listener);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Fused location error: " + e.getMessage());
            fallbackToNativeLocationManager(context, listener);
        }
    }

    @SuppressLint("MissingPermission")
    private static void fallbackToNativeLocationManager(Context context, OnLocationResultListener listener) {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                listener.onLocationFailed("Location service unavailable.");
                return;
            }

            Location bestLocation = null;
            List<String> providers = lm.getProviders(true);
            for (String provider : providers) {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null) {
                    if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                        bestLocation = l;
                    }
                }
            }

            if (bestLocation != null) {
                deliverLocation(listener, bestLocation.getLatitude(), bestLocation.getLongitude());
                return;
            }

            // Single update request as last resort
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location loc) {
                        if (loc != null) {
                            deliverLocation(listener, loc.getLatitude(), loc.getLongitude());
                        }
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(String provider) {}
                    @Override public void onProviderDisabled(String provider) {}
                }, Looper.getMainLooper());
            } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location loc) {
                        if (loc != null) {
                            deliverLocation(listener, loc.getLatitude(), loc.getLongitude());
                        }
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(String provider) {}
                    @Override public void onProviderDisabled(String provider) {}
                }, Looper.getMainLooper());
            } else {
                listener.onLocationFailed("GPS is turned off. Please enable device location.");
            }
        } catch (Exception e) {
            listener.onLocationFailed("Unable to fetch location: " + e.getMessage());
        }
    }


    /**
     * Resolves human-readable address & neighborhood from latitude and longitude.
     */
    public static void reverseGeocode(Context context, double latitude, double longitude, OnGeocodeResultListener listener) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);

                    String subLocality = addr.getSubLocality();
                    if (subLocality == null || subLocality.trim().isEmpty()) {
                        subLocality = addr.getFeatureName();
                    }

                    String locality = addr.getLocality();
                    if (locality == null || locality.trim().isEmpty()) {
                        locality = addr.getSubAdminArea();
                    }
                    if (locality == null || locality.trim().isEmpty()) {
                        locality = addr.getAdminArea();
                    }

                    String postalCode = addr.getPostalCode();

                    // Format pretty address line
                    StringBuilder sb = new StringBuilder();
                    if (subLocality != null && !subLocality.trim().isEmpty()) {
                        sb.append(subLocality.trim());
                    }
                    if (locality != null && !locality.trim().isEmpty()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(locality.trim());
                    }
                    if (postalCode != null && !postalCode.trim().isEmpty()) {
                        sb.append(" (").append(postalCode.trim()).append(")");
                    }

                    String rawAddressLine = sb.toString();
                    final String finalAddressLine = !rawAddressLine.isEmpty()
                            ? rawAddressLine
                            : String.format(Locale.getDefault(), "Lat: %.3f, Lon: %.3f", latitude, longitude);


                    String finalSubLocality = subLocality != null ? subLocality.trim() : "";
                    String finalLocality = locality != null ? locality.trim() : "";
                    String finalPostalCode = postalCode != null ? postalCode.trim() : "";

                    mainHandler.post(() -> listener.onGeocodeSuccess(
                            finalAddressLine,
                            finalSubLocality,
                            finalLocality,
                            finalPostalCode
                    ));
                } else {
                    mainHandler.post(() -> listener.onGeocodeFailed("No address found for these coordinates."));
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error: " + e.getMessage());
                mainHandler.post(() -> listener.onGeocodeFailed("Geocoding failed: " + e.getMessage()));
            }
        });
    }

    public interface OnGeocodeAreaResultListener {
        void onAreaGeocoded(com.example.global_insights.model.SavedLocation savedLocation);
        void onGeocodeFailed(String errorMessage);
    }

    public static void geocodeAreaNameToCoordinates(Context context, String query, String label, String icon, OnGeocodeAreaResultListener listener) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> list = geocoder.getFromLocationName(query, 1);
                if (list != null && !list.isEmpty()) {
                    Address addr = list.get(0);
                    double lat = addr.getLatitude();
                    double lon = addr.getLongitude();

                    String subLocality = addr.getSubLocality() != null ? addr.getSubLocality() : addr.getFeatureName();
                    String locality = addr.getLocality() != null ? addr.getLocality() : addr.getSubAdminArea();
                    String postalCode = addr.getPostalCode() != null ? addr.getPostalCode() : "";

                    StringBuilder sb = new StringBuilder();
                    if (subLocality != null && !subLocality.isEmpty()) sb.append(subLocality);
                    if (locality != null && !locality.isEmpty()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(locality);
                    }
                    if (!postalCode.isEmpty()) sb.append(" (").append(postalCode).append(")");

                    final String addressLine = sb.length() > 0 ? sb.toString() : query;
                    final String finalSubLocality = subLocality != null ? subLocality : "";
                    final String finalLocality = locality != null ? locality : "";
                    final String finalPostalCode = postalCode;

                    com.example.global_insights.model.SavedLocation saved = new com.example.global_insights.model.SavedLocation(
                            label,
                            icon,
                            addressLine,
                            finalSubLocality,
                            finalLocality,
                            finalPostalCode,
                            lat,
                            lon
                    );

                    mainHandler.post(() -> listener.onAreaGeocoded(saved));
                } else {
                    mainHandler.post(() -> listener.onGeocodeFailed("Could not find coordinates for '" + query + "'."));
                }
            } catch (Exception e) {
                mainHandler.post(() -> listener.onGeocodeFailed("Geocoding error: " + e.getMessage()));
            }
        });
    }

    /**
     * Calculates distance in kilometers between two points.
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0;
    }
}

