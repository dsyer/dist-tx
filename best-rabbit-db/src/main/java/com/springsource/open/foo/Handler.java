package com.springsource.open.foo;

public interface Handler {

	void handle(String msg);
	
	void resetItemCount();

	int getItemCount();
	
}
