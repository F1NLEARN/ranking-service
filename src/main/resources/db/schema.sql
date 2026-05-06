-- ============================================================
-- rankingdb 스키마
-- Hibernate ddl-auto: update 가 자동 생성하므로 직접 실행 불필요
-- 테이블을 초기화하거나 수동으로 재생성할 때 사용
-- ============================================================

-- 연결 대상: rankingdb
-- \c rankingdb

-- ──────────────────────────────────────────────────────────────
-- 테이블 삭제 (초기화용)
-- ──────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS ranking_badges CASCADE;
DROP TABLE IF EXISTS rankings CASCADE;

-- ──────────────────────────────────────────────────────────────
-- rankings
--   시즌 진행 중: rank IS NULL (Redis가 실시간 소스, 이 테이블은 유저 VO 스냅샷)
--   시즌 종료 후: rank IS NOT NULL (Redis 데이터를 확정 저장)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE rankings (
    ranking_id          UUID            PRIMARY KEY,
    season_id           UUID            NOT NULL,
    season_number       INT             NOT NULL,
    user_id             UUID            NOT NULL,
    user_nickname       VARCHAR(50)     NOT NULL,
    user_profile_image  VARCHAR(500),
    rank                INT,
    ranking_type        VARCHAR(20)     NOT NULL CHECK (ranking_type IN ('ALL', 'STOCK', 'ETF', 'ACHIEVEMENT')),
    score               DECIMAL(10, 4)  NOT NULL DEFAULT 0,
    last_updated_at     TIMESTAMP,
    created_at          TIMESTAMP,
    created_by          UUID,
    updated_at          TIMESTAMP,
    updated_by          UUID,
    deleted_at          TIMESTAMP,
    deleted_by          UUID,

    CONSTRAINT uk_rankings_season_user_type UNIQUE (season_id, user_id, ranking_type)
);

-- ──────────────────────────────────────────────────────────────
-- ranking_badges
--   시즌 종료 시 ALL 랭킹 기준 상위 50% 유저에게 지급
-- ──────────────────────────────────────────────────────────────
CREATE TABLE ranking_badges (
    ranking_badge_id    UUID        PRIMARY KEY,
    season_id           UUID        NOT NULL,
    season_number       INT         NOT NULL,
    user_id             UUID        NOT NULL,
    user_nickname       VARCHAR(50) NOT NULL,
    grade               VARCHAR(20) NOT NULL CHECK (grade IN ('CHAMPION', 'GOLD', 'SILVER', 'BRONZE')),
    paid_at             TIMESTAMP   NOT NULL,
    created_at          TIMESTAMP,
    created_by          UUID,
    updated_at          TIMESTAMP,
    updated_by          UUID,
    deleted_at          TIMESTAMP,
    deleted_by          UUID
);
