package com.signal.test.services;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellSignalStrengthNr;
import android.telephony.TelephonyManager;
import android.os.Build;

import com.signal.test.models.SignalData;

import java.util.List;

public class SignalCollector {
    private Context context;
    private TelephonyManager telephonyManager;
    
    public SignalCollector(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }
    
    // 获取信号数据
    public SignalData getSignalData() {
        return getSignalData(-1);
    }
    
    // 获取指定SIM卡的信号数据
    public SignalData getSignalData(int subId) {
        SignalData data = new SignalData();
        
        // 获取运营商信息
        data.setOperator(getOperator(subId));
        
        // 获取网络类型
        data.setNetworkType(getNetworkType(subId));
        
        // 获取小区信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getCellInfoModern(data, subId);
        } else {
            getCellInfoLegacy(data);
        }
        
        // 获取信号强度
        data.setRssi(getSignalStrength(subId));
        
        return data;
    }
    
    // 获取运营商
    private String getOperator(int subId) {
        try {
            if (subId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                TelephonyManager subTelephonyManager = telephonyManager.createForSubscriptionId(subId);
                return subTelephonyManager.getNetworkOperatorName();
            } else {
                return telephonyManager.getNetworkOperatorName();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "未知";
        }
    }
    
    // 获取运营商
    private String getOperator() {
        return getOperator(-1);
    }
    
    // 获取网络类型
    private String getNetworkType(int subId) {
        try {
            int type;
            if (subId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                TelephonyManager subTelephonyManager = telephonyManager.createForSubscriptionId(subId);
                type = subTelephonyManager.getNetworkType();
            } else {
                type = telephonyManager.getNetworkType();
            }
            
            switch (type) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "4G";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "5G";

                default:
                    return "Unknown";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
    
    // 获取网络类型
    private String getNetworkType() {
        return getNetworkType(-1);
    }
    
    // 获取现代Android版本的小区信息
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void getCellInfoModern(SignalData data, int subId) {
        try {
            List<CellInfo> cellInfos;
            if (subId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                TelephonyManager subTelephonyManager = telephonyManager.createForSubscriptionId(subId);
                cellInfos = subTelephonyManager.getAllCellInfo();
            } else {
                cellInfos = telephonyManager.getAllCellInfo();
            }
            
            if (cellInfos != null && !cellInfos.isEmpty()) {
                for (CellInfo cellInfo : cellInfos) {
                    if (cellInfo instanceof CellInfoNr) {
                        // 处理5G小区信息
                        CellInfoNr cellInfoNr = (CellInfoNr) cellInfo;
                        CellIdentityNr identity = (CellIdentityNr) cellInfoNr.getCellIdentity();
                        CellSignalStrengthNr signalStrength = (CellSignalStrengthNr) cellInfoNr.getCellSignalStrength();
                        
                        // 获取5G CGI
                        String nrCgi = "NR:" + identity.getMccString() + "-" + identity.getMncString() + "-" + 
                                     identity.getTac() + "-" + identity.getNci();
                        data.setNrCgi(nrCgi);
                        
                        // 获取5G频点
                        data.setNrFrequency(identity.getNrarfcn());
                        
                        // 获取5G频段
                        data.setNrBand(get5GBandFromArfcn(identity.getNrarfcn()));
                        
                        // 获取5G PCI
                        data.setNrPci(identity.getPci());
                        
                        // 获取5G信号强度参数
                        data.setRsrp(signalStrength.getSsRsrp());
                        data.setSinr(signalStrength.getSsSinr());
                        data.setRsrq(signalStrength.getSsRsrq());
                        
                        // 同时设置为主小区信息
                        data.setCgi(nrCgi);
                        data.setFrequency(identity.getNrarfcn());
                        data.setBand(data.getNrBand());
                        data.setPci(identity.getPci());
                        data.setRssi(signalStrength.getSsRsrp());
                        
                        break;
                    } else if (cellInfo instanceof CellInfoLte) {
                        // 处理LTE小区信息
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                        CellIdentityLte identity = cellInfoLte.getCellIdentity();
                        
                        // 获取CGI
                        String cgi = "LTE:" + identity.getMcc() + "-" + identity.getMnc() + "-" + 
                                    identity.getTac() + "-" + identity.getCi();
                        data.setCgi(cgi);
                        
                        // 获取频点
                        data.setFrequency(identity.getEarfcn());
                        
                        // 获取频段
                        data.setBand(getBandFromEarfcn(identity.getEarfcn()));

                        // 获取LTE PCI
                        data.setPci(identity.getPci());

                        // 获取LTE RSRP (SINR 在某些 API 级别不可用)
                        data.setRssi(cellInfoLte.getCellSignalStrength().getRsrp());

                        break;
                    }
                    // 处理其他网络类型...
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 获取现代Android版本的小区信息
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void getCellInfoModern(SignalData data) {
        getCellInfoModern(data, -1);
    }
    
    // 获取传统Android版本的小区信息
    private void getCellInfoLegacy(SignalData data) {
        // 使用反射获取信息
        try {
            // 反射代码...
            data.setCgi("N/A");
            data.setFrequency(0);
            data.setBand("N/A");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 获取信号强度
    private int getSignalStrength(int subId) {
        try {
            List<CellInfo> cellInfos;
            if (subId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                TelephonyManager subTelephonyManager = telephonyManager.createForSubscriptionId(subId);
                cellInfos = subTelephonyManager.getAllCellInfo();
            } else {
                cellInfos = telephonyManager.getAllCellInfo();
            }

            if (cellInfos != null && !cellInfos.isEmpty()) {
                for (CellInfo cellInfo : cellInfos) {
                    if (cellInfo.isRegistered()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
                            // 5G信号强度
                            CellInfoNr cellInfoNr = (CellInfoNr) cellInfo;
                            CellSignalStrengthNr signalStrength = (CellSignalStrengthNr) cellInfoNr.getCellSignalStrength();
                            return signalStrength.getSsRsrp();
                        } else if (cellInfo instanceof CellInfoLte) {
                            // 4G信号强度
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                            return cellInfoLte.getCellSignalStrength().getRsrp();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -999; // 返回无效值表示无法获取
    }
    
    // 获取信号强度
    private int getSignalStrength() {
        return getSignalStrength(-1);
    }
    
    // 根据频点获取频段
    private String getBandFromEarfcn(int earfcn) {
        // 频段映射表
        if (earfcn >= 0 && earfcn <= 599) {
            return "Band 1";
        } else if (earfcn >= 600 && earfcn <= 1199) {
            return "Band 2";
        } else if (earfcn >= 1200 && earfcn <= 1949) {
            return "Band 3";
        } else if (earfcn >= 1950 && earfcn <= 2399) {
            return "Band 4";
        } else if (earfcn >= 2400 && earfcn <= 2649) {
            return "Band 5";
        } else if (earfcn >= 2650 && earfcn <= 2749) {
            return "Band 6";
        } else if (earfcn >= 2750 && earfcn <= 3449) {
            return "Band 7";
        } else if (earfcn >= 3450 && earfcn <= 3799) {
            return "Band 8";
        } else if (earfcn >= 3800 && earfcn <= 4149) {
            return "Band 9";
        } else if (earfcn >= 4150 && earfcn <= 4749) {
            return "Band 10";
        } else if (earfcn >= 4750 && earfcn <= 4949) {
            return "Band 11";
        } else if (earfcn >= 5010 && earfcn <= 5179) {
            return "Band 12";
        } else if (earfcn >= 5180 && earfcn <= 5279) {
            return "Band 13";
        } else if (earfcn >= 5280 && earfcn <= 5379) {
            return "Band 14";
        } else if (earfcn >= 5735 && earfcn <= 5849) {
            return "Band 17";
        } else if (earfcn >= 5850 && earfcn <= 5999) {
            return "Band 18";
        } else if (earfcn >= 6000 && earfcn <= 6149) {
            return "Band 19";
        } else if (earfcn >= 6150 && earfcn <= 6449) {
            return "Band 20";
        } else if (earfcn >= 6450 && earfcn <= 6599) {
            return "Band 21";
        } else if (earfcn >= 6600 && earfcn <= 7399) {
            return "Band 22";
        } else if (earfcn >= 7500 && earfcn <= 7699) {
            return "Band 23";
        } else if (earfcn >= 7700 && earfcn <= 7899) {
            return "Band 24";
        } else if (earfcn >= 8040 && earfcn <= 8689) {
            return "Band 25";
        } else if (earfcn >= 8690 && earfcn <= 9039) {
            return "Band 26";
        } else if (earfcn >= 9040 && earfcn <= 9209) {
            return "Band 27";
        } else if (earfcn >= 9210 && earfcn <= 9659) {
            return "Band 28";
        } else if (earfcn >= 36000 && earfcn <= 36199) {
            return "Band 33";
        } else if (earfcn >= 36200 && earfcn <= 36349) {
            return "Band 34";
        } else if (earfcn >= 36350 && earfcn <= 36949) {
            return "Band 35";
        } else if (earfcn >= 36950 && earfcn <= 37549) {
            return "Band 36";
        } else if (earfcn >= 37550 && earfcn <= 38299) {
            return "Band 37";
        } else if (earfcn >= 38300 && earfcn <= 38699) {
            return "Band 38";
        } else if (earfcn >= 38700 && earfcn <= 39699) {
            return "Band 39";
        } else if (earfcn >= 39700 && earfcn <= 41599) {
            return "Band 40";
        } else if (earfcn >= 41600 && earfcn <= 43599) {
            return "Band 41";
        } else if (earfcn >= 50000 && earfcn <= 51499) {
            return "Band 42";
        } else if (earfcn >= 51500 && earfcn <= 52500) {
            return "Band 43";
        } else {
            return "Unknown";
        }
    }
    
    // 根据5G ARFCN频点获取频段
    private String get5GBandFromArfcn(int nrarfcn) {
        // 5G频段映射表（基于3GPP标准）
        if (nrarfcn >= 10000 && nrarfcn <= 20000) {
            return "n1";
        } else if (nrarfcn >= 20000 && nrarfcn <= 30000) {
            return "n2";
        } else if (nrarfcn >= 30000 && nrarfcn <= 40000) {
            return "n3";
        } else if (nrarfcn >= 40000 && nrarfcn <= 50000) {
            return "n5";
        } else if (nrarfcn >= 50000 && nrarfcn <= 60000) {
            return "n7";
        } else if (nrarfcn >= 60000 && nrarfcn <= 70000) {
            return "n8";
        } else if (nrarfcn >= 100000 && nrarfcn <= 120000) {
            return "n20";
        } else if (nrarfcn >= 120000 && nrarfcn <= 140000) {
            return "n25";
        } else if (nrarfcn >= 140000 && nrarfcn <= 160000) {
            return "n28";
        } else if (nrarfcn >= 240000 && nrarfcn <= 270000) {
            return "n38";
        } else if (nrarfcn >= 270000 && nrarfcn <= 290000) {
            return "n39";
        } else if (nrarfcn >= 290000 && nrarfcn <= 320000) {
            return "n40";
        } else if (nrarfcn >= 320000 && nrarfcn <= 350000) {
            return "n41";
        } else if (nrarfcn >= 350000 && nrarfcn <= 380000) {
            return "n48";
        } else if (nrarfcn >= 380000 && nrarfcn <= 410000) {
            return "n50";
        } else if (nrarfcn >= 410000 && nrarfcn <= 440000) {
            return "n51";
        } else if (nrarfcn >= 440000 && nrarfcn <= 470000) {
            return "n66";
        } else if (nrarfcn >= 470000 && nrarfcn <= 500000) {
            return "n70";
        } else if (nrarfcn >= 500000 && nrarfcn <= 530000) {
            return "n74";
        } else if (nrarfcn >= 530000 && nrarfcn <= 560000) {
            return "n77";
        } else if (nrarfcn >= 560000 && nrarfcn <= 590000) {
            return "n78";
        } else if (nrarfcn >= 590000 && nrarfcn <= 620000) {
            return "n79";
        } else {
            return "Unknown";
        }
    }
}