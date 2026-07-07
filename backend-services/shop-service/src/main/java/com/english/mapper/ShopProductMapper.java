package com.english.mapper;

import com.english.entity.ShopProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShopProductMapper extends JpaRepository<ShopProduct, Long> {
    // 商城列表只展示上架商品，并按 sortOrder 控制展示顺序。
    List<ShopProduct> findByActiveTrueOrderBySortOrderAscIdAsc();

    @Modifying
    /*
     * 数据库层面的原子扣库存。
     *
     * 不使用“先查库存再 save”的原因：
     * 高并发下两个请求可能同时查到 stock=1，然后都保存成 stock=0，造成超卖。
     * 这里把 stock > 0 放进 update 条件里，数据库会保证同一行更新的原子性。
     * 返回值为 1 表示扣减成功，0 表示库存不足或商品不存在。
     */
    @Query("update ShopProduct p set p.stock = p.stock - 1 where p.id = :productId and p.stock > 0")
    int decreaseStock(@Param("productId") Long productId);

    @Modifying
    // 订单创建失败或超时取消时回补库存。
    @Query("update ShopProduct p set p.stock = p.stock + 1 where p.id = :productId")
    int increaseStock(@Param("productId") Long productId);
}
