package com.finlearn.rankingservice.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.UUID;

// 시즌 컨텍스트 외부 참조용 식별자 VO
@Getter
@EqualsAndHashCode
public class SeasonId {

    private final UUID value;

    private SeasonId(UUID value) {
        if (value == null) throw new IllegalArgumentException("SeasonId 값은 null일 수 없습니다.");
        this.value = value;
    }

    public static SeasonId of(UUID value) {
        return new SeasonId(value);
    }

    public static SeasonId of(String value) {
        return new SeasonId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
