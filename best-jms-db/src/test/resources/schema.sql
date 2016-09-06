DROP TABLE T_FOOS;
create table T_FOOS (
	id integer not null primary key,
	name varchar(80) not null,
	foo_date timestamp,
	unique(name)
);
