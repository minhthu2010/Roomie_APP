package com.example.roomie.data.discovery;

import java.util.ArrayList;
import java.util.List;

/** DTO cho phường/xã thật và tra địa chỉ (Photon qua backend). */
public final class PlaceModels {

    private PlaceModels() {}

    /** GET /places/suggest */
    public static class Suggest {
        public List<Ward> wards = new ArrayList<>();
        public List<Address> addresses = new ArrayList<>();
    }

    public static class Ward {
        public String code;
        public String name;
        public String provinceCode;
        public String provinceName;
        public int roomCount;
    }

    /** Địa chỉ cụ thể có tọa độ: số nhà, đường, địa danh */
    public static class Address {
        public String title;
        public String subtitle;
        public double latitude;
        public double longitude;
        /** house | street | district | city | locality... */
        public String kind;
    }

    /** GET /admin-units/wards/{code}/center (tọa độ có thể null nếu không tra được) */
    public static class WardCenter {
        public String code;
        public String name;
        public String provinceName;
        public Double latitude;
        public Double longitude;
    }
}
