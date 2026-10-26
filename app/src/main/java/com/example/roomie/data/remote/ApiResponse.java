package com.example.roomie.data.remote;

/** Backend trả về { "data": ... } khi thành công; danh sách phân trang có thêm "meta". */
public class ApiResponse<T> {
    public T data;
    public Meta meta;

    public static class Meta {
        public int page;
        public int limit;
        public int total;
    }
}
