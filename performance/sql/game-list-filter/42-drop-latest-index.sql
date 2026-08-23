USE basketball;

DROP INDEX idx_game_list_latest
    ON game_entity;

ANALYZE TABLE game_entity;

SHOW INDEX FROM game_entity;