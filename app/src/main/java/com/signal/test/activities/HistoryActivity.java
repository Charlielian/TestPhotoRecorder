package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.Toast;

import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.utils.ExportUtils;
import com.signal.test.adapters.HistoryAdapter;
import com.signal.test.R;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button btnBack, btnExport;
    private LinearLayout emptyState;
    
    private HistoryAdapter adapter;
    private StorageUtils storageUtils;
    private ExportUtils exportUtils;
    private List<SignalData> signalDataList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        initViews();
        initServices();
        loadHistoryData();
        setupRecyclerView();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        btnBack = findViewById(R.id.btn_back);
        btnExport = findViewById(R.id.btn_export);
        emptyState = findViewById(R.id.empty_state);
        
        btnBack.setOnClickListener(v -> finish());
        btnExport.setOnClickListener(v -> showExportOptions());
    }
    
    private void initServices() {
        storageUtils = new StorageUtils(this);
        exportUtils = new ExportUtils(this);
    }
    
    private void loadHistoryData() {
        signalDataList = storageUtils.getAllSignalData();
        
        if (signalDataList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void setupRecyclerView() {
        adapter = new HistoryAdapter(this, signalDataList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData();
        setupRecyclerView();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        storageUtils.close();
    }
    
    // 显示导出选项对话框
    private void showExportOptions() {
        if (signalDataList.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 直接导出为CSV格式
        String fileName = exportUtils.getDefaultFileName();
        exportUtils.exportToCSV(signalDataList, fileName);
    }
}