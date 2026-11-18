package com.garagelog.core.dto;

import com.garagelog.core.entity.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCostBreakdown {
    private ServiceCategory category;
    private BigDecimal totalCost;
    private long transactionCount;
    private double percentageOfTotal;
}
