package com.ptithcm.attendapp.model;

import java.util.List;

public class ClassResponse {
    private List<ClassItem> items; // Hứng mảng "items" từ JSON
    private int page;
    private int totalPages;

    public List<ClassItem> getItems() { return items; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }
}