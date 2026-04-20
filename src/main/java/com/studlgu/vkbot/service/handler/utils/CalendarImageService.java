package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CalendarImageService {

    private static final int MONTH_WIDTH = 840;
    private static final int MONTH_HEIGHT = 620;
    private static final int WEEK_WIDTH = 840;
    private static final int WEEK_HEIGHT = 280;

    private static final Color BG_COLOR = Color.WHITE;
    private static final Color GRID_COLOR = new Color(220, 220, 220);
    private static final Color HEADER_COLOR = new Color(50, 50, 50);
    private static final Color DAY_COLOR = new Color(60, 60, 60);
    private static final Color EVENT_DOT_COLOR = new Color(220, 50, 50);
    private static final Color EVENT_TEXT_COLOR = new Color(200, 30, 30);
    private static final Color WEEK_EVENT_BG = new Color(255, 235, 235);
    private static final Locale RUSSIAN = Locale.of("ru");

    private static final String[] DAYS_OF_WEEK = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    public byte[] generateMonthImage(YearMonth month, List<Event> events) {
        Set<Integer> eventDays = events.stream()
                .map(e -> e.getDate().getDayOfMonth())
                .collect(Collectors.toSet());

        Map<Integer, List<Event>> eventsByDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.getDate().getDayOfMonth()));

        BufferedImage image = new BufferedImage(MONTH_WIDTH, MONTH_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(BG_COLOR);
        g.fillRect(0, 0, MONTH_WIDTH, MONTH_HEIGHT);

        String title = capitalize(month.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, RUSSIAN))
                + " " + month.getYear();
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(HEADER_COLOR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (MONTH_WIDTH - fm.stringWidth(title)) / 2, 40);

        int cellW = MONTH_WIDTH / 7;
        int headerY = 70;
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(100, 100, 100));
        for (int i = 0; i < 7; i++) {
            String dayLabel = DAYS_OF_WEEK[i];
            fm = g.getFontMetrics();
            g.drawString(dayLabel, i * cellW + (cellW - fm.stringWidth(dayLabel)) / 2, headerY);
        }

        int gridStartY = 85;
        int cellH = (MONTH_HEIGHT - gridStartY - 10) / 6;
        LocalDate firstDay = month.atDay(1);
        int startDow = firstDay.getDayOfWeek().getValue() - 1;

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            int pos = day - 1 + startDow;
            int col = pos % 7;
            int row = pos / 7;
            int x = col * cellW;
            int y = gridStartY + row * cellH;

            g.setColor(GRID_COLOR);
            g.drawRect(x, y, cellW, cellH);

            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String dayStr = String.valueOf(day);

            if (eventDays.contains(day)) {
                int cx = x + cellW / 2;
                int cy = y + 18;
                g.setColor(EVENT_DOT_COLOR);
                g.fillOval(cx - 12, cy - 13, 24, 24);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(DAY_COLOR);
            }
            fm = g.getFontMetrics();
            g.drawString(dayStr, x + (cellW - fm.stringWidth(dayStr)) / 2, y + 18);

            List<Event> dayEvents = eventsByDay.get(day);
            if (dayEvents != null && !dayEvents.isEmpty()) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(EVENT_TEXT_COLOR);
                String eventTitle = truncate(dayEvents.getFirst().getTitle(), 12);
                fm = g.getFontMetrics();
                g.drawString(eventTitle, x + (cellW - fm.stringWidth(eventTitle)) / 2, y + 32);
            }
        }

        g.dispose();
        return toPng(image);
    }

    public byte[] generateWeekImage(LocalDate from, List<Event> events) {
        Map<LocalDate, List<Event>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(Event::getDate));

        BufferedImage image = new BufferedImage(WEEK_WIDTH, WEEK_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(BG_COLOR);
        g.fillRect(0, 0, WEEK_WIDTH, WEEK_HEIGHT);

        int cellW = WEEK_WIDTH / 7;
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("d MMM", RUSSIAN);

        for (int i = 0; i < 7; i++) {
            LocalDate day = from.plusDays(i);
            int x = i * cellW;
            List<Event> dayEvents = eventsByDate.getOrDefault(day, List.of());

            if (!dayEvents.isEmpty()) {
                g.setColor(WEEK_EVENT_BG);
                g.fillRect(x, 0, cellW, WEEK_HEIGHT);
            }

            g.setColor(GRID_COLOR);
            g.drawLine(x, 0, x, WEEK_HEIGHT);

            String dowLabel = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, RUSSIAN);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(HEADER_COLOR);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(capitalize(dowLabel), x + (cellW - fm.stringWidth(capitalize(dowLabel))) / 2, 22);

            String dateLabel = day.format(dayFmt);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            fm = g.getFontMetrics();
            g.drawString(dateLabel, x + (cellW - fm.stringWidth(dateLabel)) / 2, 40);

            g.setColor(GRID_COLOR);
            g.drawLine(x, 48, x + cellW, 48);

            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int textY = 64;
            for (Event event : dayEvents) {
                g.setColor(EVENT_TEXT_COLOR);
                String label = truncate(event.getTitle(), 11);
                fm = g.getFontMetrics();
                g.drawString(label, x + (cellW - fm.stringWidth(label)) / 2, textY);
                textY += 16;
                if (textY > WEEK_HEIGHT - 10) break;
            }
        }

        g.dispose();
        return toPng(image);
    }

    private byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode image to PNG", e);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
