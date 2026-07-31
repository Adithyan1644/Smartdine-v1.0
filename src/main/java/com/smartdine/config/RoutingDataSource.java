package com.smartdine.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * =====================================================================
 * SmartDine — Dynamic Routing DataSource
 * =====================================================================
 * Extends Spring's AbstractRoutingDataSource to dynamically route each
 * database connection to either the DEV or PROD GCP Cloud SQL database.
 *
 * The routing decision is made ONCE per connection acquisition — when
 * Spring's @Transactional opens a connection at the start of a transaction.
 * All repository calls within that transaction use the same physical
 * connection, guaranteeing atomicity within a single database.
 *
 * Routing key is sourced from DataSourceContextHolder (ThreadLocal):
 *   "DEV"  → devDataSource  (smartdine_dev — sandbox)
 *   "PROD" → prodDataSource (smartdine     — production)
 *   null   → defaultTarget  (devDataSource — safe local fallback)
 * =====================================================================
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    /**
     * Called by Spring each time a new connection is requested.
     * Returns the lookup key that maps to the physical DataSource to use.
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String key = DataSourceContextHolder.get();
        System.out.println("[RoutingDataSource] Routing connection → " + key
                + " (" + (DataSourceContextHolder.DEV.equals(key)
                    ? "smartdine_dev (sandbox)"
                    : "smartdine (production)") + ")");
        return key;
    }
}
