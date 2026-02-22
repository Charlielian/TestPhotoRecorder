package com.signal.test.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.signal.test.models.SignalData;
import com.signal.test.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private List<SignalData> signalDataList;
    
    public HistoryAdapter(Context context, List<SignalData> signalDataList) {
        this.context = context;
        this.signalDataList = signalDataList;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SignalData data = signalDataList.get(position);
        
        // 设置时间戳
        holder.tvTimestamp.setText(data.getTimestamp());
        
        // 设置运营商
        holder.tvOperator.setText("运营商: " + data.getOperator());
        
        // 设置网络类型
        holder.tvNetworkType.setText("网络类型: " + data.getNetworkType());
        
        // 设置信号强度
        holder.tvRssi.setText("信号强度: " + data.getRssi() + " dBm");
        
        // 设置位置
        holder.tvLocation.setText("位置: " + data.getLocation());
        
        // 设置缩略图
        if (data.getPhotoPath() != null && !data.getPhotoPath().isEmpty()) {
            Bitmap bitmap = loadScaledBitmap(data.getPhotoPath(), 200, 200);
            if (bitmap != null) {
                holder.ivThumbnail.setImageBitmap(bitmap);
            } else {
                holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    
    @Override
    public int getItemCount() {
        return signalDataList.size();
    }

    /**
     * 加载缩放后的位图以避免内存溢出
     * @param path 图片路径
     * @param reqWidth 目标宽度
     * @param reqHeight 目标高度
     * @return 缩放后的位图
     */
    private Bitmap loadScaledBitmap(String path, int reqWidth, int reqHeight) {
        try {
            // 首先解码图片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // 计算缩放比例
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // 使用缩放比例解码图片
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用更少内存
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 计算合适的缩放比例
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        
        ImageView ivThumbnail;
        TextView tvTimestamp, tvOperator, tvNetworkType, tvRssi, tvLocation;
        
        public ViewHolder(View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvOperator = itemView.findViewById(R.id.tv_operator);
            tvNetworkType = itemView.findViewById(R.id.tv_network_type);
            tvRssi = itemView.findViewById(R.id.tv_rssi);
            tvLocation = itemView.findViewById(R.id.tv_location);
        }
    }
}