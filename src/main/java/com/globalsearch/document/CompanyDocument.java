package com.globalsearch.document;

import com.globalsearch.entity.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "companies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class    CompanyDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Text)
    private String industry;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String contactEmail;

    @Field(type = FieldType.Keyword)
    private String contactPhone;

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

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Integer)
    private Integer maxUsers;

    @Field(type = FieldType.Integer)
    private Integer maxLocations;

    @Field(type = FieldType.Integer)
    private Integer maxSensors;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd['T'HH:mm:ss[.SSS][.SS][.S]]")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd['T'HH:mm:ss[.SSS][.SS][.S]]")
    private LocalDateTime updatedAt;

    public static CompanyDocument fromEntity(Company company) {
        return CompanyDocument.builder()
                .id(company.getId())
                .name(company.getName())
                .tenantId(company.getTenantId())
                .industry(company.getIndustry())
                .description(company.getDescription())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .address(company.getAddress())
                .city(company.getCity())
                .state(company.getState())
                .country(company.getCountry())
                .postalCode(company.getPostalCode())
                .status(company.getStatus().name())
                .maxUsers(company.getMaxUsers())
                .maxLocations(company.getMaxLocations())
                .maxSensors(company.getMaxSensors())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
