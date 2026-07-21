package com.smartdine.coreheart;

import java.util.UUID;

public class TenantContext {
	
	private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
	private static volatile UUID activeRestaurantId = null;

	private static boolean isCloudMode() {
		return System.getenv("GAE_INSTANCE") != null || System.getenv("GAE_ENV") != null;
	}
	
	public static void setRestaurantId(UUID restaurantId) {
		currentTenant.set(restaurantId);
		if (restaurantId != null && !isCloudMode()) {
			activeRestaurantId = restaurantId;
		}
	}
	
	public static void setActiveRestaurantId(UUID restaurantId) {
		if (!isCloudMode()) {
			activeRestaurantId = restaurantId;
		}
	}
	
	public static UUID getRestaurantId() {
		if (activeRestaurantId != null && !isCloudMode()) {
			return activeRestaurantId;
		}
		UUID id = currentTenant.get();
		if (id != null) {
			return id;
		}
		return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
	}
	
	public static void clear() {
		currentTenant.remove();
	}

}
