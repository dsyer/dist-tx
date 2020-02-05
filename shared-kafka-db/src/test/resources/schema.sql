DROP TABLE T_FOOS;
create table T_FOOS (
	id integer not null primary key,
	name varchar(80) not null,
	foo_date timestamp,
	unique(name)
);
DROP TABLE T_OFFSETS;
create table T_OFFSETS (
	id integer not null primary key,
	topic varchar(80) not null,
	part int,
	offset int,
	unique(topic, part, offset)
);
