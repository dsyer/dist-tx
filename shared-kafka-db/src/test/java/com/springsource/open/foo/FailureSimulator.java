package com.springsource.open.foo;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FailureSimulator {

	/**
	 * Just throws a {@link DataIntegrityViolationException}.
	 */
	public void simulateBusinessProcessingFailure() {
		throw new DataIntegrityViolationException("Simulated failure.");
	}

	@AfterReturning("execution(* *..*Handler+.handle(String,..)) && args(msg, ..)")
	public void maybeFail(String msg) {
		if (msg.contains("fail")) {
			System.err.println("Failing...");
			simulateBusinessProcessingFailure();
		}
	}

}
