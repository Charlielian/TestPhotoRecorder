package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import java.util.List;

import com.signal.test.services.SignalCollector;
import com.signal.test.services.LocationService;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    
    private TextView tvNetworkType, tvSignalStrength;
    private TextView tvOperator, tvCgi, tvFrequency, tvBand, tvPci, tvRssi, tvSinr, tvLocation, tvTimestamp;
    // 5G参数控件
    private TextView tvNrCgi, tvNrFrequency, tvNrBand, tvRsrp, tvNrPci, tvRsrq;
    private LinearLayout layout5gParams;
    private Switch networkSwitch;
    private Spinner simCardSpinner;
    private Button btnCamera, btnHistory, btnChart, btnBatchTest;
    
    private boolean show5GMode = false;
    private int selectedSimId = -1;
    private List<SubscriptionInfo> subscriptionInfoList;
    
    private SignalCollector signalCollector;
    private LocationService locationService;
    private StorageUtils storageUtils;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initServices();
        requestPermissions();
        updateSignalInfo();
    }
    
    private void initViews() {
        tvNetworkType = findViewById(R.id.network_type);
        tvSignalStrength = findViewById(R.id.signal_strength);
        tvOperator = findViewById(R.id.tv_operator);
        tvCgi = findViewById(R.id.tv_cgi);
        tvFrequency = findViewById(R.id.tv_frequency);
        tvBand = findViewById(R.id.tv_band);
        tvPci = findViewById(R.id.tv_pci);
        tvRssi = findViewById(R.id.tv_rssi);
        tvSinr = findViewById(R.id.tv_sinr);
        tvLocation = findViewById(R.id.tv_location);
        tvTimestamp = findViewById(R.id.tv_timestamp);
        
        // 初始化5G参数控件（如果布局中存在）
        // 注意：这些 View 在当前布局中不存在，暂时注释掉
        // layout5gParams = findViewById(R.id._5g_params_container);
        // tvNrCgi = findViewById(R.id.tv_nr_cgi);
        // tvNrFrequency = findViewById(R.id.tv_nr_frequency);
        // tvNrBand = findViewById(R.id.tv_nr_band);
        // tvRsrp = findViewById(R.id.tv_rsrp);
        // tvNrPci = findViewById(R.id.tv_nr_pci);
        // tvRsrq = findViewById(R.id.tv_rsrq);
        
        btnCamera = findViewById(R.id.btn_camera);
        btnHistory = findViewById(R.id.btn_history);
        btnChart = findViewById(R.id.btn_chart);
        btnBatchTest = findViewById(R.id.btn_batch_test);
        networkSwitch = findViewById(R.id.network_switch);
        simCardSpinner = findViewById(R.id.sim_card_spinner);
        
        btnCamera.setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        btnChart.setOnClickListener(v -> startActivity(new Intent(this, ChartActivity.class)));
        btnBatchTest.setOnClickListener(v -> startActivity(new Intent(this, BatchTestActivity.class)));
        
        // 网络类型切换监听器
        networkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show5GMode = isChecked;
            updateSignalInfo();
        });
        
        // SIM卡选择监听器
        simCardSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && subscriptionInfoList != null && position <= subscriptionInfoList.size()) {
                    SubscriptionInfo info = subscriptionInfoList.get(position - 1);
                    selectedSimId = info.getSubscriptionId();
                } else {
                    selectedSimId = -1;
                }
                updateSignalInfo();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedSimId = -1;
                updateSignalInfo();
            }
        });
        
        // 初始化SIM卡选择器
        initSimCardSpinner();
    }
    
    // 初始化SIM卡选择器
    private void initSimCardSpinner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(SubscriptionManager.class);
            if (subscriptionManager != null) {
                if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                    if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
                        // 创建SIM卡选择器的数据源
                        java.util.ArrayList<String> simCardList = new java.util.ArrayList<>();
                        simCardList.add("请选择SIM卡");
                        for (SubscriptionInfo info : subscriptionInfoList) {
                            simCardList.add("SIM " + info.getSimSlotIndex() + ": " + info.getCarrierName());
                        }
                        
                        // 创建适配器
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, simCardList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        simCardSpinner.setAdapter(adapter);
                        
                        // 默认选择第一个SIM卡
                        if (subscriptionInfoList.size() > 0) {
                            simCardSpinner.setSelection(1);
                        }
                    }
                }
            }
        }
    }
    
    private void initServices() {
        signalCollector = new SignalCollector(this);
        locationService = new LocationService(this);
        storageUtils = new StorageUtils(this);
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
            };
            
            requestPermissions(permissions, REQUEST_PERMISSIONS);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                locationService.startLocationUpdates();
                updateSignalInfo();
            } else {
                Toast.makeText(this, R.string.msg_permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void updateSignalInfo() {
        SignalData data = signalCollector.getSignalData(selectedSimId);

        // 添加空值检查
        if (data == null) {
            return;
        }

        if (show5GMode) {
            // 显示5G信息在主界面
            tvNetworkType.setText("5G");
            tvSignalStrength.setText(data.getRsrp() != 0 ? data.getRsrp() + " dBm" : "N/A");
            tvOperator.setText(data.getOperator() != null ? data.getOperator() : "未知");
            tvCgi.setText(data.getNrCgi() != null ? data.getNrCgi() : "N/A");
            tvFrequency.setText(data.getNrFrequency() != 0 ? String.valueOf(data.getNrFrequency()) : "N/A");
            tvBand.setText(data.getNrBand() != null ? data.getNrBand() : "N/A");
            tvPci.setText(data.getPci() != 0 ? String.valueOf(data.getPci()) : "N/A");
            tvRssi.setText(data.getRsrp() != 0 ? data.getRsrp() + " dBm" : "N/A");
            tvSinr.setText(data.getSinr() != 0 ? data.getSinr() + " dB" : "N/A");
            tvLocation.setText(locationService.getLocationDescription());
            tvTimestamp.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            // 隐藏5G参数容器（如果存在）
            if (layout5gParams != null) {
                layout5gParams.setVisibility(View.GONE);
            }
        } else {
            // 显示4G信息在主界面
            tvNetworkType.setText(data.getNetworkType() != null ? data.getNetworkType() : "未知");
            tvSignalStrength.setText(data.getRssi() + " dBm");
            tvOperator.setText(data.getOperator() != null ? data.getOperator() : "未知");
            tvCgi.setText(data.getCgi() != null ? data.getCgi() : "N/A");
            tvFrequency.setText(String.valueOf(data.getFrequency()));
            tvBand.setText(data.getBand() != null ? data.getBand() : "N/A");
            tvPci.setText(data.getPci() != 0 ? String.valueOf(data.getPci()) : "N/A");
            tvRssi.setText(data.getRssi() + " dBm");
            tvSinr.setText(data.getSinr() != 0 ? data.getSinr() + " dB" : "N/A");
            tvLocation.setText(locationService.getLocationDescription());
            tvTimestamp.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            // 隐藏5G参数容器（如果存在）
            if (layout5gParams != null) {
                layout5gParams.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        locationService.startLocationUpdates();
        updateSignalInfo();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        locationService.stopLocationUpdates();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        storageUtils.close();
    }
}