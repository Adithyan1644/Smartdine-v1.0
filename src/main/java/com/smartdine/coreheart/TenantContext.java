package com.smartdine.coreheart;

import java.util.UUID;

public class TenantContext {
	
	private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
	private static volatile UUID activeRestaurantId = null;
	
	public static void setRestaurantId(UUID restaurantId) {
		currentTenant.set(restaurantId);
		if (restaurantId != null) {
			activeRestaurantId = restaurantId;
		}
	}
	
	public static void setActiveRestaurantId(UUID restaurantId) {
		activeRestaurantId = restaurantId;
	}
	
	public static UUID getRestaurantId() {
		UUID id = currentTenant.get();
		if (id != null) {
			return id;
		}
		if (activeRestaurantId != null) {
			return activeRestaurantId;
		}
		return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
	}
	
	public static void clear() {
		currentTenant.remove();
	}

}
