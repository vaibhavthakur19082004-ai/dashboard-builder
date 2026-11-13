package com.axlero.dashboard;

import com.axlero.dashboard.dto.DataPoint;
import com.axlero.dashboard.dto.MetricQuery;
import com.axlero.dashboard.dto.MetricQuery.Aggregation;
import com.axlero.dashboard.dto.MetricQuery.Filter;
import com.axlero.dashboard.entity.SalesRecord;
import com.axlero.dashboard.exception.InvalidQueryException;
import com.axlero.dashboard.repository.SalesRecordRepository;
import com.axlero.dashboard.service.CachedMetricService;
import com.axlero.dashboard.service.MetricService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MetricServiceTest {
    @Autowired MetricService metrics;
    @Autowired CachedMetricService cached;
    @Autowired SalesRecordRepository sales;
    @Autowired CacheManager cacheManager;

    @Test
    void sumByRegionMatchesTotalRevenue() {
        List<DataPoint> points = metrics.execute(new MetricQuery("region", Aggregation.SUM, "amount", null));
        BigDecimal grouped = points.stream().map(DataPoint::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = sales.findAll().stream().map(SalesRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(points).hasSize(5);
        assertThat(grouped).isEqualByComparingTo(expected);
    }

    @Test
    void countWithoutDimensionReturnsSingleKpi() {
        List<DataPoint> points = metrics.execute(new MetricQuery(null, Aggregation.COUNT, null, null));
        assertThat(points).hasSize(1);
        assertThat(points.get(0).value()).isEqualByComparingTo(BigDecimal.valueOf(sales.count()));
    }

    @Test
    void filtersAreApplied() {
        MetricQuery q = new MetricQuery("category", Aggregation.COUNT, null, List.of(new Filter("region", "EQ", "North")));
        long expected = sales.findAll().stream().filter(s -> "North".equals(s.getRegion())).count();
        long actual = metrics.execute(q).stream().mapToLong(p -> p.value().longValue()).sum();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void sqlInjectionAttemptsAreRejected() {
        assertThatThrownBy(() -> metrics.execute(new MetricQuery("region; DROP TABLE app_user;--", Aggregation.SUM, "amount", null)))
                .isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> metrics.execute(new MetricQuery("region", Aggregation.SUM, "amount) FROM x --", null)))
                .isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> metrics.execute(new MetricQuery("region", Aggregation.COUNT, null,
                List.of(new Filter("region; DROP", "EQ", "x")))))
                .isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> metrics.execute(new MetricQuery("region", Aggregation.COUNT, null,
                List.of(new Filter("amount", "GT", "1 OR 1=1")))))
                .isInstanceOf(InvalidQueryException.class);
    }

    @Test
    void injectionInFilterValueIsTreatedAsPlainData() {
        MetricQuery q = new MetricQuery("region", Aggregation.COUNT, null,
                List.of(new Filter("region", "EQ", "North' OR '1'='1")));
        assertThat(metrics.execute(q)).isEmpty();
    }

    @Test
    void resultsAreCached() {
        MetricQuery q = new MetricQuery("product", Aggregation.AVG, "amount", null);
        List<DataPoint> first = cached.run(q);
        assertThat(cacheManager.getCache("metrics").get(q)).isNotNull();
        assertThat(cached.run(q)).isSameAs(first);   // same instance => served from cache
    }
}
