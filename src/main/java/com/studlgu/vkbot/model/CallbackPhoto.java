package com.studlgu.vkbot.model;

import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CallbackPhoto {
	private Long id;
	private Long ownerId;
	private String accessKey;
	private CallbackOrigPhoto origPhoto;
}
