package com.globalsearch.document;

import com.globalsearch.entity.Zone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Integer)
    private Integer floorNumber;

    @Field(type = FieldType.Double)
    private Double areaSize;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Double)
    private Double temperatureMin;

    @Field(type = FieldType.Double)
    private Double temperatureMax;

    @Field(type = FieldType.Double)
    private Double humidityMin;

    @Field(type = FieldType.Double)
    private Double humidityMax;

    @Field(type = FieldType.Boolean)
    private Boolean alertEnabled;

    // Security fields
    @Field(type = FieldType.Long)
    private Long locationId;

    @Field(type = FieldType.Long)
    private Long companyId;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    public static ZoneDocument fromEntity(Zone zone) {
        Long companyId = null;
        String tenantId = null;
        if (zone.getLocation() != null && zone.getLocation().getCompany() != null) {
            companyId = zone.getLocation().getCompany().getId();
            tenantId = zone.getLocation().getCompany().getTenantId();
        }

        return ZoneDocument.builder()
                .id(zone.getId())
                .name(zone.getName())
                .type(zone.getType())
                .description(zone.getDescription())
                .floorNumber(zone.getFloorNumber())
                .areaSize(zone.getAreaSize())
                .status(zone.getStatus().name())
                .temperatureMin(zone.getTemperatureMin())
                .temperatureMax(zone.getTemperatureMax())
                .humidityMin(zone.getHumidityMin())
                .humidityMax(zone.getHumidityMax())
                .alertEnabled(zone.getAlertEnabled())
                .locationId(zone.getLocation() != null ? zone.getLocation().getId() : null)
                .companyId(companyId)
                .tenantId(tenantId)
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }
}
