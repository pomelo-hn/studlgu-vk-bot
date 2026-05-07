package com.studlgu.vkbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Appeal {
    private String id;
    private Long userId;
    private String text;
    private List<String> photoUrls;
    private AppealStatus status;
    private Instant createdAt;
    private Instant answeredAt;
    private Long answeredByUserId;
    private String answerText;
}
