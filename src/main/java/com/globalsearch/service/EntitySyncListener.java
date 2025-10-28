package com.globalsearch.service;

import com.globalsearch.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;

/**
 * JPA Entity Listener that automatically syncs entities to Elasticsearch
 * whenever they are created, updated, or deleted.
 *
 * This ensures MySQL and Elasticsearch stay in sync in real-time.
 */
@Component
@Slf4j
public class EntitySyncListener {

    private static ElasticsearchSyncService syncService;

    @Autowired
    public void setSyncService(ElasticsearchSyncService syncService) {
        EntitySyncListener.syncService = syncService;
    }

    /**
     * Sync entity after persist (create)
     */
    @PostPersist
    public void onPostPersist(Object entity) {
        if (syncService == null) return;

        if (entity instanceof Company) {
            Company company = (Company) entity;
            log.debug("Company persisted, syncing to Elasticsearch: {}", company.getId());
            syncService.syncCompany(company);
        } else if (entity instanceof Location) {
            Location location = (Location) entity;
            log.debug("Location persisted, syncing to Elasticsearch: {}", location.getId());
            syncService.syncLocation(location);
        } else if (entity instanceof Zone) {
            Zone zone = (Zone) entity;
            log.debug("Zone persisted, syncing to Elasticsearch: {}", zone.getId());
            syncService.syncZone(zone);
        } else if (entity instanceof Sensor) {
            Sensor sensor = (Sensor) entity;
            log.debug("Sensor persisted, syncing to Elasticsearch: {}", sensor.getId());
            syncService.syncSensor(sensor);
        } else if (entity instanceof Report) {
            Report report = (Report) entity;
            log.debug("Report persisted, syncing to Elasticsearch: {}", report.getId());
            syncService.syncReport(report);
        } else if (entity instanceof Dashboard) {
            Dashboard dashboard = (Dashboard) entity;
            log.debug("Dashboard persisted, syncing to Elasticsearch: {}", dashboard.getId());
            syncService.syncDashboard(dashboard);
        }
    }

    /**
     * Sync entity after update
     */
    @PostUpdate
    public void onPostUpdate(Object entity) {
        if (syncService == null) return;

        if (entity instanceof Company) {
            Company company = (Company) entity;
            log.debug("Company updated, syncing to Elasticsearch: {}", company.getId());
            syncService.syncCompany(company);
        } else if (entity instanceof Location) {
            Location location = (Location) entity;
            log.debug("Location updated, syncing to Elasticsearch: {}", location.getId());
            syncService.syncLocation(location);
        } else if (entity instanceof Zone) {
            Zone zone = (Zone) entity;
            log.debug("Zone updated, syncing to Elasticsearch: {}", zone.getId());
            syncService.syncZone(zone);
        } else if (entity instanceof Sensor) {
            Sensor sensor = (Sensor) entity;
            log.debug("Sensor updated, syncing to Elasticsearch: {}", sensor.getId());
            syncService.syncSensor(sensor);
        } else if (entity instanceof Report) {
            Report report = (Report) entity;
            log.debug("Report updated, syncing to Elasticsearch: {}", report.getId());
            syncService.syncReport(report);
        } else if (entity instanceof Dashboard) {
            Dashboard dashboard = (Dashboard) entity;
            log.debug("Dashboard updated, syncing to Elasticsearch: {}", dashboard.getId());
            syncService.syncDashboard(dashboard);
        }
    }

    /**
     * Delete entity from Elasticsearch before removal
     */
    @PreRemove
    public void onPreRemove(Object entity) {
        if (syncService == null) return;

        if (entity instanceof Company) {
            Company company = (Company) entity;
            log.debug("Company being removed, deleting from Elasticsearch: {}", company.getId());
            syncService.deleteCompany(company.getId());
        } else if (entity instanceof Location) {
            Location location = (Location) entity;
            log.debug("Location being removed, deleting from Elasticsearch: {}", location.getId());
            syncService.deleteLocation(location.getId());
        } else if (entity instanceof Zone) {
            Zone zone = (Zone) entity;
            log.debug("Zone being removed, deleting from Elasticsearch: {}", zone.getId());
            syncService.deleteZone(zone.getId());
        } else if (entity instanceof Sensor) {
            Sensor sensor = (Sensor) entity;
            log.debug("Sensor being removed, deleting from Elasticsearch: {}", sensor.getId());
            syncService.deleteSensor(sensor.getId());
        } else if (entity instanceof Report) {
            Report report = (Report) entity;
            log.debug("Report being removed, deleting from Elasticsearch: {}", report.getId());
            syncService.deleteReport(report.getId());
        } else if (entity instanceof Dashboard) {
            Dashboard dashboard = (Dashboard) entity;
            log.debug("Dashboard being removed, deleting from Elasticsearch: {}", dashboard.getId());
            syncService.deleteDashboard(dashboard.getId());
        }
    }
}
