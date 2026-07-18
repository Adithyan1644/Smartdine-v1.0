package com.smartdine.dto;

import java.util.List;
import java.util.UUID;

public class MergeTableRequest {
    private List<UUID> tableIds;
    private String notes;

    public MergeTableRequest() {}

    public List<UUID> getTableIds() {
        return tableIds;
    }

    public void setTableIds(List<UUID> tableIds) {
        this.tableIds = tableIds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
