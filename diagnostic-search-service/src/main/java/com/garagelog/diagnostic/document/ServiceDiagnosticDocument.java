package com.garagelog.diagnostic.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "service_diagnostic_logs")
public class ServiceDiagnosticDocument {

    @Id
    private String recordId;

    @Field(type = FieldType.Keyword)
    private String vehicleId;

    @Field(type = FieldType.Keyword)
    private String plateNumber;

    @Field(type = FieldType.Text)
    private String brandModel;

    @Field(type = FieldType.Keyword)
    private String vehicleType;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate serviceDate;

    @Field(type = FieldType.Integer)
    private Integer odometerAtService;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text)
    private String workshopName;

    @Field(type = FieldType.Double)
    private BigDecimal cost;

    @Field(type = FieldType.Text)
    private String mechanicNotes;

    @Field(type = FieldType.Text)
    private String advisorMessage;

    @Field(type = FieldType.Boolean)
    private Boolean urgentMaintenanceFlag;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant indexedAt;
}
