package com.springsource.open.foo;

public interface Handler {

	void handle(String msg, long offset);
	
	void resetItemCount();

	int getItemCount();
	
}
