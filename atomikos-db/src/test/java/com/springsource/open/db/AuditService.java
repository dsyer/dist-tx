package com.springsource.open.db;


public interface AuditService {

	void update(int id, String operation, String name);

}
