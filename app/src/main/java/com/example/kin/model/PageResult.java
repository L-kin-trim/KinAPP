package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class PageResult<T> {
    public final List<T> items = new ArrayList<>();
    public int page;
    public int size;
    public int totalPages;
    public long total;
}
