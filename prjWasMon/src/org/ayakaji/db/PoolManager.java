package org.ayakaji.db;

/** 
 * Title: ConnectPool.java 
 * Description: ���ݿ���� 
 * Copyright: Copyright (c) 2002/12/25 
 * Company: 
 * Author : 
 * remark : ����ָ��ع� 
 * Version 2.0 
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

public class PoolManager extends ConnectionPool {

	private ConnectionPool connMgr;
	private Statement stmt;
	private Connection conn;
	private ResultSet rs;

	/**
	 * �������ӳ�ʼ��
	 * */

	public Connection getPool(String name) throws Exception {
		try {
			connMgr = ConnectionPool.getInstance();
			conn = connMgr.getConnection(name);
		} catch (Exception e) {
			System.err.println("���ܴ�������!�볢������Ӧ�÷�����");

		}
		return conn;
	}

	/**
	 * 2008-10-14 �������ӳ�ʼ��
	 */
	public Connection getPool(String name, String url) throws Exception {
		try {
			connMgr = ConnectionPool.getInstance(url);
			conn = connMgr.getConnection(name);
		} catch (Exception e) {
			System.err.println("���ܴ�������!�볢������Ӧ�÷�����");

		}
		return conn;
	}

	/**
	 * ͬ���Ϸ���,�������ӿ��еȴ�ʱ�� ���÷���
	 * */

	public Connection getPool_t(String name, long time) throws Exception {
		try {
			connMgr = ConnectionPool.getInstance();
			conn = connMgr.getConnection(name, time);
		} catch (Exception e) {
			System.err.println("���ܴ�������!");

		}
		return conn;
	}

	/**
	 * ִ�в�ѯ����1
	 * */
	public ResultSet executeQuery(String SqlStr) throws Exception {
		ResultSet result = null;
		try {
			stmt = conn.createStatement();
			result = stmt.executeQuery(SqlStr);
			// here add one line by jnma 12.11
			conn.commit();
		} catch (java.sql.SQLException e) {
			throw new Exception("ִ�в�ѯ������");
		}
		return result;
	}

	/**
	 * ִ�в�ѯ����2
	 * */
	public ResultSet getRst(String SqlStr) throws Exception {
		// ResultSet result = null;
		try {
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery(SqlStr);
			// here add one line by jnma 12.11
			conn.commit();
		} catch (java.sql.SQLException e) {
			throw new Exception("ִ�в�ѯ������");
		}
		return rs;
	}

	/**
	 * ִ�и���
	 * */
	public int Update(String SqlStr) throws Exception {
		int result = -1;
		try {
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
			result = stmt.executeUpdate(SqlStr);
			// here add one line by jnma 12.11
			conn.commit();
			if (result == 0) {
				System.out.println("ִ��delete,update,insert SQL����");
			}
		} catch (java.sql.SQLException e) {
			System.err.println("ִ��delete,update,insert SQL����");
		}
		return result;
	}

	/**
	 * ִ��������
	 * */
	public boolean handleTransaction(Vector<?> SqlArray) throws Exception {
		boolean result = false;
		int ArraySize = SqlArray.size();
		try {
			stmt = conn.createStatement();
			conn.setAutoCommit(false);
			System.out.println("ArraySize is" + ArraySize);
			for (int i = 0; i < ArraySize; i++) {
				System.out.println(" ��ʼִ�����" + (String) SqlArray.elementAt(i));
				stmt.executeUpdate((String) SqlArray.elementAt(i));
				System.out.println(" ִ�гɹ�");
			}
			conn.commit();
			conn.setAutoCommit(true); // ����
			System.out.println("����ִ�гɹ�");
			result = true;
		} catch (java.sql.SQLException e) {
			try {
				System.out.println(e.toString());
				System.out.println("���ݿ����ʧ��");
				conn.rollback();
			} catch (java.sql.SQLException Te) {
				System.err.println("�������ع��쳣");
			}
		}
		try {
			conn.setAutoCommit(true);
		} catch (java.sql.SQLException e) {
			System.err.println("�����Զ��ύʧ��");
		}
		return result;
	}

	/**
	 * �ͷ�����
	 * */
	public void close(String name) throws Exception {
		try {
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				connMgr.freeConnection(name, conn);

				System.out.println(" [c �����ͷ�һ������ ] ");

			}
		} catch (java.sql.SQLException e) {
			System.err.println("�ͷ����ӳ���");
		}
	}

}