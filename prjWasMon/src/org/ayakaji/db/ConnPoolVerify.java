package org.ayakaji.db;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public final class ConnPoolVerify {
	public static void main(String[] args) throws NamingException {
		Context ctx = new InitialContext();
		System.out.println(ctx.getEnvironment());
	}

}
