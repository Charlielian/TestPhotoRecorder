package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.utils.ExportUtils;
import com.signal.test.adapters.HistoryAdapter;
import com.signal.test.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnItemClickListener {

    private static final String TAG = "HistoryActivity";
    private static final int BATCH_SIZE = 20; // 批量加载大小
    private static final int EXPORT_PROGRESS_MAX = 100;

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private Button btnBack, btnExport;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearProgressIndicator progressIndicator;
    private FloatingActionButton fabDeleteAll;
    private ChipGroup filterChipGroup;
    private Chip chipAll, chip4G, chip5G, chipToday, chipWeek;
    private SearchView searchView;
    private TextView tvTotalCount, tvSelectedCount;

    private HistoryAdapter adapter;
    private StorageUtils storageUtils;
    private ExportUtils exportUtils;
    private List<SignalData> allSignalData;
    private List<SignalData> filteredSignalData;
    private List<SignalData> selectedItems;

    private ExecutorService executorService;
    private Handler mainHandler;

    // 排序方式
    private SortOrder currentSortOrder = SortOrder.DATE_DESC;
    // 过滤条件
    private FilterType currentFilter = FilterType.ALL;
    private String searchQuery = "";

    private boolean isSelectionMode = false;
    private boolean isLoadingMore = false;
    private boolean hasMoreData = true;
    private int currentPage = 0;

    private enum SortOrder {
        DATE_DESC, DATE_ASC, RSSI_DESC, RSSI_ASC, OPERATOR_ASC
    }

    private enum FilterType {
        ALL, NETWORK_4G, NETWORK_5G, TODAY, WEEK
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initThreading();
        initViews();
        initServices();
        setupRecyclerView();
        loadHistoryDataAsync();
    }

    private void initThreading() {
        executorService = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        btnBack = findViewById(R.id.btn_back);
        btnExport = findViewById(R.id.btn_export);
        emptyState = findViewById(R.id.empty_state);

        btnBack.setOnClickListener(v -> finish());
        btnExport.setOnClickListener(v -> showExportOptions());

        selectedItems = new ArrayList<>();
    }

    private void initServices() {
        storageUtils = new StorageUtils(this);
        exportUtils = new ExportUtils(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("历史记录");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        
        // 添加分隔线
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(divider);

        adapter = new HistoryAdapter(this, new ArrayList<>());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        // 添加滑动删除功能
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // 添加加载更多监听
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoadingMore && hasMoreData) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadMoreData();
                        }
                    }
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
            getResources().getColor(R.color.primary),
            getResources().getColor(R.color.accent),
            getResources().getColor(R.color.primary_dark)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void setupFilterChips() {
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = FilterType.ALL;
                applyFilters();
            }
        });

        chip4G.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = FilterType.NETWORK_4G;
                applyFilters();
            }
        });

        chip5G.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = FilterType.NETWORK_5G;
                applyFilters();
            }
        });

        chipToday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = FilterType.TODAY;
                applyFilters();
            }
        });

        chipWeek.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = FilterType.WEEK;
                applyFilters();
            }
        });

        // 默认选中全部
        chipAll.setChecked(true);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                applyFilters();
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            searchQuery = "";
            applyFilters();
            return false;
        });
    }

    /**
     * 异步加载历史数据
     */
    private void loadHistoryDataAsync() {
        showLoading(true);
        
        executorService.execute(() -> {
            try {
                allSignalData = storageUtils.getAllSignalData();
                mainHandler.post(() -> {
                    showLoading(false);
                    processLoadedData();
                });
            } catch (Exception e) {
                Log.e(TAG, "加载数据失败", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    showError("加载数据失败");
                });
            }
        });
    }

    /**
     * 处理加载的数据
     */
    private void processLoadedData() {
        if (allSignalData == null || allSignalData.isEmpty()) {
            showEmptyState(true, "暂无历史数据\n点击下方按钮开始测试");
            btnExport.setEnabled(false);
            fabDeleteAll.hide();
            return;
        }

        showEmptyState(false, null);
        btnExport.setEnabled(true);
        fabDeleteAll.show();
        
        tvTotalCount.setText(String.format(Locale.CHINA, "总数: %d", allSignalData.size()));
        
        // 初始化过滤后的数据
        filteredSignalData = new ArrayList<>(allSignalData);
        applyFilters();
    }

    /**
     * 应用过滤条件
     */
    private void applyFilters() {
        if (allSignalData == null) return;

        executorService.execute(() -> {
            List<SignalData> filtered = new ArrayList<>(allSignalData);
            
            // 应用网络类型过滤
            if (currentFilter == FilterType.NETWORK_4G) {
                filtered.removeIf(data -> !"4G".equals(data.getNetworkType()));
            } else if (currentFilter == FilterType.NETWORK_5G) {
                filtered.removeIf(data -> !"5G".equals(data.getNetworkType()));
            } else if (currentFilter == FilterType.TODAY) {
                filtered.removeIf(data -> !isToday(data.getTimestamp()));
            } else if (currentFilter == FilterType.WEEK) {
                filtered.removeIf(data -> !isThisWeek(data.getTimestamp()));
            }

            // 应用搜索过滤
            if (!searchQuery.isEmpty()) {
                filtered.removeIf(data -> 
                    (data.getOperator() == null || !data.getOperator().toLowerCase().contains(searchQuery.toLowerCase())) &&
                    (data.getCgi() == null || !data.getCgi().toLowerCase().contains(searchQuery.toLowerCase())) &&
                    (data.getLocation() == null || !data.getLocation().toLowerCase().contains(searchQuery.toLowerCase()))
                );
            }

            // 应用排序
            applySorting(filtered);

            mainHandler.post(() -> {
                filteredSignalData = filtered;
                currentPage = 0;
                hasMoreData = filtered.size() > BATCH_SIZE;
                loadBatchData();
            });
        });
    }

    /**
     * 应用排序
     */
    private void applySorting(List<SignalData> list) {
        switch (currentSortOrder) {
            case DATE_DESC:
                list.sort((a, b) -> compareDate(b.getTimestamp(), a.getTimestamp()));
                break;
            case DATE_ASC:
                list.sort((a, b) -> compareDate(a.getTimestamp(), b.getTimestamp()));
                break;
            case RSSI_DESC:
                list.sort((a, b) -> Integer.compare(b.getRssi(), a.getRssi()));
                break;
            case RSSI_ASC:
                list.sort((a, b) -> Integer.compare(a.getRssi(), b.getRssi()));
                break;
            case OPERATOR_ASC:
                list.sort((a, b) -> {
                    String opA = a.getOperator() != null ? a.getOperator() : "";
                    String opB = b.getOperator() != null ? b.getOperator() : "";
                    return opA.compareTo(opB);
                });
                break;
        }
    }

    /**
     * 比较日期
     */
    private int compareDate(String date1, String date2) {
        if (date1 == null && date2 == null) return 0;
        if (date1 == null) return -1;
        if (date2 == null) return 1;
        return date1.compareTo(date2);
    }

    /**
     * 判断是否是今天
     */
    private boolean isToday(String timestamp) {
        if (timestamp == null) return false;
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
            return timestamp.startsWith(today);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否是本周
     */
    private boolean isThisWeek(String timestamp) {
        if (timestamp == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            Date date = sdf.parse(timestamp.split(" ")[0]);
            if (date == null) return false;
            
            Date now = new Date();
            long diff = now.getTime() - date.getTime();
            return diff <= 7 * 24 * 60 * 60 * 1000L;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 加载批量数据
     */
    private void loadBatchData() {
        if (filteredSignalData == null) return;

        int start = currentPage * BATCH_SIZE;
        int end = Math.min(start + BATCH_SIZE, filteredSignalData.size());

        if (start >= filteredSignalData.size()) {
            hasMoreData = false;
            return;
        }

        List<SignalData> batchData = filteredSignalData.subList(start, end);
        
        if (currentPage == 0) {
            adapter.updateData(batchData);
        } else {
            adapter.addData(batchData);
        }

        currentPage++;
        hasMoreData = end < filteredSignalData.size();
    }

    /**
     * 加载更多数据
     */
    private void loadMoreData() {
        isLoadingMore = true;
        executorService.execute(() -> {
            try {
                Thread.sleep(500); // 模拟加载延迟
                mainHandler.post(() -> {
                    loadBatchData();
                    isLoadingMore = false;
                });
            } catch (InterruptedException e) {
                isLoadingMore = false;
            }
        });
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        swipeRefreshLayout.setRefreshing(true);
        loadHistoryDataAsync();
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * 显示加载状态
     */
    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            progressIndicator.setProgress(50, true);
        }
    }

    /**
     * 显示空状态
     */
    private void showEmptyState(boolean show, String message) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (message != null && tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
                .setAction("重试", v -> loadHistoryDataAsync())
                .show();
    }

    /**
     * 显示导出选项
     */
    private void showExportOptions() {
        if (filteredSignalData == null || filteredSignalData.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {
            "导出当前视图为CSV",
            "导出全部数据为CSV",
            "导出为PDF报告",
            "导出选中项",
            "分享数据"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("导出数据")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportCurrentView();
                            break;
                        case 1:
                            exportAllData();
                            break;
                        case 2:
                            exportAsPdf();
                            break;
                        case 3:
                            exportSelectedItems();
                            break;
                        case 4:
                            shareData();
                            break;
                    }
                })
                .show();
    }

    /**
     * 导出当前视图
     */
    private void exportCurrentView() {
        if (adapter.getCurrentData() == null || adapter.getCurrentData().isEmpty()) {
            Toast.makeText(this, "当前视图无数据", Toast.LENGTH_SHORT).show();
            return;
        }

        showExportProgress();
        
        executorService.execute(() -> {
            String fileName = "Signal_View_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".csv";
            boolean success = exportUtils.exportToCSV(adapter.getCurrentData(), fileName);

            mainHandler.post(() -> {
                hideExportProgress();
                if (success) {
                    Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 导出全部数据
     */
    private void exportAllData() {
        showExportProgress();
        
        executorService.execute(() -> {
            String fileName = "Signal_All_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".csv";
            boolean success = exportUtils.exportToCSV(allSignalData, fileName);

            mainHandler.post(() -> {
                hideExportProgress();
                if (success) {
                    Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 导出为PDF
     */
    private void exportAsPdf() {
        showExportProgress();
        
        executorService.execute(() -> {
            try {
                // 创建PDF文档
                PdfDocument document = new PdfDocument();
                
                // 创建页面
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4尺寸
                PdfDocument.Page page = document.startPage(pageInfo);

                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();
                
                // 绘制标题
                paint.setColor(Color.BLACK);
                paint.setTextSize(20f);
                paint.setFakeBoldText(true);
                canvas.drawText("信号测试历史数据报告", 50, 50, paint);
                
                // 绘制日期
                paint.setTextSize(12f);
                paint.setFakeBoldText(false);
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
                canvas.drawText("生成时间: " + date, 50, 80, paint);
                
                // 绘制统计信息
                int yPos = 120;
                paint.setTextSize(14f);
                canvas.drawText("数据统计:", 50, yPos, paint);
                yPos += 25;
                canvas.drawText("总记录数: " + allSignalData.size(), 70, yPos, paint);
                yPos += 20;
                
                long count4G = allSignalData.stream().filter(d -> "4G".equals(d.getNetworkType())).count();
                long count5G = allSignalData.stream().filter(d -> "5G".equals(d.getNetworkType())).count();
                canvas.drawText("4G记录: " + count4G, 70, yPos, paint);
                yPos += 20;
                canvas.drawText("5G记录: " + count5G, 70, yPos, paint);
                
                // 绘制表格头
                yPos += 40;
                paint.setTextSize(11f);
                paint.setFakeBoldText(true);
                canvas.drawText("时间", 50, yPos, paint);
                canvas.drawText("运营商", 150, yPos, paint);
                canvas.drawText("网络", 250, yPos, paint);
                canvas.drawText("RSSI", 320, yPos, paint);
                canvas.drawText("RSRP", 380, yPos, paint);
                canvas.drawText("位置", 440, yPos, paint);
                
                paint.setFakeBoldText(false);
                
                // 绘制数据行
                int rowCount = 0;
                for (SignalData data : allSignalData) {
                    yPos += 20;
                    if (yPos > 800) {
                        // 分页
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPos = 50;
                        rowCount = 0;
                    }
                    
                    canvas.drawText(formatDate(data.getTimestamp()), 50, yPos, paint);
                    canvas.drawText(data.getOperator() != null ? data.getOperator() : "-", 150, yPos, paint);
                    canvas.drawText(data.getNetworkType() != null ? data.getNetworkType() : "-", 250, yPos, paint);
                    canvas.drawText(String.valueOf(data.getRssi()), 320, yPos, paint);
                    canvas.drawText(String.valueOf(data.getRsrp()), 380, yPos, paint);
                    canvas.drawText(shortenLocation(data.getLocation()), 440, yPos, paint);
                    
                    rowCount++;
                }

                document.finishPage(page);

                // 保存PDF
                File directory = new File(getExternalFilesDir(null), "SignalTest/Reports");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                String fileName = "Signal_Report_" + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".pdf";
                File file = new File(directory, fileName);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    document.writeTo(fos);
                }

                document.close();

                mainHandler.post(() -> {
                    hideExportProgress();
                    showExportSuccess(file.getAbsolutePath());
                });

            } catch (IOException e) {
                Log.e(TAG, "PDF导出失败", e);
                mainHandler.post(() -> {
                    hideExportProgress();
                    Toast.makeText(this, "PDF导出失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 导出选中项
     */
    private void exportSelectedItems() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "请先选择要导出的项目", Toast.LENGTH_SHORT).show();
            return;
        }

        showExportProgress();
        
        executorService.execute(() -> {
            String fileName = "Signal_Selected_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".csv";
            boolean success = exportUtils.exportToCSV(selectedItems, fileName);

            mainHandler.post(() -> {
                hideExportProgress();
                if (success) {
                    Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 分享数据
     */
    private void shareData() {
        // 先导出临时文件
        executorService.execute(() -> {
            String fileName = "temp_share_" + System.currentTimeMillis() + ".csv";
            boolean success = exportUtils.exportToCSV(filteredSignalData, fileName);

            mainHandler.post(() -> {
                if (success) {
                    Toast.makeText(this, "分享成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 分享文件
     */
    private void shareFile(File file) {
        Uri fileUri;
        
        if (Build.VERSION.SDK_INT >= 24) {
            fileUri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
        } else {
            fileUri = Uri.fromFile(file);
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, "分享数据"));
    }

    /**
     * 显示导出进度
     */
    private void showExportProgress() {
        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setProgress(0, true);
    }

    /**
     * 隐藏导出进度
     */
    private void hideExportProgress() {
        progressIndicator.setVisibility(View.GONE);
    }

    /**
     * 显示导出成功
     */
    private void showExportSuccess(String filePath) {
        Snackbar.make(recyclerView, "导出成功: " + filePath, Snackbar.LENGTH_LONG)
                .setAction("打开", v -> openFile(filePath))
                .show();
    }

    /**
     * 打开文件
     */
    private void openFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return;

        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/csv");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "没有可打开CSV文件的应用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteAllConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除所有记录")
                .setMessage("确定要删除所有历史记录吗？此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> deleteAllData())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除所有数据
     */
    private void deleteAllData() {
        showLoading(true);
        
        executorService.execute(() -> {
            boolean success = storageUtils.deleteAllData();
            
            mainHandler.post(() -> {
                showLoading(false);
                if (success) {
                    Toast.makeText(this, "所有记录已删除", Toast.LENGTH_SHORT).show();
                    refreshData();
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 删除单条数据
     */
    private void deleteItem(SignalData data, int position) {
        executorService.execute(() -> {
            boolean success = storageUtils.deleteSignalData(data);
            
            mainHandler.post(() -> {
                if (success) {
                    allSignalData.remove(data);
                    filteredSignalData.remove(data);
                    adapter.removeItem(position);
                    
                    Snackbar.make(recyclerView, "已删除", Snackbar.LENGTH_LONG)
                            .setAction("撤销", v -> restoreItem(data, position))
                            .show();
                    
                    if (allSignalData.isEmpty()) {
                        showEmptyState(true, "暂无历史数据");
                        btnExport.setEnabled(false);
                        fabDeleteAll.hide();
                    }
                    
                    tvTotalCount.setText(String.format(Locale.CHINA, "总数: %d", allSignalData.size()));
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 恢复删除的数据
     */
    private void restoreItem(SignalData data, int position) {
        executorService.execute(() -> {
            long result = storageUtils.insertSignalData(data);
            boolean success = result > 0;
            
            mainHandler.post(() -> {
                if (success) {
                    allSignalData.add(position, data);
                    filteredSignalData.add(position, data);
                    adapter.restoreItem(data, position);
                    
                    showEmptyState(false, null);
                    btnExport.setEnabled(true);
                    fabDeleteAll.show();
                    
                    tvTotalCount.setText(String.format(Locale.CHINA, "总数: %d", allSignalData.size()));
                } else {
                    Toast.makeText(this, "恢复失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 进入选择模式
     */
    private void enterSelectionMode() {
        isSelectionMode = true;
        selectedItems.clear();
        adapter.setSelectionMode(true);
        
        // 显示选择模式UI（简化实现）
        updateSelectedCount();
        
        // 隐藏其他控件
        btnExport.setVisibility(View.GONE);
        fabDeleteAll.hide();
        
        Snackbar.make(recyclerView, "长按项目可选择多个", Snackbar.LENGTH_SHORT).show();
    }

    /**
     * 退出选择模式
     */
    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        adapter.setSelectionMode(false);
        
        // 隐藏选择模式UI（简化实现）
        
        // 显示其他控件
        btnExport.setVisibility(View.VISIBLE);
        if (!allSignalData.isEmpty()) {
            fabDeleteAll.show();
        }
    }

    /**
     * 更新选中计数
     */
    private void updateSelectedCount() {
        tvSelectedCount.setText(String.format(Locale.CHINA, "已选择: %d", selectedItems.size()));
    }

    /**
     * 选择全部
     */
    private void selectAll() {
        selectedItems.clear();
        selectedItems.addAll(adapter.getCurrentData());
        adapter.selectAll();
        updateSelectedCount();
    }

    /**
     * 取消全部选择
     */
    private void deselectAll() {
        selectedItems.clear();
        adapter.deselectAll();
        updateSelectedCount();
    }

    /**
     * 删除选中的项目
     */
    private void deleteSelected() {
        if (selectedItems.isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("删除选中项")
                .setMessage("确定要删除选中的 " + selectedItems.size() + " 条记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    showLoading(true);
                    
                    executorService.execute(() -> {
                        for (SignalData data : selectedItems) {
                            storageUtils.deleteSignalData(data);
                            allSignalData.remove(data);
                            filteredSignalData.remove(data);
                        }
                        
                        mainHandler.post(() -> {
                            showLoading(false);
                            adapter.removeItems(selectedItems);
                            
                            Snackbar.make(recyclerView, 
                                "已删除 " + selectedItems.size() + " 条记录", 
                                Snackbar.LENGTH_LONG).show();
                            
                            exitSelectionMode();
                            
                            if (allSignalData.isEmpty()) {
                                showEmptyState(true, "暂无历史数据");
                                btnExport.setEnabled(false);
                                fabDeleteAll.hide();
                            }
                            
                            tvTotalCount.setText(String.format(Locale.CHINA, "总数: %d", allSignalData.size()));
                        });
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 格式化日期
     */
    private String formatDate(String timestamp) {
        if (timestamp == null) return "-";
        try {
            String[] parts = timestamp.split(" ");
            if (parts.length > 1) {
                String[] timeParts = parts[1].split(":");
                return timeParts[0] + ":" + timeParts[1];
            }
            return timestamp;
        } catch (Exception e) {
            return timestamp;
        }
    }

    /**
     * 缩短位置字符串
     */
    private String shortenLocation(String location) {
        if (location == null) return "-";
        if (location.length() > 10) {
            return location.substring(0, 8) + "...";
        }
        return location;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_export) {
            showExportOptions();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteAllConfirmDialog();
            return true;
        } else if (id == R.id.action_refresh) {
            loadHistoryDataAsync();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onItemClick(SignalData data, int position) {
        if (isSelectionMode) {
            // 选择模式：切换选中状态
            if (selectedItems.contains(data)) {
                selectedItems.remove(data);
            } else {
                selectedItems.add(data);
            }
            adapter.toggleSelection(position);
            updateSelectedCount();
        } else {
            // 普通模式：查看详情（简化实现）
            // 这里可以添加一个简单的对话框来显示详情
        }
    }

    public void onItemLongClick(SignalData data, int position) {
        if (!isSelectionMode) {
            enterSelectionMode();
            selectedItems.add(data);
            adapter.toggleSelection(position);
            updateSelectedCount();
        }
    }

    public void onDeleteClick(SignalData data, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除记录")
                .setMessage("确定要删除这条记录吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteItem(data, position))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 滑动删除回调
     */
    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private final HistoryAdapter adapter;

        SwipeToDeleteCallback(HistoryAdapter adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            this.adapter = adapter;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            SignalData data = adapter.getCurrentData().get(position);
            deleteItem(data, position);
        }
    }
}