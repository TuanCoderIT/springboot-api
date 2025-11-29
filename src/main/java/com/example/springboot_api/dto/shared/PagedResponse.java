package com.example.springboot_api.dto.shared;

import java.util.List;

public class PagedResponse<T> {
  private List<T> items;
  private Meta meta;

  public PagedResponse(List<T> items, Meta meta) {
    this.items = items;
    this.meta = meta;
  }

  public static class Meta {
    public int page;
    public int size;
    public long total;
    public int totalPages;

    public Meta(int page, int size, long total, int totalPages) {
      this.page = page;
      this.size = size;
      this.total = total;
      this.totalPages = totalPages;
    }
  }

  public List<T> getItems() {
    return items;
  }

  public Meta getMeta() {
    return meta;
  }
}
