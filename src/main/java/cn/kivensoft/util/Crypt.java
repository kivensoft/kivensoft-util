package cn.kivensoft.util;

import cn.kivensoft.util.impl.BCrypt;
import cn.kivensoft.util.impl.Md5Crypt;

/** 口令加密及判断接口
 * @author kiven lee
 * @version 1.0
 */
public interface Crypt {
	/** 加密类型 */
	enum CryptType { MD5, BCrypt }

	/** 创建具体加密实现类的静态函数 */
	static Crypt create(CryptType cryptType) {
		switch (cryptType) {
			case MD5: return new Md5Crypt();
			case BCrypt: return new BCrypt();
			default: return new Md5Crypt();
		}
	}

	/** 口令加密函数
	 * @param passwd 需要加密的口令
	 * @return 加密过后的口令
	 */
	String encrypt(String passwd);

	/** 口令比对函数
	 * @param passwd 需要比对的口令
	 * @param encryptString 加密过后的口令
	 * @return true: 口令比对成功, false: 口令比对失败
	 */
	boolean verification(String passwd, String encryptString);
}
