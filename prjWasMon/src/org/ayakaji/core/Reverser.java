package org.ayakaji.core;
import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class Reverser {

	public static void main(String[] args) throws NotFoundException,
			CannotCompileException, IOException {
		reverseAdminClientImpl();
	}

	/**
	 * 拦截AdminClient::invoke()调用,此处为jython调用入口
	 * 
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 * @throws IOException
	 */
	public static void reverseAdminClientImpl() throws NotFoundException,
			CannotCompileException, IOException {
		ClassPool pool = ClassPool.getDefault();
		CtClass cc = pool.get("com.ibm.ws.management.AdminClientImpl");
		for (CtMethod m : cc.getMethods()) {
			if (m.getName().equals("invoke")) {
				m.insertBefore("{ java.lang.System.out.println(\"[\"); "
						+ "java.lang.System.out.println(\" \" + $1.toString()); "
						+ "java.lang.System.out.println(\" \" + $2); "
						+ "if ($3 != null) java.lang.System.out.println(\" \" + java.util.Arrays.asList($3).toString()); "
						+ "if ($4 != null) java.lang.System.out.println(\" \" + java.util.Arrays.asList($4).toString()); "
						+ "java.lang.System.out.println(\"]\"); }");
			}
		}
		cc.writeFile();
		cc.detach();
	}

}
