package com.androidapp.attendencecheckqrcode.data.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PageResponse<T> {
    @SerializedName("items")
    private List<T> items;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    @SerializedName("totalElements")
    private int totalElements;

    @SerializedName("totalPages")
    private int totalPages;

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }
}