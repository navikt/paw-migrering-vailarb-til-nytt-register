drop table Group_Id;

alter table Hendelser add column group_id varchar(255) NOT NULL default '0';

create index idx_group_id on Hendelser(group_id);