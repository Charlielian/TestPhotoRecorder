package com.signal.test.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;

public class LocationService {
    private Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentLocation;
    
    public LocationService(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        
        this.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = location;
            }
            
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            
            @Override
            public void onProviderEnabled(String provider) {
            }
            
            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }
    
    // 开始定位
    public void startLocationUpdates() {
        // 检查权限
        if (!hasLocationPermission()) {
            return;
        }

        try {
            // 请求位置更新
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    // 停止定位
    public void stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    // 获取当前位置
    public Location getCurrentLocation() {
        // 检查权限
        if (!hasLocationPermission()) {
            return null;
        }

        try {
            // 尝试获取最后已知位置
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (gpsLocation != null) {
                return gpsLocation;
            } else if (networkLocation != null) {
                return networkLocation;
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 检查位置权限
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    // 获取位置描述
    public String getLocationDescription() {
        Location location = getCurrentLocation();
        if (location != null) {
            return "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
        }
        return "未知位置";
    }
}