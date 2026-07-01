package com.english.config;

import com.english.entity.ShopProduct;
import com.english.mapper.ShopProductMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;



@Component
public class ShopDataInitializer implements CommandLineRunner {
    private final ShopProductMapper productMapper;

    public ShopDataInitializer(ShopProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public void run(String... args) {
        /*
         * 项目当前 application.yml 里 sql.init.mode=never，不会自动执行 data.sql。
         * 因此商城商品用 CommandLineRunner 初始化，应用启动后由 JPA 自动建表，再插入默认商品。
         */
        if (productMapper.count() > 0) {
            // 只在空表时初始化，避免每次启动重复插入商品。
            return;
        }

        create("CET4 精选课程", "大学英语四级", "覆盖核心词汇、阅读技巧、听力突破和写作模板，适合四级系统备考。",
                "199", "299", "course", "热卖课程", "tone-blue", "30 节精讲课|四级高频词清单|模拟训练计划", 120, 1);
        create("考研英语精选课程", "考研英语", "面向考研英语一/二，强化长难句、阅读逻辑、翻译与作文提分路径。",
                "399", "599", "book", "系统课", "tone-green", "长难句专项|阅读题型拆解|作文素材库", 80, 2);
        create("考研真题书", "真题资料", "精选历年考研英语真题，按题型拆解解析，适合刷题和错题复盘。",
                "89", "128", "exam", "备考书籍", "tone-orange", "历年真题汇编|逐题解析|答案速查册", 300, 3);
        create("CET4 高频词汇手册", "词汇资料", "配合每日单词练习使用，按考试频率整理重点词、短语和例句。",
                "49", "69", "vocab", "词汇精选", "tone-red", "高频词分组|短语搭配|例句速记", 500, 4);
    }

    private void create(String title,
                        String category,
                        String description,
                        String price,
                        String originalPrice,
                        String icon,
                        String tag,
                        String tone,
                        String points,
                        Integer stock,
                        Integer sortOrder) {
        /*
         * points 使用 “|” 分隔，例如：
         * “30 节精讲课|四级高频词清单|模拟训练计划”
         * 前端拿到后按 “|” split 成数组渲染。
         */
        ShopProduct product = new ShopProduct();
        product.setTitle(title);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setOriginalPrice(new BigDecimal(originalPrice));
        product.setIcon(icon);
        product.setTag(tag);
        product.setTone(tone);
        product.setPoints(points);
        product.setStock(stock);
        product.setSortOrder(sortOrder);
        product.setActive(true);
        productMapper.save(product);
    }
}
