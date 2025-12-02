package com.globalsearch.document;

import com.globalsearch.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(indexName = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Text)
    private String address;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Keyword)
    private String postalCode;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Double)
    private Double totalArea;

    @Field(type = FieldType.Keyword)
    private String timeZone;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String managerName;

    @Field(type = FieldType.Keyword)
    private String managerEmail;

    @Field(type = FieldType.Keyword)
    private String managerPhone;

    // Security fields
    @Field(type = FieldType.Long)
    private Long companyId;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Date)
    private LocalDate createdAt;

    @Field(type = FieldType.Date)
    private LocalDate updatedAt;

    public static LocationDocument fromEntity(Location location) {
        GeoPoint geoPoint = null;
        if (location.getLatitude() != null && location.getLongitude() != null) {
            geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        }

        return LocationDocument.builder()
                .id(location.getId())
                .name(location.getName())
                .type(location.getType())
                .description(location.getDescription())
                .address(location.getAddress())
                .city(location.getCity())
                .state(location.getState())
                .country(location.getCountry())
                .postalCode(location.getPostalCode())
                .location(geoPoint)
                .totalArea(location.getTotalArea())
                .timeZone(location.getTimeZone())
                .status(location.getStatus().name())
                .managerName(location.getManagerName())
                .managerEmail(location.getManagerEmail())
                .managerPhone(location.getManagerPhone())
                .companyId(location.getCompany() != null ? location.getCompany().getId() : null)
                .tenantId(location.getCompany() != null ? location.getCompany().getTenantId() : null)
                .createdAt(location.getCreatedAt() != null ? location.getCreatedAt().toLocalDate() : null)
                .updatedAt(location.getUpdatedAt() != null ? location.getUpdatedAt().toLocalDate() : null)
                .build();
    }
}
