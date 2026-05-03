package com.finlearn.rankingservice.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

// 유저 컨텍스트 외부 참조용 식별자 VO
@Getter
@EqualsAndHashCode
public class UserId {

    private final UUID value;

    private UserId(UUID value) {
        if (value == null) throw new IllegalArgumentException("UserId 값은 null일 수 없습니다.");
        this.value = value;
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
