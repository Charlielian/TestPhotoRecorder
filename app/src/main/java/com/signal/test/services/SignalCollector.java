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
                        long nci = identity.getNci();
                        long genb = nci / 4096;
                        long lcrid = nci % 4096;
                        String nrCgi = identity.getMccString() + "-" + identity.getMncString() + "-" + 
                                     genb + "-" + lcrid;
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
                        int ci = identity.getCi();
                        int enb = ci / 256;
                        int lcrid = ci % 256;
                        String cgi = identity.getMcc() + "-" + identity.getMnc() + "-" + 
                                    enb + "-" + lcrid;
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
    
    // 根据LTE EARFCN频点计算实际频率
    private double calculateLTEFrequency(int earfcn) {
        // LTE频段映射表（基于3GPP标准）
        if (earfcn >= 0 && earfcn <= 599) {
            // Band 1
            return 1920 + 0.1 * (earfcn - 0);
        } else if (earfcn >= 600 && earfcn <= 1199) {
            // Band 2
            return 1850 + 0.1 * (earfcn - 600);
        } else if (earfcn >= 1200 && earfcn <= 1949) {
            // Band 3
            return 1710 + 0.1 * (earfcn - 1200);
        } else if (earfcn >= 1950 && earfcn <= 2399) {
            // Band 4
            return 1710 + 0.1 * (earfcn - 1950);
        } else if (earfcn >= 2400 && earfcn <= 2649) {
            // Band 5
            return 824 + 0.1 * (earfcn - 2400);
        } else if (earfcn >= 2650 && earfcn <= 2749) {
            // Band 6
            return 830 + 0.1 * (earfcn - 2650);
        } else if (earfcn >= 2750 && earfcn <= 3449) {
            // Band 7
            return 2500 + 0.1 * (earfcn - 2750);
        } else if (earfcn >= 3450 && earfcn <= 3799) {
            // Band 8
            return 880 + 0.1 * (earfcn - 3450);
        } else if (earfcn >= 3800 && earfcn <= 4149) {
            // Band 9
            return 1749.9 + 0.1 * (earfcn - 3800);
        } else if (earfcn >= 4150 && earfcn <= 4749) {
            // Band 10
            return 2110 + 0.1 * (earfcn - 4150);
        } else if (earfcn >= 4750 && earfcn <= 4949) {
            // Band 11
            return 1427.9 + 0.1 * (earfcn - 4750);
        } else if (earfcn >= 5010 && earfcn <= 5179) {
            // Band 12
            return 728 + 0.1 * (earfcn - 5010);
        } else if (earfcn >= 5180 && earfcn <= 5279) {
            // Band 13
            return 746 + 0.1 * (earfcn - 5180);
        } else if (earfcn >= 5280 && earfcn <= 5379) {
            // Band 14
            return 758 + 0.1 * (earfcn - 5280);
        } else if (earfcn >= 5735 && earfcn <= 5849) {
            // Band 17
            return 734 + 0.1 * (earfcn - 5735);
        } else if (earfcn >= 5850 && earfcn <= 5999) {
            // Band 18
            return 815 + 0.1 * (earfcn - 5850);
        } else if (earfcn >= 6000 && earfcn <= 6149) {
            // Band 19
            return 830 + 0.1 * (earfcn - 6000);
        } else if (earfcn >= 6150 && earfcn <= 6449) {
            // Band 20
            return 832 + 0.1 * (earfcn - 6150);
        } else if (earfcn >= 6450 && earfcn <= 6599) {
            // Band 21
            return 1447.9 + 0.1 * (earfcn - 6450);
        } else if (earfcn >= 6600 && earfcn <= 7399) {
            // Band 22
            return 3410 + 0.1 * (earfcn - 6600);
        } else if (earfcn >= 7500 && earfcn <= 7699) {
            // Band 23
            return 2180 + 0.1 * (earfcn - 7500);
        } else if (earfcn >= 7700 && earfcn <= 7899) {
            // Band 24
            return 1525 + 0.1 * (earfcn - 7700);
        } else if (earfcn >= 8040 && earfcn <= 8689) {
            // Band 25
            return 1930 + 0.1 * (earfcn - 8040);
        } else if (earfcn >= 8690 && earfcn <= 9039) {
            // Band 26
            return 814 + 0.1 * (earfcn - 8690);
        } else if (earfcn >= 9040 && earfcn <= 9209) {
            // Band 27
            return 807 + 0.1 * (earfcn - 9040);
        } else if (earfcn >= 9210 && earfcn <= 9659) {
            // Band 28
            return 703 + 0.1 * (earfcn - 9210);
        } else if (earfcn >= 36000 && earfcn <= 36199) {
            // Band 33
            return 1900 + 0.1 * (earfcn - 36000);
        } else if (earfcn >= 36200 && earfcn <= 36349) {
            // Band 34
            return 2010 + 0.1 * (earfcn - 36200);
        } else if (earfcn >= 36350 && earfcn <= 36949) {
            // Band 35
            return 1850 + 0.1 * (earfcn - 36350);
        } else if (earfcn >= 36950 && earfcn <= 37549) {
            // Band 36
            return 1930 + 0.1 * (earfcn - 36950);
        } else if (earfcn >= 37550 && earfcn <= 38299) {
            // Band 37
            return 1910 + 0.1 * (earfcn - 37550);
        } else if (earfcn >= 38300 && earfcn <= 38699) {
            // Band 38
            return 2570 + 0.1 * (earfcn - 38300);
        } else if (earfcn >= 38700 && earfcn <= 39699) {
            // Band 39
            return 1880 + 0.1 * (earfcn - 38700);
        } else if (earfcn >= 39700 && earfcn <= 41599) {
            // Band 40
            return 2300 + 0.1 * (earfcn - 39700);
        } else if (earfcn >= 41600 && earfcn <= 43599) {
            // Band 41
            return 2496 + 0.1 * (earfcn - 41600);
        } else if (earfcn >= 50000 && earfcn <= 51499) {
            // Band 42
            return 3400 + 0.1 * (earfcn - 50000);
        } else if (earfcn >= 51500 && earfcn <= 52500) {
            // Band 43
            return 3600 + 0.1 * (earfcn - 51500);
        } else {
            return 0;
        }
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
    
    // 根据5G ARFCN频点计算实际频率
    private double calculate5GFrequency(int nrarfcn) {
        double frequency;
        if (nrarfcn >= 0 && nrarfcn <= 599999) {
            // 0 – 3000 MHz, ΔFGlobal = 5 kHz
            frequency = 0 + 5 * (nrarfcn - 0) / 1000.0;
        } else if (nrarfcn >= 600000 && nrarfcn <= 2016666) {
            // 3000 – 24250 MHz, ΔFGlobal = 15 kHz
            frequency = 3000 + 15 * (nrarfcn - 600000) / 1000.0;
        } else if (nrarfcn >= 2016667 && nrarfcn <= 3279165) {
            // 24250 – 100000 MHz, ΔFGlobal = 60 kHz
            frequency = 24250.08 + 60 * (nrarfcn - 2016667) / 1000.0;
        } else {
            frequency = 0;
        }
        return frequency;
    }
    
    // 根据5G ARFCN频点获取频段
    private String get5GBandFromArfcn(int nrarfcn) {
        // 计算实际频率
        double frequency = calculate5GFrequency(nrarfcn);
        
        // 根据实际频率映射频段（基于3GPP标准）
        if (frequency >= 2110 && frequency <= 2170) {
            return "n1";
        } else if (frequency >= 1930 && frequency <= 1990) {
            return "n2";
        } else if (frequency >= 1710 && frequency <= 1785) {
            return "n3";
        } else if (frequency >= 824 && frequency <= 849) {
            return "n5";
        } else if (frequency >= 2500 && frequency <= 2570) {
            return "n7";
        } else if (frequency >= 880 && frequency <= 915) {
            return "n8";
        } else if (frequency >= 832 && frequency <= 862) {
            return "n20";
        } else if (frequency >= 1850 && frequency <= 1915) {
            return "n25";
        } else if (frequency >= 703 && frequency <= 748) {
            return "n28";
        } else if (frequency >= 2570 && frequency <= 2620) {
            return "n38";
        } else if (frequency >= 1880 && frequency <= 1920) {
            return "n39";
        } else if (frequency >= 2300 && frequency <= 2400) {
            return "n40";
        } else if (frequency >= 2496 && frequency <= 2690) {
            return "n41";
        } else if (frequency >= 3550 && frequency <= 3700) {
            return "n48";
        } else if (frequency >= 3300 && frequency <= 3400) {
            return "n50";
        } else if (frequency >= 3400 && frequency <= 3500) {
            return "n51";
        } else if (frequency >= 1710 && frequency <= 1780) {
            return "n66";
        } else if (frequency >= 1695 && frequency <= 1710) {
            return "n70";
        } else if (frequency >= 1427 && frequency <= 1470) {
            return "n74";
        } else if (frequency >= 3300 && frequency <= 4200) {
            return "n77";
        } else if (frequency >= 3300 && frequency <= 3800) {
            return "n78";
        } else if (frequency >= 4400 && frequency <= 5000) {
            return "n79";
        } else {
            return "Unknown";
        }
    }
}