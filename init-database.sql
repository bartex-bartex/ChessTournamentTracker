CREATE TABLE "users" (
  "user_id" int8,
  "username" text,
  "mail" text,
  "encrypted_password" text,
  "first_name" text,
  "last_name" text,
  "sex" text,
  "date_of_birth" date,
  "fide" int4,
"k" smallint DEFAULT 40,
  PRIMARY KEY ("user_id")
);

CREATE TABLE "tournaments" (
  "tournament_id" int8,
  "name" text,
  "location" text,
  "organiser" text,
  "time_control" text,
  "start_date" date,
  "end_date" date,
  "rounds" int4,
  "info" text,
  "tournament_state" smallint DEFAULT 0,
  PRIMARY KEY ("tournament_id")
);

CREATE TABLE "sessions" (
  "session_id" text,
  "user_id" int8,
  "date" timestamp,
  PRIMARY KEY ("session_id"),
  CONSTRAINT "fk_sessions.user_id"
    FOREIGN KEY ("user_id")
      REFERENCES "users"("user_id")
);

CREATE TABLE "matches" (
  "match_id" int8,
  "white_player_id" int8,
  "black_player_id" int8,
  "tournament_id" int8,
  "score" int4,
  "round" int4,
  "table" int8,
  "game_notation" text,
  PRIMARY KEY ("match_id"),
  CONSTRAINT "fk_matches.black_player_id"
    FOREIGN KEY ("black_player_id")
      REFERENCES "users"("user_id"),
  CONSTRAINT "fk_matches.white_player_id"
    FOREIGN KEY ("white_player_id")
      REFERENCES "users"("user_id"),
  CONSTRAINT "fk_matches.tournament_id"
    FOREIGN KEY ("tournament_id")
      REFERENCES "tournaments"("tournament_id")
);

CREATE TABLE "fide_changes" (
  "match_id" int8,
  "user_id" int8,
  "value" int4,
  CONSTRAINT "pk_fide_changes"
    PRIMARY KEY ("match_id", "user_id"),
  CONSTRAINT "fk_fide_changes.user_id"
    FOREIGN KEY ("user_id")
      REFERENCES "users"("user_id"),
  CONSTRAINT "fk_fide_changes.match_id"
    FOREIGN KEY ("match_id")
      REFERENCES "matches"("match_id")
);

CREATE TABLE "tournament_roles" (
  "user_id" int8,
  "tournament_id" int8,
  "role" text,
  "start_fide" int4,
 "bye" smallint DEFAULT 0,
 "k" smallint DEFAULT 40,
  CONSTRAINT "pk_tournament_roles"
    PRIMARY KEY ("user_id", "tournament_id"),
  CONSTRAINT "fk_tournament_roles.user_id"
    FOREIGN KEY ("user_id")
      REFERENCES "users"("user_id"),
  CONSTRAINT "fk_tournament_roles.tournament_id"
    FOREIGN KEY ("tournament_id")
      REFERENCES "tournaments"("tournament_id")
);
