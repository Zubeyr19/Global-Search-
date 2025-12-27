package com.globalsearch.document;

import com.globalsearch.entity.Report;
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

@Document(indexName = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Keyword)
    private String reportType;  

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String filePath;

    @Field(type = FieldType.Long)
    private Long fileSize;

    @Field(type = FieldType.Keyword)
    private String mimeType;

    @Field(type = FieldType.Long)
    private Long createdBy;

    @Field(type = FieldType.Date)
    private LocalDate createdAt;

    @Field(type = FieldType.Date)
    private LocalDate updatedAt;

    @Field(type = FieldType.Text)
    private String tags;

    @Field(type = FieldType.Boolean)
    private Boolean isPublic;

    @Field(type = FieldType.Long)
    private Long executionTimeMs;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date)
    private LocalDateTime scheduledAt;

    @Field(type = FieldType.Date)
    private LocalDateTime expiresAt;

    public static ReportDocument fromEntity(Report report) {
        return ReportDocument.builder()
                .id(report.getId())
                .name(report.getName())
                .tenantId(report.getTenantId())
                .reportType(report.getReportType())
                .description(report.getDescription())
                .filePath(report.getFilePath())
                .fileSize(report.getFileSize())
                .mimeType(report.getMimeType())
                .createdBy(report.getCreatedBy())
                .createdAt(report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate() : null)
                .updatedAt(report.getUpdatedAt() != null ? report.getUpdatedAt().toLocalDate() : null)
                .tags(report.getTags())
                .isPublic(report.getIsPublic())
                .executionTimeMs(report.getExecutionTimeMs())
                .status(report.getStatus() != null ? report.getStatus().name() : null)
                .scheduledAt(report.getScheduledAt())
                .expiresAt(report.getExpiresAt())
                .build();
    }
}
