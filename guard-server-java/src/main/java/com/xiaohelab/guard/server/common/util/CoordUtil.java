package com.xiaohelab.guard.server.common.util;

/**
 * 简易坐标系转换工具：GCJ-02 / WGS84 / BD-09。
 * 网关统一转换为 WGS84，业务层仅接受 WGS84。
 */
public final class CoordUtil {

    private static final double PI = 3.141592653589793;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    private CoordUtil() {}

    public static double[] gcj02ToWgs84(double lng, double lat) {
        if (outOfChina(lng, lat)) return new double[]{lng, lat};
        double[] delta = delta(lng, lat);
        return new double[]{lng - delta[0], lat - delta[1]};
    }

    public static double[] bd09ToWgs84(double lng, double lat) {
        double[] gcj = bd09ToGcj02(lng, lat);
        return gcj02ToWgs84(gcj[0], gcj[1]);
    }

    public static double[] bd09ToGcj02(double lng, double lat) {
        double x = lng - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * PI);
        return new double[]{z * Math.cos(theta), z * Math.sin(theta)};
    }

    public static double[] toWgs84(double lng, double lat, String coordSystem) {
        if (coordSystem == null || coordSystem.equalsIgnoreCase("WGS84")) {
            return new double[]{lng, lat};
        }
        if (coordSystem.equalsIgnoreCase("GCJ-02")) return gcj02ToWgs84(lng, lat);
        if (coordSystem.equalsIgnoreCase("BD-09")) return bd09ToWgs84(lng, lat);
        throw new IllegalArgumentException("unsupported coord system: " + coordSystem);
    }

    /** 点到点距离（米，大圆距离 Haversine） */
    public static double haversineMeter(double lng1, double lat1, double lng2, double lat2) {
        double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    private static boolean outOfChina(double lng, double lat) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private static double[] delta(double lng, double lat) {
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{dLng, dLat};
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
}
