package com.example.roomie.data;

public class PostModel {
    private String id;
    private String title;
    private String price;
    private String location;
    private String status;
    private String imageUrl;

    public PostModel() {}

    public PostModel(String id, String title, String price, String location, String status, String imageUrl) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.location = location;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
