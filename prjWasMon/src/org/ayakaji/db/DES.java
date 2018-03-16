package org.ayakaji.db;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

public class DES {
	private Key key; // 密钥
	private static String oriKey = "RrJ98zkhvoLprKhYTXhiJa8rkGigbPhfGNpv46MUClssw2CYYq";

	/**
	 * 根据参数生成KEY
	 * 
	 * @param strKey
	 *            密钥字符串
	 */
	public void getKey(String strKey) {
		try {
			KeyGenerator _generator = KeyGenerator.getInstance("DES");
			_generator.init(new SecureRandom(strKey.getBytes()));
			this.key = _generator.generateKey();
			_generator = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 加密String明文输入,String密文输出
	 * 
	 * @param strMing
	 *            String明文
	 * @return String密文
	 */
	public String getEncString(String strMing) {
		String strMi = "";
		try {
			/*
			 * byteMing = strMing.getBytes("UTF8"); byteMi =
			 * this.getEncCode(byteMing); strMi = new String(byteMi, "UTF8");
			 */
			strMi = parseByte2HexStr(getEncCode(strMing.getBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return strMi;
	}

	/**
	 * 解密
	 * 
	 * @param strMi
	 * @return
	 */
	public String getDesString(String strMi) {
		String strMing = "";
		try {
			strMing = new String(getDesCode(parseHexStr2Byte(strMi)));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return strMing;
	}

	/**
	 * 加密以byte[]明文输入,byte[]密文输出
	 * 
	 * @param byteS
	 *            byte[]明文
	 * @return byte[]密文
	 */
	private byte[] getEncCode(byte[] byteS) {
		byte[] byteFina = null;
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byteFina = cipher.doFinal(byteS);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cipher = null;
		}
		return byteFina;
	}

	/**
	 * 解密以byte[]密文输入,以byte[]明文输出
	 * 
	 * @param byteD
	 *            byte[]密文
	 * @return byte[]明文
	 */
	private byte[] getDesCode(byte[] byteD) {
		Cipher cipher;
		byte[] byteFina = null;
		try {
			cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byteFina = cipher.doFinal(byteD);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cipher = null;
		}
		return byteFina;
	}

	/**
	 * 将二进制转换成16进制
	 * 
	 * @param buf
	 * @return
	 */
	public String parseByte2HexStr(byte buf[]) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buf.length; i++) {
			String hex = Integer.toHexString(buf[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			sb.append(hex.toUpperCase());
		}
		return sb.toString();
	}

	/**
	 * 将16进制转换为二进制
	 * 
	 * @param hexStr
	 * @return
	 */
	public byte[] parseHexStr2Byte(String hexStr) {
		if (hexStr.length() < 1)
			return null;
		byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++) {
			int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2),
					16);
			result[i] = (byte) (high * 16 + low);
		}
		return result;
	}

	/**
	 * 获取原始加密密钥,固定在程序中,本方法无需运行
	 * 
	 * @param length
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String getPasswdCryptKey(int length) {
		String base = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rand = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int idx = rand.nextInt(base.length() - 1);
			sb.append(base.charAt(idx));
		}
		System.out.println(sb.toString());
		return sb.toString();
	}

	public static void main(String[] args) {
		DES des = new DES();
		des.getKey(oriKey);
		String strEnc = des.getEncString("emma");
		System.out.println(strEnc);
		String strDes = des.getDesString(strEnc);
		System.out.println(strDes);
	}

}