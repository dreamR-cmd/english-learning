package com.english.service;

import com.english.entity.ShopProduct;

import java.util.List;

public interface ShopProductSearchService {
    List<Long> searchProductIds(String keyword);

    void rebuildIndex();

    void indexProduct(ShopProduct product);
}
