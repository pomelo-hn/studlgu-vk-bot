package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;

    public List<Event> getEventsForWeek(LocalDate from) {
        LocalDate end = from.plusDays(7);
        return repository.findAll().stream()
                .filter(e -> !e.getDate().isBefore(from) && e.getDate().isBefore(end))
                .sorted(Comparator.comparing(Event::getDate).thenComparing(Event::getTime))
                .toList();
    }

    public List<Event> getEventsForMonth(YearMonth month) {
        return repository.findAll().stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(month))
                .sorted(Comparator.comparing(Event::getDate).thenComparing(Event::getTime))
                .toList();
    }

    public void addEvent(String rawInput) {
        String[] parts = rawInput.split("\\|");
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                    "Неверный формат. Ожидается: название|YYYY-MM-DD|HH:mm|описание|место");
        }
        try {
            Event event = new Event(
                    null,
                    parts[0].trim(),
                    LocalDate.parse(parts[1].trim()),
                    LocalTime.parse(parts[2].trim()),
                    parts[3].trim(),
                    parts[4].trim()
            );
            repository.save(event);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Неверный формат даты или времени. Дата: YYYY-MM-DD, Время: HH:mm");
        }
    }

    public boolean deleteEventByPrefix(String idPrefix) {
        return repository.deleteByIdPrefix(idPrefix);
    }

    public List<Event> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Event::getDate).thenComparing(Event::getTime))
                .toList();
    }
}
