package com.smartdine.coreheart;

import org.springframework.context.ApplicationEvent;

public class TableUpdateEvent extends ApplicationEvent {
    public TableUpdateEvent(Object source) {
        super(source);
    }
}
