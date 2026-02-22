package com.signal.test.models;

public class SignalData {
    private int id;
    private String operator;          // 运营商
    private String cgi;               // 小区CGI
    private int frequency;            // 频点
    private String band;              // 频段
    private int rssi;                 // 接收电平
    private int pci;                  // 物理小区ID
    private int sinr;                 // 信号与干扰加噪声比
    private String networkType;       // 网络类型
    private double latitude;          // 纬度
    private double longitude;         // 经度
    private String location;          // 位置描述
    private String timestamp;         // 时间戳
    private String photoPath;         // 照片路径
    // 5G特有字段
    private String nrCgi;             // 5G小区CGI
    private int nrFrequency;          // 5G频点
    private String nrBand;            // 5G频段
    private int nrPci;                // 5G物理小区ID
    private int rsrp;                 // 参考信号接收功率
    private int rsrq;                 // 参考信号接收质量
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public String getCgi() {
        return cgi;
    }
    
    public void setCgi(String cgi) {
        this.cgi = cgi;
    }
    
    public int getFrequency() {
        return frequency;
    }
    
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    
    public String getBand() {
        return band;
    }
    
    public void setBand(String band) {
        this.band = band;
    }
    
    public int getRssi() {
        return rssi;
    }
    
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
    
    public int getPci() {
        return pci;
    }
    
    public void setPci(int pci) {
        this.pci = pci;
    }
    
    public int getSinr() {
        return sinr;
    }
    
    public void setSinr(int sinr) {
        this.sinr = sinr;
    }
    
    public String getNetworkType() {
        return networkType;
    }
    
    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPhotoPath() {
        return photoPath;
    }
    
    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
    
    // 5G特有字段的Getters and Setters
    public String getNrCgi() {
        return nrCgi;
    }
    
    public void setNrCgi(String nrCgi) {
        this.nrCgi = nrCgi;
    }
    
    public int getNrFrequency() {
        return nrFrequency;
    }
    
    public void setNrFrequency(int nrFrequency) {
        this.nrFrequency = nrFrequency;
    }
    
    public String getNrBand() {
        return nrBand;
    }
    
    public void setNrBand(String nrBand) {
        this.nrBand = nrBand;
    }
    
    public int getNrPci() {
        return nrPci;
    }
    
    public void setNrPci(int nrPci) {
        this.nrPci = nrPci;
    }
    
    public int getRsrp() {
        return rsrp;
    }
    
    public void setRsrp(int rsrp) {
        this.rsrp = rsrp;
    }
    
    public int getRsrq() {
        return rsrq;
    }
    
    public void setRsrq(int rsrq) {
        this.rsrq = rsrq;
    }
}