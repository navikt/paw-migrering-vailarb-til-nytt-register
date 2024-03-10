create table Group_Id
(
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    group_id varchar(255) UNIQUE NOT NULL
);
