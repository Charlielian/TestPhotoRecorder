package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.R;

import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Button btnBack;
    private StorageUtils storageUtils;
    private List<SignalData> signalDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initViews();
        initServices();
        loadData();

        // 获取SupportMapFragment并设置回调
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void initServices() {
        storageUtils = new StorageUtils(this);
    }

    private void loadData() {
        signalDataList = storageUtils.getAllSignalData();
        if (signalDataList.isEmpty()) {
            Toast.makeText(this, "没有测试数据可显示", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 设置地图属性
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // 设置标记点击监听器
        mMap.setOnMarkerClickListener(this);

        // 在地图上添加标记
        addMarkers();
    }

    // 在地图上添加标记
    private void addMarkers() {
        if (signalDataList.isEmpty()) {
            return;
        }

        // 用于定位相机的中心点
        LatLng firstLocation = null;

        for (SignalData data : signalDataList) {
            double latitude = data.getLatitude();
            double longitude = data.getLongitude();

            // 检查位置数据是否有效
            if (latitude != 0 && longitude != 0) {
                LatLng location = new LatLng(latitude, longitude);

                // 保存第一个位置用于相机定位
                if (firstLocation == null) {
                    firstLocation = location;
                }

                // 创建标记选项
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .title(data.getOperator() + " - " + data.getNetworkType())
                        .snippet("信号强度: " + data.getRssi() + " dBm\n" +
                                "时间: " + data.getTimestamp())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

                // 添加标记
                Marker marker = mMap.addMarker(markerOptions);
                
                // 存储数据对象到标记的标签中，以便点击时获取详细信息
                if (marker != null) {
                    marker.setTag(data);
                }
            }
        }

        // 定位相机到第一个标记位置
        if (firstLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 15f));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // 获取标记关联的数据
        SignalData data = (SignalData) marker.getTag();
        if (data != null) {
            // 显示详细信息
            Toast.makeText(this, 
                    "运营商: " + data.getOperator() + "\n" +
                    "网络类型: " + data.getNetworkType() + "\n" +
                    "小区CGI: " + data.getCgi() + "\n" +
                    "信号强度: " + data.getRssi() + " dBm\n" +
                    "时间: " + data.getTimestamp(), 
                    Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storageUtils != null) {
            storageUtils.close();
        }
    }
}
