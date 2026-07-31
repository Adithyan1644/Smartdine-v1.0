package com.smartdine.config;

/**
 * =====================================================================
 * SmartDine — DataSource Routing Context Holder
 * =====================================================================
 * Thread-local context that determines which physical GCP Cloud SQL
 * database should service the current HTTP request thread.
 *
 * Routing keys:
 *   "DEV"  → smartdine_dev  (sandbox — for 🧪 Testing/Demo accounts)
 *   "PROD" → smartdine      (production — for 🟢 Live accounts)
 *
 * Default (when no key is explicitly set):
 *   - PROD on App Engine (detected via GAE_INSTANCE env var)
 *   - DEV  locally       (safe fallback for development)
 *
 * Usage pattern in controllers:
 *   DataSourceContextHolder.set(request.isTest() ? DEV : PROD);
 *   try {
 *       service.doWork();
 *   } finally {
 *       DataSourceContextHolder.clear();   // ALWAYS clear — prevents leaks
 *   }
 * =====================================================================
 */
public final class DataSourceContextHolder {

    /** Routing key for the GCP Cloud SQL sandbox database (smartdine_dev) */
    public static final String DEV  = "DEV";

    /** Routing key for the GCP Cloud SQL production database (smartdine) */
    public static final String PROD = "PROD";

    // InheritableThreadLocal so child threads (async tasks) inherit the context
    private static final ThreadLocal<String> CONTEXT = new InheritableThreadLocal<>();

    private DataSourceContextHolder() { /* utility class — no instantiation */ }

    /**
     * Sets the datasource routing key for this request thread.
     * MUST be called before any @Transactional method boundary — Spring opens
     * the DB connection when the transaction starts, so the key must already
     * be present in the ThreadLocal at that point.
     */
    public static void set(String key) {
        CONTEXT.set(key);
    }

    /**
     * Returns the active routing key for this thread.
     * Falls back to a smart default when no key has been explicitly set:
     *   - App Engine (cloud deploy)  → PROD
     *   - Local development machine  → DEV
     */
    public static String get() {
        String key = CONTEXT.get();
        if (key != null) {
            return key;
        }
        // Auto-detect runtime environment using GAE environment variables
        boolean onAppEngine = System.getenv("GAE_INSTANCE") != null
                           || System.getenv("GAE_ENV") != null;
        return onAppEngine ? PROD : DEV;
    }

    /**
     * Clears the thread-local context after the request completes.
     * Always invoke in a finally block to prevent stale keys from leaking
     * into subsequent requests handled by the same thread pool thread.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
