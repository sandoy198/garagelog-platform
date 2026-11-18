package com.garagelog.diagnostic.repository;

import com.garagelog.diagnostic.document.ServiceDiagnosticDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceDiagnosticRepository extends ElasticsearchRepository<ServiceDiagnosticDocument, String> {

    List<ServiceDiagnosticDocument> findByUrgentMaintenanceFlagTrueOrderByServiceDateDescIndexedAtDesc();
}
