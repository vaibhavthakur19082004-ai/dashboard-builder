package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.DataPoint;
import com.axlero.dashboard.dto.MetricQuery;
import com.axlero.dashboard.dto.MetricQuery.Filter;
import com.axlero.dashboard.entity.SalesRecord;
import com.axlero.dashboard.exception.InvalidQueryException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Dynamic query engine. Builds GROUP BY / aggregate queries with the JPA Criteria API.
 *
 * SQL-injection defence (defence in depth):
 *  1. Every field name must be in a fixed whitelist (never concatenated into a query string).
 *  2. Aggregation + operator are enums / fixed sets.
 *  3. Filter values are parsed to typed values and bound as parameters by Hibernate.
 */
@Service
public class MetricService {
    public static final List<String> DIMENSIONS = List.of("region", "product", "category", "channel");
    public static final List<String> MEASURES = List.of("amount", "quantity");
    public static final List<String> OPERATORS = List.of("EQ", "NE", "GT", "GTE", "LT", "LTE");
    private static final int MAX_ROWS = 100;
    private static final int MAX_FILTERS = 5;

    @PersistenceContext
    private EntityManager em;

    public Map<String, Object> schema() {
        return Map.of("dimensions", DIMENSIONS, "measures", MEASURES, "filterFields",
                List.of("region", "product", "category", "channel", "amount", "quantity"),
                "aggregations", Arrays.stream(MetricQuery.Aggregation.values()).map(Enum::name).toList(),
                "operators", OPERATORS);
    }

    public void validate(MetricQuery q) {
        if (q == null || q.aggregation() == null) throw new InvalidQueryException("aggregation is required");
        if (q.dimension() != null && !q.dimension().isBlank() && !DIMENSIONS.contains(q.dimension()))
            throw new InvalidQueryException("Unknown dimension: " + safe(q.dimension()));
        boolean count = q.aggregation() == MetricQuery.Aggregation.COUNT;
        if (!count && (q.measure() == null || !MEASURES.contains(q.measure())))
            throw new InvalidQueryException("measure must be one of " + MEASURES);
        if (count && q.measure() != null && !q.measure().isBlank() && !MEASURES.contains(q.measure()))
            throw new InvalidQueryException("Unknown measure: " + safe(q.measure()));
        List<Filter> filters = q.filters() == null ? List.of() : q.filters();
        if (filters.size() > MAX_FILTERS) throw new InvalidQueryException("At most " + MAX_FILTERS + " filters allowed");
        for (Filter f : filters) {
            if (f == null || f.field() == null || !(DIMENSIONS.contains(f.field()) || MEASURES.contains(f.field())))
                throw new InvalidQueryException("Unknown filter field: " + safe(f == null ? null : f.field()));
            if (f.op() == null || !OPERATORS.contains(f.op()))
                throw new InvalidQueryException("Unknown filter operator: " + safe(f.op()));
            if (f.value() == null || f.value().length() > 100)
                throw new InvalidQueryException("Filter value is required (max 100 chars)");
            if (MEASURES.contains(f.field())) numeric(f);
            if (DIMENSIONS.contains(f.field()) && !(f.op().equals("EQ") || f.op().equals("NE")))
                throw new InvalidQueryException("Text fields only support EQ and NE");
        }
    }

    @Transactional(readOnly = true)
    public List<DataPoint> execute(MetricQuery q) {
        validate(q);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<SalesRecord> root = cq.from(SalesRecord.class);

        Expression<? extends Number> agg = aggregate(cb, root, q);
        boolean grouped = q.dimension() != null && !q.dimension().isBlank();
        if (grouped) {
            Path<String> dim = root.get(q.dimension());
            cq.multiselect(dim, agg).groupBy(dim).orderBy(cb.asc(dim));
        } else {
            cq.multiselect(cb.literal("Total"), agg);
        }
        cq.where(predicates(cb, root, q).toArray(new Predicate[0]));

        List<DataPoint> out = new ArrayList<>();
        for (Object[] row : em.createQuery(cq).setMaxResults(MAX_ROWS).getResultList()) {
            out.add(new DataPoint(String.valueOf(row[0]), toDecimal(row[1])));
        }
        return out;
    }

    private Expression<? extends Number> aggregate(CriteriaBuilder cb, Root<SalesRecord> root, MetricQuery q) {
        if (q.aggregation() == MetricQuery.Aggregation.COUNT) return cb.count(root);
        Path<Number> m = root.get(q.measure());
        return switch (q.aggregation()) {
            case SUM -> cb.sum(m);
            case AVG -> cb.avg(m);
            case MIN -> cb.min(m);
            case MAX -> cb.max(m);
            default -> throw new InvalidQueryException("Unsupported aggregation");
        };
    }

    private List<Predicate> predicates(CriteriaBuilder cb, Root<SalesRecord> root, MetricQuery q) {
        List<Predicate> list = new ArrayList<>();
        if (q.filters() == null) return list;
        for (Filter f : q.filters()) {
            if (DIMENSIONS.contains(f.field())) {
                Path<String> p = root.get(f.field());
                list.add(f.op().equals("EQ") ? cb.equal(p, f.value()) : cb.notEqual(p, f.value()));
            } else {
                Path<Number> p = root.get(f.field());
                Number n = numeric(f);
                list.add(switch (f.op()) {
                    case "EQ" -> cb.equal(p, n);
                    case "NE" -> cb.notEqual(p, n);
                    case "GT" -> cb.gt(p, n);
                    case "GTE" -> cb.ge(p, n);
                    case "LT" -> cb.lt(p, n);
                    default -> cb.le(p, n);
                });
            }
        }
        return list;
    }

    private Number numeric(Filter f) {
        try {
            return f.field().equals("quantity") ? (Number) Integer.valueOf(f.value().trim()) : new BigDecimal(f.value().trim());
        } catch (NumberFormatException e) {
            throw new InvalidQueryException("Filter value for '" + f.field() + "' must be a number");
        }
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        BigDecimal d = v instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) v).doubleValue());
        return d.setScale(2, RoundingMode.HALF_UP);
    }

    private String safe(String s) {
        if (s == null) return "null";
        return s.length() > 30 ? s.substring(0, 30) + "..." : s;
    }
}
