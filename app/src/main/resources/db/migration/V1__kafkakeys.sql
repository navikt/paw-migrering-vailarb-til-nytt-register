create table Hendelser
(
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tidspunkt timestamp NOT NULL,
    hendelse bytea NOT NULL
);

create index idx_tidspunkt on Hendelser(tidspunkt);