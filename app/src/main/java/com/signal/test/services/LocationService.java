package com.signal.test.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.util.Locale;

public class LocationService {
    private static final String TAG = "LocationService";
    private Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private boolean isIndoor = false;
    private int indoorLevel = 0;
    
    public LocationService(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        
        this.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocation(location);
            }
            
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // 检查室内位置信息
                if (extras != null && extras.containsKey("floor_level")) {
                    isIndoor = true;
                    indoorLevel = extras.getInt("floor_level", 0);
                }
            }
            
            @Override
            public void onProviderEnabled(String provider) {
            }
            
            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        
        // 初始化Fused Location回调
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        updateLocation(location);
                    }
                }
            }
        };
    }
    
    private void updateLocation(Location location) {
        if (location != null) {
            // 检查是否是更优的位置
            if (currentLocation == null || isBetterLocation(location, currentLocation)) {
                currentLocation = location;
                
                // 检查室内位置信息
                if (location.getExtras() != null) {
                    isIndoor = location.getExtras().getBoolean("indoor", false);
                    if (isIndoor) {
                        indoorLevel = location.getExtras().getInt("level", 0);
                    }
                }
            }
        }
    }
    
    // 开始定位
    public void startLocationUpdates() {
        // 检查权限
        if (!hasLocationPermission()) {
            return;
        }

        try {
            // 请求传统位置更新
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);
            
            // 请求Fused Location更新
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000
            ).setMinUpdateDistanceMeters(1)
             .setWaitForAccurateLocation(true)
             .build();
            
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    // 停止定位
    public void stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener);
            fusedLocationClient.removeLocationUpdates(locationCallback);
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
            
            // 融合多个位置源
            Location bestLocation = getBestLocation(gpsLocation, networkLocation);
            
            // 如果有Fused Location的位置，优先使用
            if (currentLocation != null) {
                bestLocation = getBestLocation(bestLocation, currentLocation);
            }
            
            return bestLocation;
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
    
    // 位置质量评估方法
    public LocationQuality assessLocationQuality() {
        Location location = getCurrentLocation();
        if (location == null) {
            return new LocationQuality(0, "无位置信息");
        }
        
        int qualityScore = 0;
        StringBuilder qualityDescription = new StringBuilder();
        
        // 基于精度评估
        float accuracy = location.getAccuracy();
        if (accuracy < 10) {
            qualityScore += 30;
            qualityDescription.append("高精度(");
        } else if (accuracy < 50) {
            qualityScore += 20;
            qualityDescription.append("中等精度(");
        } else {
            qualityScore += 10;
            qualityDescription.append("低精度(");
        }
        qualityDescription.append(String.format(Locale.CHINA, "%.1f米)", accuracy));
        
        // 基于提供者评估
        String provider = location.getProvider();
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            qualityScore += 30;
            qualityDescription.append(", GPS定位");
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
            qualityScore += 20;
            qualityDescription.append(", 网络定位");
        } else if ("fused".equals(provider)) {
            qualityScore += 25;
            qualityDescription.append(", 融合定位");
        }
        
        // 基于时间评估
        long timeDiff = System.currentTimeMillis() - location.getTime();
        if (timeDiff < 60000) { // 1分钟内
            qualityScore += 20;
            qualityDescription.append(", 最新位置");
        } else if (timeDiff < 300000) { // 5分钟内
            qualityScore += 15;
            qualityDescription.append(", 较新位置");
        } else {
            qualityScore += 5;
            qualityDescription.append(", 较旧位置");
        }
        
        // 基于速度和方向评估
        if (location.hasSpeed()) {
            qualityScore += 10;
            qualityDescription.append(", 含速度信息");
        }
        if (location.hasBearing()) {
            qualityScore += 5;
            qualityDescription.append(", 含方向信息");
        }
        
        // 室内定位评估
        if (isIndoor) {
            qualityScore += 10;
            qualityDescription.append(", 室内定位(").append(indoorLevel).append("层)");
        }
        
        return new LocationQuality(qualityScore, qualityDescription.toString());
    }
    
    // 优化的位置描述生成逻辑
    public String getLocationDescription() {
        Location location = getCurrentLocation();
        if (location != null) {
            StringBuilder description = new StringBuilder();
            
            // 添加室内位置信息
            if (isIndoor) {
                description.append("室内 ").append(indoorLevel).append("层, ");
            }
            
            // 添加坐标信息
            description.append("Lat: ").append(String.format(Locale.CHINA, "%.6f", location.getLatitude()));
            description.append(", Lng: ").append(String.format(Locale.CHINA, "%.6f", location.getLongitude()));
            
            // 添加精度信息
            if (location.hasAccuracy()) {
                description.append(", 精度: ").append(String.format(Locale.CHINA, "%.1f", location.getAccuracy())).append("米");
            }
            
            // 添加提供者信息
            description.append(", 来源: ").append(location.getProvider());
            
            return description.toString();
        }
        return "未知位置";
    }
    
    // 检查是否有有效的位置信息
    public boolean hasValidLocation() {
        Location location = getCurrentLocation();
        if (location == null) {
            return false;
        }
        
        // 检查位置时间戳，确保是最近的
        long timeDiff = System.currentTimeMillis() - location.getTime();
        return timeDiff < 300000; // 5分钟内的位置视为有效
    }
    
    // 检查是否在室内
    public boolean isIndoor() {
        return isIndoor;
    }
    
    // 获取室内楼层
    public int getIndoorLevel() {
        return indoorLevel;
    }
    
    // 位置质量评估类
    public static class LocationQuality {
        private int score;
        private String description;
        
        public LocationQuality(int score, String description) {
            this.score = score;
            this.description = description;
        }
        
        public int getScore() {
            return score;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getQualityLevel() {
            if (score >= 80) {
                return "优秀";
            } else if (score >= 60) {
                return "良好";
            } else if (score >= 40) {
                return "一般";
            } else {
                return "较差";
            }
        }
    }
    
    // 位置比较方法，判断哪个位置更好
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }
        
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 60000;
        boolean isSignificantlyOlder = timeDelta < -60000;
        boolean isNewer = timeDelta > 0;
        
        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }
        
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;
        
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());
        
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        
        return false;
    }
    
    // 检查两个位置是否来自同一提供者
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    // 获取多个位置中的最佳位置
    private Location getBestLocation(Location... locations) {
        Location bestLocation = null;
        for (Location location : locations) {
            if (location != null) {
                if (bestLocation == null || isBetterLocation(location, bestLocation)) {
                    bestLocation = location;
                }
            }
        }
        return bestLocation;
    }
}