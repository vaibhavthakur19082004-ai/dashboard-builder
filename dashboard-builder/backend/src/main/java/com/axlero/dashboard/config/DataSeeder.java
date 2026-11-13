package com.axlero.dashboard.config;

import com.axlero.dashboard.entity.SalesRecord;
import com.axlero.dashboard.repository.SalesRecordRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Seeds a reproducible demo sales dataset on first start. */
@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedSales(SalesRecordRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            String[] regions = {"North", "South", "East", "West", "Central"};
            String[] channels = {"Online", "Retail", "Partner"};
            String[][] products = {{"Laptop", "Electronics"}, {"Phone", "Electronics"}, {"Desk", "Furniture"},
                    {"Chair", "Furniture"}, {"Notebook", "Stationery"}, {"Pen Set", "Stationery"}};
            Random rnd = new Random(42);
            List<SalesRecord> rows = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                String[] p = products[rnd.nextInt(products.length)];
                SalesRecord r = new SalesRecord();
                r.setRegion(regions[rnd.nextInt(regions.length)]);
                r.setChannel(channels[rnd.nextInt(channels.length)]);
                r.setProduct(p[0]);
                r.setCategory(p[1]);
                r.setQuantity(1 + rnd.nextInt(10));
                r.setAmount(BigDecimal.valueOf(20 + rnd.nextInt(2000)).add(BigDecimal.valueOf(rnd.nextInt(100), 2)));
                r.setOrderDate(LocalDate.of(2025, 1, 1).plusDays(rnd.nextInt(365)));
                rows.add(r);
            }
            repo.saveAll(rows);
        };
    }
}
