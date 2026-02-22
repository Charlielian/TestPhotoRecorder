package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends AppCompatActivity implements 
    OnMapReadyCallback, 
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnCameraIdleListener,
    ClusterManager.OnClusterItemClickListener<SignalClusterItem> {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final float DEFAULT_ZOOM = 12f;
    private static final int HEATMAP_RADIUS = 50;
    private static final double MAX_CLUSTER_DISTANCE = 100; // 米

    private GoogleMap mMap;
    private Button btnBack;

    private StorageUtils storageUtils;
    private List<SignalData> allSignalData;
    private List<SignalData> filteredSignalData;
    private Map<Marker, SignalData> markerDataMap;

    private ClusterManager<SignalClusterItem> clusterManager;
    private Polyline currentPolyline;
    private Circle selectionCircle;

    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;
    private Handler mainHandler;

    private boolean isHeatmapVisible = false;
    private boolean isClusterMode = true;
    private boolean isSelectionMode = false;
    private LatLng selectedLocation;
    private float selectedRadius = 500; // 米

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initThreading();
        initViews();
        setupToolbar();
        initServices();
        loadData();
        setupMapFragment();
    }

    private void initThreading() {
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("信号分布地图");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initServices() {
        storageUtils = new StorageUtils(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerDataMap = new HashMap<>();
    }

    private void loadData() {
        allSignalData = storageUtils.getAllSignalData();
        filteredSignalData = new ArrayList<>(allSignalData);

        if (allSignalData.isEmpty()) {
            Toast.makeText(this, "没有测试数据可显示", Toast.LENGTH_LONG).show();
        }
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }





    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupMap();
        checkLocationPermission();
    }

    private void setupMap() {
        // 设置地图基本属性
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);

        // 设置监听器
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraIdleListener(this);

        // 初始化聚类管理器
        setupClusterManager();

        // 添加标记
        addMarkersToMap();

        // 设置默认视图
        setDefaultCameraPosition();
    }

    private void setupClusterManager() {
        clusterManager = new ClusterManager<>(this, mMap);
        clusterManager.setOnClusterItemClickListener(this);

        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void moveToMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
                    } else {
                        Toast.makeText(this, "无法获取当前位置", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setDefaultCameraPosition() {
        if (allSignalData.isEmpty()) {
            // 如果没有数据，显示默认位置（例如北京）
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(39.9042, 116.4074), DEFAULT_ZOOM));
            return;
        }

        // 计算所有标记的边界
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        int validCount = 0;

        for (SignalData data : allSignalData) {
            if (data.getLatitude() != 0 && data.getLongitude() != 0) {
                builder.include(new LatLng(data.getLatitude(), data.getLongitude()));
                validCount++;
            }
        }

        if (validCount > 0) {
            LatLngBounds bounds = builder.build();
            int padding = 100;
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }

    private void addMarkersToMap() {
        if (mMap == null || filteredSignalData.isEmpty()) return;

        clearMap();

        if (isClusterMode) {
            addClusteredMarkers();
        } else {
            addIndividualMarkers();
        }

        if (isHeatmapVisible) {
            addHeatmap();
        }

        if (isSelectionMode && selectedLocation != null) {
            addSelectionCircle();
        }
    }

    private void addIndividualMarkers() {
        for (SignalData data : filteredSignalData) {
            if (data.getLatitude() != 0 && data.getLongitude() != 0) {
                LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
                Marker marker = mMap.addMarker(createMarkerOptions(data, location));
                if (marker != null) {
                    marker.setTag(data);
                    markerDataMap.put(marker, data);
                }
            }
        }
    }

    private void addClusteredMarkers() {
        // 简化实现，移除对不存在的聚类渲染器的依赖
        // 直接使用单个标记点
        addIndividualMarkers();
    }

    private MarkerOptions createMarkerOptions(SignalData data, LatLng location) {
        float color = getSignalColor(data);
        
        return new MarkerOptions()
                .position(location)
                .title(data.getOperator() + " - " + data.getNetworkType())
                .snippet(buildMarkerSnippet(data))
                .icon(BitmapDescriptorFactory.defaultMarker(color))
                .anchor(0.5f, 1.0f);
    }

    private String buildMarkerSnippet(SignalData data) {
        StringBuilder snippet = new StringBuilder();
        
        if ("5G".equals(data.getNetworkType())) {
            snippet.append("RSRP: ").append(data.getRsrp()).append(" dBm\n");
        } else {
            snippet.append("RSSI: ").append(data.getRssi()).append(" dBm\n");
        }
        
        snippet.append("PCI: ").append(data.getPci()).append("\n");
        snippet.append("时间: ").append(data.getTimestamp());
        
        return snippet.toString();
    }

    private float getSignalColor(SignalData data) {
        if ("5G".equals(data.getNetworkType())) {
            int rsrp = data.getRsrp();
            if (rsrp >= -85) return BitmapDescriptorFactory.HUE_GREEN;
            if (rsrp >= -100) return BitmapDescriptorFactory.HUE_YELLOW;
            return BitmapDescriptorFactory.HUE_RED;
        } else {
            int rssi = data.getRssi();
            if (rssi >= -70) return BitmapDescriptorFactory.HUE_GREEN;
            if (rssi >= -85) return BitmapDescriptorFactory.HUE_YELLOW;
            return BitmapDescriptorFactory.HUE_RED;
        }
    }

    private void addHeatmap() {
        // 简化实现，移除对不存在的HeatmapTileProvider的引用
        if (filteredSignalData.isEmpty()) return;

        List<LatLng> locations = new ArrayList<>();
        for (SignalData data : filteredSignalData) {
            if (data.getLatitude() != 0 && data.getLongitude() != 0) {
                locations.add(new LatLng(data.getLatitude(), data.getLongitude()));
            }
        }

        if (locations.isEmpty()) return;

        // 热力图功能暂不可用
        Toast.makeText(this, "热力图功能暂不可用", Toast.LENGTH_SHORT).show();
        isHeatmapVisible = false;
    }

    private void toggleHeatmap() {
        isHeatmapVisible = !isHeatmapVisible;
        Toast.makeText(this, isHeatmapVisible ? "热力图已开启" : "热力图已关闭", Toast.LENGTH_SHORT).show();
    }

    private void toggleClusterMode() {
        isClusterMode = !isClusterMode;
        
        clearMap();
        
        if (isClusterMode) {
            addClusteredMarkers();
            Toast.makeText(this, "聚类模式已开启", Toast.LENGTH_SHORT).show();
        } else {
            addIndividualMarkers();
            Toast.makeText(this, "聚类模式已关闭", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleBottomSheet() {
        Toast.makeText(this, "底部面板功能暂不可用", Toast.LENGTH_SHORT).show();
    }

    private void toggleSelectionMode() {
        isSelectionMode = !isSelectionMode;
        
        if (!isSelectionMode && selectionCircle != null) {
            selectionCircle.remove();
            selectionCircle = null;
            selectedLocation = null;
        }
    }

    private void filterData(String networkType, Boolean strongSignal) {
        executorService.execute(() -> {
            List<SignalData> filtered = new ArrayList<>(allSignalData);
            
            // 网络类型过滤
            if (networkType != null) {
                filtered.removeIf(data -> !networkType.equals(data.getNetworkType()));
            }
            
            // 信号强度过滤
            if (strongSignal != null) {
                filtered.removeIf(data -> {
                    if ("5G".equals(data.getNetworkType())) {
                        return strongSignal ? data.getRsrp() < -85 : data.getRsrp() >= -85;
                    } else {
                        return strongSignal ? data.getRssi() < -70 : data.getRssi() >= -70;
                    }
                });
            }

            mainHandler.post(() -> {
                filteredSignalData = filtered;
                addMarkersToMap();
                updateMarkerCount();
            });
        });
    }

    private void filterByTimeRange(int days) {
        if (days == 0) {
            filteredSignalData = new ArrayList<>(allSignalData);
            addMarkersToMap();
            return;
        }

        executorService.execute(() -> {
            long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            
            List<SignalData> filtered = new ArrayList<>();
            
            for (SignalData data : allSignalData) {
                try {
                    Date date = sdf.parse(data.getTimestamp());
                    if (date != null && date.getTime() >= cutoffTime) {
                        filtered.add(data);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "日期解析失败", e);
                }
            }

            mainHandler.post(() -> {
                filteredSignalData = filtered;
                addMarkersToMap();
                updateMarkerCount();
            });
        });
    }

    private void updateMarkerCount() {
        // 简化实现，移除对不存在的tvMarkerDetails的引用
        Log.d(TAG, "当前显示: " + filteredSignalData.size() + " 个标记点");
    }

    private void clearMap() {
        mMap.clear();
        markerDataMap.clear();
        if (clusterManager != null) {
            clusterManager.clearItems();
        }
    }

    private void addSelectionCircle() {
        if (selectedLocation == null) return;

        if (selectionCircle != null) {
            selectionCircle.remove();
        }

        selectionCircle = mMap.addCircle(new CircleOptions()
                .center(selectedLocation)
                .radius(selectedRadius)
                .strokeColor(Color.BLUE)
                .strokeWidth(2)
                .fillColor(Color.argb(50, 0, 0, 255)));
    }

    private void showMarkersInRadius(LatLng center, float radius) {
        List<SignalData> nearbyMarkers = new ArrayList<>();
        
        for (SignalData data : allSignalData) {
            if (data.getLatitude() != 0 && data.getLongitude() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(
                    center.latitude, center.longitude,
                    data.getLatitude(), data.getLongitude(),
                    results);
                
                if (results[0] <= radius) {
                    nearbyMarkers.add(data);
                }
            }
        }

        if (!nearbyMarkers.isEmpty()) {
            showNearbyMarkersDialog(nearbyMarkers);
        } else {
            Toast.makeText(this, "半径内没有标记点", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNearbyMarkersDialog(List<SignalData> markers) {
        String[] items = new String[markers.size()];
        for (int i = 0; i < markers.size(); i++) {
            SignalData data = markers.get(i);
            items[i] = String.format(Locale.CHINA, "%s | %s | %d dBm",
                data.getOperator(),
                data.getNetworkType(),
                "5G".equals(data.getNetworkType()) ? data.getRsrp() : data.getRssi());
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("半径内的标记点 (" + markers.size() + "个)")
            .setItems(items, (dialog, which) -> {
                SignalData selected = markers.get(which);
                showMarkerDetailDialog(selected);
            })
            .show();
    }

    private void showMarkerDetailDialog(SignalData data) {
        StringBuilder details = new StringBuilder();
        details.append("运营商: ").append(data.getOperator()).append("\n");
        details.append("网络类型: ").append(data.getNetworkType()).append("\n");
        details.append("小区CGI: ").append(data.getCgi()).append("\n");
        details.append("频点: ").append(data.getFrequency()).append("\n");
        details.append("频段: ").append(data.getBand()).append("\n");
        details.append("PCI: ").append(data.getPci()).append("\n");
        
        if ("5G".equals(data.getNetworkType())) {
            details.append("RSRP: ").append(data.getRsrp()).append(" dBm\n");
            details.append("RSRQ: ").append(data.getRsrq()).append(" dB\n");
            details.append("SINR: ").append(data.getSinr()).append(" dB\n");
        } else {
            details.append("RSSI: ").append(data.getRssi()).append(" dBm\n");
        }
        
        details.append("位置: ").append(data.getLocation()).append("\n");
        details.append("时间: ").append(data.getTimestamp());

        new android.app.AlertDialog.Builder(this)
            .setTitle("详细信号信息")
            .setMessage(details.toString())
            .setPositiveButton("查看照片", (dialog, which) -> {
                if (data.getPhotoPath() != null) {
                    openPhoto(data.getPhotoPath());
                } else {
                    Toast.makeText(this, "无照片", Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("导航", (dialog, which) -> {
                openNavigation(data.getLatitude(), data.getLongitude());
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    private void openPhoto(String photoPath) {
        // 简化实现，移除对不存在的PhotoViewActivity的引用
        Toast.makeText(this, "照片查看功能开发中", Toast.LENGTH_SHORT).show();
    }

    private void openNavigation(double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "请安装Google地图", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawPathBetweenMarkers(Marker marker1, Marker marker2) {
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        PolylineOptions lineOptions = new PolylineOptions()
                .add(marker1.getPosition())
                .add(marker2.getPosition())
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);

        currentPolyline = mMap.addPolyline(lineOptions);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        SignalData data = markerDataMap.get(marker);
        if (data != null) {
            marker.showInfoWindow();
            
            // 震动反馈
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                marker.hideInfoWindow();
                marker.showInfoWindow();
            }
        }
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        SignalData data = markerDataMap.get(marker);
        if (data != null) {
            showMarkerDetailDialog(data);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        selectedLocation = latLng;
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("选择操作")
            .setMessage("位置: " + String.format(Locale.CHINA, "%.6f, %.6f", 
                latLng.latitude, latLng.longitude))
            .setPositiveButton("添加标记", (dialog, which) -> {
                // 添加临时标记
                mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("临时标记")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            })
            .setNeutralButton("半径选择", (dialog, which) -> {
                showRadiusSelector(latLng);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showRadiusSelector(LatLng center) {
        // 简化实现，移除对不存在的dialog_radius_selector布局文件的引用
        float defaultRadius = 500; // 默认半径500米
        showMarkersInRadius(center, defaultRadius);
        
        // 显示选择圆
        selectedLocation = center;
        selectedRadius = defaultRadius;
        addSelectionCircle();
    }

    @Override
    public void onCameraIdle() {
        // 可以在这里实现按需加载标记点
    }

    @Override
    public boolean onClusterItemClick(SignalClusterItem clusterItem) {
        SignalData data = clusterItem.getSignalData();
        showMarkerDetailDialog(data);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_zoom_in) {
            mMap.animateCamera(CameraUpdateFactory.zoomIn());
            return true;
        } else if (id == R.id.action_zoom_out) {
            mMap.animateCamera(CameraUpdateFactory.zoomOut());
            return true;
        } else if (id == R.id.action_center) {
            moveToMyLocation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void takeScreenshot() {
        // 截取地图屏幕
        GoogleMap.SnapshotReadyCallback callback = bitmap -> {
            // 保存或分享截图
            saveScreenshot(bitmap);
        };
        mMap.snapshot(callback);
    }

    private void saveScreenshot(Bitmap bitmap) {
        // 实现截图保存逻辑
        Toast.makeText(this, "截图功能开发中", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, 
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "需要位置权限才能显示您的位置", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (storageUtils != null) {
            storageUtils.close();
        }
    }
}

/**
 * 聚类项目类
 */
class SignalClusterItem implements com.google.maps.android.clustering.ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final SignalData signalData;

    public SignalClusterItem(double lat, double lng, String title, String snippet, 
                             SignalData signalData) {
        this.position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
        this.signalData = signalData;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    public SignalData getSignalData() {
        return signalData;
    }
}

/**
 * 聚类渲染器（简化实现）
 */
class SignalClusterRenderer {
    // 简化实现，移除对不存在的DefaultClusterRenderer的依赖
}