package com.thunder.coolweatherbro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

public class LocationClass {

    private Context ctx;
    private static LocationClass location;

    private Location currentLocation;

    private LocationManager locationManager;

    public static LocationClass getInstance(Context ctx) {
        if (location != null) {
            return location;
        }
        return location = new LocationClass(ctx);
    }

    public LocationClass(Context ctx) {
        this.ctx = ctx;
        currentLocation = null;
    }

    public boolean checkForGPSPermission() {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void destroyLocationService() {
        locationManager.removeUpdates(listener);
    }

    public void getCurrentLocation() {

        if (checkForGPSPermission()) {
            locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

            LocationManager locationManager =
                    (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!gpsEnabled) {
                askToTurnOnGPS();
            }
            setupLocation();
        } else {
            ActivityCompat.requestPermissions((Activity) ctx, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

    }

    private Location requestUpdatesFromProvider(final String provider) {
        Location location = null;
        if (checkForGPSPermission()) {
            if (locationManager.isProviderEnabled(provider)) {

                locationManager.requestLocationUpdates(provider, 10000, 10, listener);

                for (String providerTemp : locationManager.getAllProviders()) {
                    location = locationManager.getLastKnownLocation(providerTemp);
                    if (location != null){
                        break;
                    }
                }
            } else {
                Toast.makeText(ctx, "Error", Toast.LENGTH_LONG).show();
            }
        }
        return location;
    }

    public double getLatFromCurrentLocation() {
        return currentLocation.getLatitude();
    }

    public double getLongFromCurrentLocation() {
        return currentLocation.getLongitude();
    }

    private final LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // A new location update is received.  Do something useful with it.  Update the UI with
            // the location update.
            currentLocation = location;
            Log.d("Location Changed", location.getLatitude() + " <-> " + location.getLongitude());
            Toast.makeText(ctx, "Location Changed: " + location.getLatitude() + " <-> " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (currentLocation == null) {
                currentLocation = new Location(provider);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (currentLocation == null) {
                currentLocation = new Location(provider);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (currentLocation == null) {
                currentLocation = new Location(provider);
            }
        }
    };

    public void setupLocation() {
        currentLocation = requestUpdatesFromProvider(LocationManager.GPS_PROVIDER);

        if (currentLocation != null) {
            Log.d("Location Changed", currentLocation.getLatitude() + " <-> " + currentLocation.getLongitude());
        }
    }

    public void askToTurnOnGPS() {
        AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
        alertDialog.setTitle("GPS Not Available");
        alertDialog.setMessage("Do you want to activate it?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        enableLocationSettings();
                    }
                });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        ctx.startActivity(settingsIntent);
    }
}
