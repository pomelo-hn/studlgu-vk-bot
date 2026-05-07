package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceTest {

    @Test
    void addEventRequiresOnlyTitleAndDate() {
        EventRepository repository = mock(EventRepository.class);
        EventService eventService = new EventService(repository);

        eventService.addEvent("Экзамен", LocalDate.parse("2026-05-15"), null, "", "   ");

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(event ->
                event.getTitle().equals("Экзамен")
                        && event.getDate().equals(LocalDate.parse("2026-05-15"))
                        && event.getTime() == null
                        && event.getDescription() == null
                        && event.getLocation() == null
        ));
    }

    @Test
    void addEventRejectsBlankTitle() {
        EventService eventService = new EventService(mock(EventRepository.class));

        assertThatThrownBy(() -> eventService.addEvent(" ", LocalDate.parse("2026-05-15"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Название");
    }

    @Test
    void eventsForDateSortUntimedEventsAfterTimedEvents() {
        EventRepository repository = mock(EventRepository.class);
        EventService eventService = new EventService(repository);
        LocalDate date = LocalDate.parse("2026-05-15");
        Event untimed = new Event("1", "Без времени", date, null, null, null);
        Event timed = new Event("2", "С временем", date, LocalTime.parse("09:00"), null, null);
        when(repository.findAll()).thenReturn(List.of(untimed, timed));

        List<Event> events = eventService.getEventsForDate(date);

        assertThat(events).extracting(Event::getTitle)
                .containsExactly("С временем", "Без времени");
    }
}
