package com.globalsearch.document;

import com.globalsearch.entity.Sensor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(indexName = "sensors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String serialNumber;

    @Field(type = FieldType.Keyword)
    private String sensorType;

    @Field(type = FieldType.Keyword)
    private String manufacturer;

    @Field(type = FieldType.Keyword)
    private String model;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date)
    private LocalDateTime lastReadingTime;

    @Field(type = FieldType.Double)
    private Double lastReadingValue;

    @Field(type = FieldType.Keyword)
    private String unitOfMeasurement;

    @Field(type = FieldType.Integer)
    private Integer readingInterval;

    @Field(type = FieldType.Double)
    private Double alertThresholdMin;

    @Field(type = FieldType.Double)
    private Double alertThresholdMax;

    @Field(type = FieldType.Integer)
    private Integer batteryLevel;

    @Field(type = FieldType.Date)
    private LocalDateTime installationDate;

    @Field(type = FieldType.Date)
    private LocalDateTime lastMaintenanceDate;

    // Security fields
    @Field(type = FieldType.Long)
    private Long zoneId;

    @Field(type = FieldType.Long)
    private Long locationId;

    @Field(type = FieldType.Long)
    private Long companyId;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Date)
    private LocalDate createdAt;

    @Field(type = FieldType.Date)
    private LocalDate updatedAt;

    public static SensorDocument fromEntity(Sensor sensor) {
        Long locationId = null;
        Long companyId = null;
        String tenantId = null;

        if (sensor.getZone() != null) {
            if (sensor.getZone().getLocation() != null) {
                locationId = sensor.getZone().getLocation().getId();
                if (sensor.getZone().getLocation().getCompany() != null) {
                    companyId = sensor.getZone().getLocation().getCompany().getId();
                    tenantId = sensor.getZone().getLocation().getCompany().getTenantId();
                }
            }
        }

        return SensorDocument.builder()
                .id(sensor.getId())
                .name(sensor.getName())
                .serialNumber(sensor.getSerialNumber())
                .sensorType(sensor.getSensorType().name())
                .manufacturer(sensor.getManufacturer())
                .model(sensor.getModel())
                .description(sensor.getDescription())
                .status(sensor.getStatus().name())
                .lastReadingTime(sensor.getLastReadingTime())
                .lastReadingValue(sensor.getLastReadingValue())
                .unitOfMeasurement(sensor.getUnitOfMeasurement())
                .readingInterval(sensor.getReadingInterval())
                .alertThresholdMin(sensor.getAlertThresholdMin())
                .alertThresholdMax(sensor.getAlertThresholdMax())
                .batteryLevel(sensor.getBatteryLevel())
                .installationDate(sensor.getInstallationDate())
                .lastMaintenanceDate(sensor.getLastMaintenanceDate())
                .zoneId(sensor.getZone() != null ? sensor.getZone().getId() : null)
                .locationId(locationId)
                .companyId(companyId)
                .tenantId(tenantId)
                .createdAt(sensor.getCreatedAt() != null ? sensor.getCreatedAt().toLocalDate() : null)
                .updatedAt(sensor.getUpdatedAt() != null ? sensor.getUpdatedAt().toLocalDate() : null)
                .build();
    }
}
