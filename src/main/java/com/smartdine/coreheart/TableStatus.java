package com.smartdine.coreheart;

public enum TableStatus {
	 AVAILABLE,      // Green
	 RUNNING,        // Orange (KOT is active)
	 PAYMENT_PENDING,// Red (Bill printed, waiting for cash)
	 PAID           // Blue (Settled, but table not yet cleared)
   }