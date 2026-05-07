package com.studlgu.vkbot.service.handler.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Accessors(chain = true)
public class EventDraft {
    private String title;
    private LocalDate date;
    private LocalTime time;
    private String description;
    private String location;
}
