package com.globalsearch.document;

import com.globalsearch.entity.Dashboard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "dashboards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Long)
    private Long ownerId;

    @Field(type = FieldType.Keyword)
    private String ownerName;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String dashboardType;

    @Field(type = FieldType.Boolean)
    private Boolean isDefault;

    @Field(type = FieldType.Boolean)
    private Boolean isShared;

    @Field(type = FieldType.Boolean)
    private Boolean isFavorite;

    @Field(type = FieldType.Integer)
    private Integer refreshInterval;

    @Field(type = FieldType.Text)
    private String tags;

    @Field(type = FieldType.Integer)
    private Integer accessCount;

    @Field(type = FieldType.Date)
    private LocalDateTime lastAccessedAt;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    public static DashboardDocument fromEntity(Dashboard dashboard) {
        return DashboardDocument.builder()
                .id(dashboard.getId())
                .name(dashboard.getName())
                .tenantId(dashboard.getTenantId())
                .ownerId(dashboard.getOwnerId())
                .ownerName(dashboard.getOwnerName())
                .description(dashboard.getDescription())
                .dashboardType(dashboard.getDashboardType() != null ? dashboard.getDashboardType().name() : null)
                .isDefault(dashboard.getIsDefault())
                .isShared(dashboard.getIsShared())
                .isFavorite(dashboard.getIsFavorite())
                .refreshInterval(dashboard.getRefreshInterval())
                .tags(dashboard.getTags())
                .accessCount(dashboard.getAccessCount())
                .lastAccessedAt(dashboard.getLastAccessedAt())
                .createdAt(dashboard.getCreatedAt())
                .updatedAt(dashboard.getUpdatedAt())
                .build();
    }
}
