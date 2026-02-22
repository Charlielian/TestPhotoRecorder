package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.signal.test.services.SignalCollector;
import com.signal.test.services.LocationService;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.utils.ExportUtils;
import com.signal.test.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 100;
    private long UPDATE_INTERVAL = 2000; // 2秒更新一次
    private static final long LOCATION_TIMEOUT = 10000; // 10秒位置超时

    // UI组件
    private Toolbar toolbar;
    private ScrollView scrollView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CircularProgressIndicator progressIndicator;
    
    // 网络信息卡片
    private MaterialCardView cardNetworkInfo;
    private TextView tvNetworkType, tvSignalStrength;
    private TextView tvOperator, tvCgi, tvFrequency, tvBand, tvPci;
    private TextView tvRssi, tvSinr, tvRsrp, tvRsrq, tvTa;
    
    // 信号质量和稳定性卡片
    private MaterialCardView cardSignalQuality;
    private TextView tvSignalQuality, tvNetworkStability;
    private TextView tvQualityScore, tvStabilityScore;
    
    // 位置信息卡片
    private MaterialCardView cardLocationInfo;
    private TextView tvLocation, tvLatitude, tvLongitude, tvAccuracy, tvTimestamp;
    
    // 5G参数卡片
    private MaterialCardView card5gParams;
    private TextView tvNrCgi, tvNrFrequency, tvNrBand, tvNrPci, tvNrRsrp, tvNrRsrq, tvNrSinr;
    
    // 控制组件
    private Switch networkSwitch;
    private Spinner simCardSpinner;
    private Chip chipRefresh, chipSave;
    
    // 功能按钮
    private Button btnCamera, btnHistory, btnChart, btnBatchTest;
    private FloatingActionButton fabSettings, fabExport;
    
    // 状态标志
    private boolean show5GMode = false;
    private boolean isUpdating = false;
    private boolean locationAvailable = false;
    private int selectedSimId = -1;
    
    // 数据和服务
    private List<SubscriptionInfo> subscriptionInfoList;
    private SignalCollector signalCollector;
    private LocationService locationService;
    private StorageUtils storageUtils;
    private ExportUtils exportUtils;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    private Handler updateHandler;
    private Runnable updateRunnable;
    
    private SignalData currentSignalData;
    private SignalData previousSignalData; // 上一次的信号数据，用于检测波动
    private List<SignalData> signalHistory; // 信号历史数据，用于波动分析
    private Location currentLocation;
    private AtomicBoolean isRefreshing = new AtomicBoolean(false);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化信号历史列表
        signalHistory = new ArrayList<>();
        
        initThreading();
        initViews();
        setupToolbar();
        initServices();
        setupRefreshHandler();
        requestPermissions();
    }
    
    private void initThreading() {
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        updateHandler = new Handler(Looper.getMainLooper());
    }
    
    private void initViews() {
        // 顶部栏控件
        tvNetworkType = findViewById(R.id.network_type);
        tvSignalStrength = findViewById(R.id.signal_strength);
        networkSwitch = findViewById(R.id.network_switch);
        simCardSpinner = findViewById(R.id.sim_card_spinner);
        
        // 信号信息控件
        tvOperator = findViewById(R.id.tv_operator);
        tvCgi = findViewById(R.id.tv_cgi);
        tvFrequency = findViewById(R.id.tv_frequency);
        tvBand = findViewById(R.id.tv_band);
        tvPci = findViewById(R.id.tv_pci);
        tvRssi = findViewById(R.id.tv_rssi);
        tvSinr = findViewById(R.id.tv_sinr);
        tvLocation = findViewById(R.id.tv_location);
        tvTimestamp = findViewById(R.id.tv_timestamp);
        
        // 功能按钮
        btnCamera = findViewById(R.id.btn_camera);
        btnHistory = findViewById(R.id.btn_history);
        btnChart = findViewById(R.id.btn_chart);
        btnBatchTest = findViewById(R.id.btn_batch_test);
        
        // 设置点击监听器
        setupClickListeners();
    }
    
    private void setupToolbar() {
        // 简化实现，移除对不存在toolbar的引用
    }
    
    private void setupClickListeners() {
        // 功能按钮
        btnCamera.setOnClickListener(v -> {
            checkPermissionAndStart(Manifest.permission.CAMERA, CameraActivity.class);
        });
        
        btnHistory.setOnClickListener(v -> 
            startActivity(new Intent(this, HistoryActivity.class)));
        
        btnChart.setOnClickListener(v -> 
            startActivity(new Intent(this, ChartActivity.class)));
        
        btnBatchTest.setOnClickListener(v -> 
            startActivity(new Intent(this, BatchTestActivity.class)));
        
        // 网络类型切换
        networkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show5GMode = isChecked;
            updateDisplayData();
        });
        
        // SIM卡选择
        simCardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && subscriptionInfoList != null && position <= subscriptionInfoList.size()) {
                    SubscriptionInfo info = subscriptionInfoList.get(position - 1);
                    selectedSimId = info.getSubscriptionId();
                    refreshData();
                } else {
                    selectedSimId = -1;
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSimId = -1;
            }
        });
    }
    
    private void initServices() {
        signalCollector = new SignalCollector(this);
        locationService = new LocationService(this);
        storageUtils = new StorageUtils(this);
        exportUtils = new ExportUtils(this);
    }
    
    private void setupRefreshHandler() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isUpdating) {
                    refreshData();
                    updateHandler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };
    }
    
    private void requestPermissions() {
        String[] permissions = getRequiredPermissions();
        List<String> missingPermissions = new ArrayList<>();
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                missingPermissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            onPermissionsGranted();
        }
    }
    
    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        
        return permissions.toArray(new String[0]);
    }
    
    private void onPermissionsGranted() {
        startLocationService();
        initSimCardSpinner();
        startAutoUpdate();
    }
    
    @SuppressLint("MissingPermission")
    private void initSimCardSpinner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                SubscriptionManager subscriptionManager = 
                    (SubscriptionManager) getSystemService(SubscriptionManager.class);
                
                if (subscriptionManager != null) {
                    subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                    
                    if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
                        List<String> simCardList = new ArrayList<>();
                        simCardList.add("自动选择SIM卡");
                        
                        for (SubscriptionInfo info : subscriptionInfoList) {
                            String carrierName = info.getCarrierName() != null ? 
                                info.getCarrierName().toString() : "未知运营商";
                            simCardList.add(String.format(Locale.CHINA, 
                                "SIM %d: %s", info.getSimSlotIndex() + 1, carrierName));
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                            android.R.layout.simple_spinner_item, simCardList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        simCardSpinner.setAdapter(adapter);
                        
                        // 默认选择第一个SIM卡
                        if (subscriptionInfoList.size() > 0) {
                            simCardSpinner.setSelection(1);
                        }
                    } else {
                        // 无SIM卡
                        List<String> noSimList = new ArrayList<>();
                        noSimList.add("未检测到SIM卡");
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                            android.R.layout.simple_spinner_item, noSimList);
                        simCardSpinner.setAdapter(adapter);
                        simCardSpinner.setEnabled(false);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "无法访问SIM卡信息", e);
            }
        }
    }
    
    private void startLocationService() {
        try {
            locationService.startLocationUpdates();
            locationAvailable = true;
        } catch (SecurityException e) {
            Log.e(TAG, "位置服务启动失败", e);
            locationAvailable = false;
        }
    }
    
    private void startAutoUpdate() {
        isUpdating = true;
        updateHandler.post(updateRunnable);
    }
    
    private void stopAutoUpdate() {
        isUpdating = false;
        updateHandler.removeCallbacks(updateRunnable);
    }
    
    private void refreshData() {
        if (isRefreshing.get()) return;
        
        isRefreshing.set(true);
        showProgress(true);
        
        executorService.execute(() -> {
            try {
                // 获取信号数据
                SignalData signalData = signalCollector.getSignalData(selectedSimId);
                
                // 获取位置数据
                Location location = locationService.getCurrentLocation();
                
                mainHandler.post(() -> {
                    currentSignalData = signalData;
                    currentLocation = location;
                    updateDisplayData();
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                    isRefreshing.set(false);
                    
                    // 根据信号强度改变颜色
                    updateSignalStrengthColor(signalData);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "刷新数据失败", e);
                mainHandler.post(() -> {
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                    isRefreshing.set(false);
                    showError("获取信号数据失败");
                });
            }
        });
    }
    
    private void updateDisplayData() {
        if (currentSignalData == null) {
            showEmptyData();
            return;
        }
        
        // 更新时间戳
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            .format(new Date());
        tvTimestamp.setText(timestamp);
        
        // 更新网络类型和信号强度
        String networkType = currentSignalData.getNetworkType();
        tvNetworkType.setText(networkType != null ? networkType : "未知");
        
        // 更新运营商
        tvOperator.setText(currentSignalData.getOperator() != null ? 
            currentSignalData.getOperator() : "未知");
        
        // 根据网络类型显示不同参数
        if (show5GMode && "5G".equals(networkType)) {
            display5GParameters();
        } else {
            display4GParameters();
        }
        
        // 更新信号质量和稳定性信息
        updateSignalQualityDisplay();
        
        // 检测信号波动和异常
        detectSignalFluctuation();
        checkSignalAnomaly();
        
        // 更新位置信息
        updateLocationDisplay();
    }
    
    private void display4GParameters() {
        tvCgi.setText(currentSignalData.getCgi() != null ? 
            currentSignalData.getCgi() : "N/A");
        tvFrequency.setText(String.valueOf(currentSignalData.getFrequency()));
        tvBand.setText(currentSignalData.getBand() != null ? 
            currentSignalData.getBand() : "N/A");
        tvPci.setText(String.valueOf(currentSignalData.getPci()));
        
        // 信号强度
        int rssi = currentSignalData.getRssi();
        tvRssi.setText(rssi != 0 ? rssi + " dBm" : "N/A");
        tvSignalStrength.setText(rssi != 0 ? rssi + " dBm" : "N/A");
        
        // 5G相关参数（如果存在）
        tvRsrp.setText(currentSignalData.getRsrp() != 0 ? 
            currentSignalData.getRsrp() + " dBm" : "N/A");
        tvRsrq.setText(currentSignalData.getRsrq() != 0 ? 
            currentSignalData.getRsrq() + " dB" : "N/A");
        tvSinr.setText(currentSignalData.getSinr() != 0 ? 
            currentSignalData.getSinr() + " dB" : "N/A");
    }
    
    private void display5GParameters() {
        // 简化实现，移除对不存在控件的引用
    }
    
    private void updateLocationDisplay() {
        if (currentLocation != null) {
            tvLocation.setText(locationService.getLocationDescription());
        } else {
            tvLocation.setText("正在获取位置...");
        }
    }
    
    private void updateSignalStrengthColor(SignalData data) {
        int color;
        if (data != null) {
            if (show5GMode && "5G".equals(data.getNetworkType())) {
                int rsrp = data.getRsrp();
                if (rsrp >= -85) color = Color.GREEN;
                else if (rsrp >= -100) color = Color.YELLOW;
                else color = Color.RED;
            } else {
                int rssi = data.getRssi();
                if (rssi >= -70) color = Color.GREEN;
                else if (rssi >= -85) color = Color.YELLOW;
                else color = Color.RED;
            }
        } else {
            color = Color.GRAY;
        }
        
        tvSignalStrength.setTextColor(color);
    }
    
    // 更新信号质量和稳定性显示
    private void updateSignalQualityDisplay() {
        // 简化实现，移除对不存在控件的引用
    }
    
    // 根据信号质量设置颜色
    private void updateQualityColor(String quality, TextView textView) {
        if (quality == null) return;
        
        int color;
        switch (quality) {
            case "优秀":
                color = Color.GREEN;
                break;
            case "良好":
                color = Color.rgb(144, 238, 144); // 淡绿色
                break;
            case "一般":
                color = Color.YELLOW;
                break;
            case "较差":
                color = Color.rgb(255, 165, 0); // 橙色
                break;
            case "差":
                color = Color.RED;
                break;
            default:
                color = Color.GRAY;
        }
        textView.setTextColor(color);
    }
    
    // 根据网络稳定性设置颜色
    private void updateStabilityColor(String stability, TextView textView) {
        if (stability == null) return;
        
        int color;
        switch (stability) {
            case "稳定":
                color = Color.GREEN;
                break;
            case "基本稳定":
                color = Color.rgb(144, 238, 144); // 淡绿色
                break;
            case "轻度波动":
                color = Color.YELLOW;
                break;
            case "中度波动":
                color = Color.rgb(255, 165, 0); // 橙色
                break;
            case "严重波动":
                color = Color.RED;
                break;
            default:
                color = Color.GRAY;
        }
        textView.setTextColor(color);
    }
    
    // 检测信号波动
    private void detectSignalFluctuation() {
        if (currentSignalData == null || previousSignalData == null) {
            // 更新历史数据
            updateSignalHistory();
            return;
        }
        
        // 计算信号强度差异
        int currentStrength = 0;
        int previousStrength = 0;
        String networkType = currentSignalData.getNetworkType();
        
        if ("5G".equals(networkType) || "4G".equals(networkType)) {
            currentStrength = currentSignalData.getRsrp() != 0 ? currentSignalData.getRsrp() : currentSignalData.getRssi();
            previousStrength = previousSignalData.getRsrp() != 0 ? previousSignalData.getRsrp() : previousSignalData.getRssi();
        } else {
            currentStrength = currentSignalData.getRssi();
            previousStrength = previousSignalData.getRssi();
        }
        
        // 检查是否有显著波动
        if (currentStrength != 0 && previousStrength != 0) {
            int diff = Math.abs(currentStrength - previousStrength);
            
            // 如果波动超过阈值，显示提醒
            if (diff >= 15) {
                String message = "信号强度波动较大 (" + diff + " dBm)";
                showSignalWarning(message);
            }
        }
        
        // 更新历史数据
        updateSignalHistory();
    }
    
    // 检查信号异常并提醒
    private void checkSignalAnomaly() {
        if (currentSignalData == null) return;
        
        // 检查是否为异常信号
        if (currentSignalData.isAnomaly()) {
            String networkType = currentSignalData.getNetworkType();
            String message = "检测到异常信号";
            
            if ("5G".equals(networkType) || "4G".equals(networkType)) {
                int rsrp = currentSignalData.getRsrp();
                int rsrq = currentSignalData.getRsrq();
                
                if (rsrp != 0 && rsrp < -120) {
                    message = "检测到异常信号：RSRP过低 (" + rsrp + " dBm)";
                } else if (rsrq != 0 && rsrq < -15) {
                    message = "检测到异常信号：RSRQ过低 (" + rsrq + " dB)";
                }
            } else {
                int rssi = currentSignalData.getRssi();
                if (rssi != 0 && rssi < -100) {
                    message = "检测到异常信号：RSSI过低 (" + rssi + " dBm)";
                }
            }
            
            showSignalWarning(message);
        }
    }
    
    // 更新信号历史
    private void updateSignalHistory() {
        if (currentSignalData == null) return;
        
        // 更新上一次的信号数据
        previousSignalData = currentSignalData;
        
        // 添加到历史记录
        signalHistory.add(0, currentSignalData);
        
        // 保持历史记录大小
        if (signalHistory.size() > 10) {
            signalHistory.remove(signalHistory.size() - 1);
        }
    }
    
    // 显示信号警告
    private void showSignalWarning(String message) {
        Snackbar.make(scrollView, message, Snackbar.LENGTH_SHORT)
            .setAction("了解", v -> {
                // 可以添加更多信息或操作
            })
            .show();
    }
    
    private void showEmptyData() {
        tvNetworkType.setText("无信号");
        tvOperator.setText("未知");
        tvCgi.setText("N/A");
        tvFrequency.setText("0");
        tvBand.setText("N/A");
        tvPci.setText("0");
        tvRssi.setText("N/A");
        tvSinr.setText("N/A");
        tvRsrp.setText("N/A");
        tvRsrq.setText("N/A");
        tvTa.setText("0");
    }
    
    private void saveCurrentData() {
        if (currentSignalData == null) {
            Toast.makeText(this, "无数据可保存", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgress(true);
        
        executorService.execute(() -> {
            try {
                // 更新位置信息
                if (currentLocation != null) {
                    currentSignalData.setLatitude(currentLocation.getLatitude());
                    currentSignalData.setLongitude(currentLocation.getLongitude());
                    currentSignalData.setLocation(locationService.getLocationDescription());
                }
                
                currentSignalData.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                    Locale.CHINA).format(new Date()));
                
                long result = storageUtils.insertSignalData(currentSignalData);
                boolean success = result > 0;
                
                mainHandler.post(() -> {
                    showProgress(false);
                    if (success) {
                        Snackbar.make(scrollView, "数据保存成功", Snackbar.LENGTH_SHORT)
                            .setAction("查看", v -> 
                                startActivity(new Intent(MainActivity.this, HistoryActivity.class)))
                            .show();
                    } else {
                        Toast.makeText(MainActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "保存数据失败", e);
                mainHandler.post(() -> {
                    showProgress(false);
                    Toast.makeText(MainActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void exportCurrentData() {
        if (currentSignalData == null) {
            Toast.makeText(this, "无数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<SignalData> singleItemList = new ArrayList<>();
        singleItemList.add(currentSignalData);
        
        showProgress(true);
        
        executorService.execute(() -> {
            String fileName = "Signal_Single_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".csv";
            boolean success = exportUtils.exportToCSV(singleItemList, fileName);
            
            mainHandler.post(() -> {
                showProgress(false);
                if (success) {
                    Snackbar.make(scrollView, "导出成功", Snackbar.LENGTH_SHORT)
                        .show();
                } else {
                    Toast.makeText(MainActivity.this, "导出失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void openFile(String filePath) {
        // 实现文件打开逻辑
    }
    
    private void checkPermissionAndStart(String permission, Class<?> activityClass) {
        if (ContextCompat.checkSelfPermission(this, permission) 
                == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, activityClass));
        } else {
            ActivityCompat.requestPermissions(this, 
                new String[]{permission}, REQUEST_PERMISSIONS);
        }
    }
    
    private void showSettingsDialog() {
        String[] options = {"更新频率", "数据存储位置", "清除缓存", "关于"};
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("设置")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showUpdateFrequencyDialog();
                        break;
                    case 1:
                        showStorageLocationDialog();
                        break;
                    case 2:
                        clearCache();
                        break;
                    case 3:
                        showAboutDialog();
                        break;
                }
            })
            .show();
    }
    
    private void showUpdateFrequencyDialog() {
        String[] frequencies = {"1秒", "2秒", "5秒", "10秒", "手动"};
        int[] intervals = {1000, 2000, 5000, 10000, 0};
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("选择更新频率")
            .setItems(frequencies, (dialog, which) -> {
                stopAutoUpdate();
                if (intervals[which] > 0) {
                    UPDATE_INTERVAL = intervals[which]; // 注意：这里需要修改为可变
                    startAutoUpdate();
                    Toast.makeText(this, "已设置为" + frequencies[which], Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "已切换为手动更新", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }
    
    private void showStorageLocationDialog() {
        // 实现存储位置选择
    }
    
    private void clearCache() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("清除缓存")
            .setMessage("确定要清除所有缓存数据吗？")
            .setPositiveButton("清除", (dialog, which) -> {
                // 实现缓存清除逻辑
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("关于")
            .setMessage("信号测试工具 v1.0.0\n\n" +
                "一款专业的移动网络信号测试工具\n" +
                "支持4G/5G网络参数采集、分析、导出\n\n" +
                "© 2024 SignalTest")
            .setPositiveButton("确定", null)
            .show();
    }
    
    private void showProgress(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void showError(String message) {
        Snackbar.make(scrollView, message, Snackbar.LENGTH_LONG)
            .setAction("重试", v -> refreshData())
            .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_export) {
            exportCurrentData();
            return true;
        } else if (id == R.id.action_clear) {
            if (storageUtils.deleteAllData()) {
                Toast.makeText(this, "所有数据已清除", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "清除数据失败", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void shareCurrentData() {
        if (currentSignalData == null) {
            Toast.makeText(this, "无数据可分享", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String shareText = buildShareText();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "信号测试数据");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        startActivity(Intent.createChooser(shareIntent, "分享数据"));
    }
    
    private String buildShareText() {
        StringBuilder sb = new StringBuilder();
        sb.append("信号测试数据\n");
        sb.append("时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
            Locale.CHINA).format(new Date())).append("\n");
        sb.append("网络类型: ").append(tvNetworkType.getText()).append("\n");
        sb.append("运营商: ").append(tvOperator.getText()).append("\n");
        sb.append("信号强度: ").append(tvSignalStrength.getText()).append("\n");
        sb.append("位置: ").append(tvLocation.getText()).append("\n");
        sb.append("经纬度: ").append(tvLatitude.getText()).append(", ")
            .append(tvLongitude.getText()).append("\n");
        
        return sb.toString();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, 
                                           int[] grantResults) {
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
                onPermissionsGranted();
            } else {
                Toast.makeText(this, R.string.msg_permission_required, 
                    Toast.LENGTH_LONG).show();
                // 显示权限说明对话框
                showPermissionExplanationDialog();
            }
        }
    }
    
    private void showPermissionExplanationDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("需要权限")
            .setMessage("应用需要以下权限才能正常工作：\n\n" +
                "• 位置权限：获取基站位置信息\n" +
                "• 电话权限：读取网络信号参数\n" +
                "• 相机权限：拍摄带信号水印的照片\n\n" +
                "请在设置中授予所需权限。")
            .setPositiveButton("去设置", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            })
            .setNegativeButton("取消", (dialog, which) -> finish())
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (locationAvailable) {
            locationService.startLocationUpdates();
        }
        startAutoUpdate();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopAutoUpdate();
        locationService.stopLocationUpdates();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoUpdate();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (storageUtils != null) {
            storageUtils.close();
        }
        if (locationService != null) {
            locationService.stopLocationUpdates();
        }
    }
}