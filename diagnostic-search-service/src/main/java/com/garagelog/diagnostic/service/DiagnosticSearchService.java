package com.garagelog.diagnostic.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.garagelog.diagnostic.document.ServiceDiagnosticDocument;
import com.garagelog.diagnostic.repository.ServiceDiagnosticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosticSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ServiceDiagnosticRepository diagnosticRepository;

    public List<ServiceDiagnosticDocument> searchDiagnostics(String keyword, String category, Boolean urgentOnly) {
        BoolQuery.Builder boolBuilder = QueryBuilders.bool();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasKeyword) {
            String trimmed = keyword.trim();
            String wildcardTerm = "*" + trimmed.toLowerCase(Locale.ROOT).replaceAll("[*?\"~()\\[\\]{}:\\\\/]", "") + "*";

            Query textSearchQuery = QueryBuilders.bool(b -> b
                    .should(s -> s.multiMatch(mm -> mm
                            .query(trimmed)
                            .fields("mechanicNotes^2", "advisorMessage")
                            .fuzziness("AUTO")
                    ))
                    .should(s -> s.queryString(qs -> qs
                            .query(wildcardTerm)
                            .fields("mechanicNotes", "advisorMessage")
                    ))
                    .minimumShouldMatch("1")
            );
            boolBuilder.must(textSearchQuery);
        }

        if (category != null && !category.isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t
                    .field("category")
                    .value(category.trim().toUpperCase(Locale.ROOT))
            ));
        }

        if (Boolean.TRUE.equals(urgentOnly)) {
            boolBuilder.filter(f -> f.term(t -> t
                    .field("urgentMaintenanceFlag")
                    .value(true)
            ));
        }

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(boolBuilder.build()._toQuery());

        if (!hasKeyword) {
            queryBuilder.withSort(SortOptions.of(s -> s.field(f -> f.field("serviceDate").order(SortOrder.Desc))));
            queryBuilder.withSort(SortOptions.of(s -> s.field(f -> f.field("indexedAt").order(SortOrder.Desc))));
        }

        SearchHits<ServiceDiagnosticDocument> hits = elasticsearchOperations.search(
                queryBuilder.build(),
                ServiceDiagnosticDocument.class
        );

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public List<ServiceDiagnosticDocument> getUrgentAlerts() {
        return diagnosticRepository.findByUrgentMaintenanceFlagTrueOrderByServiceDateDescIndexedAtDesc();
    }
}
