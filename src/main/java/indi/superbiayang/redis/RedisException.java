package indi.superbiayang.redis;


/**
 * @author SuperbiaYang
 */
public class RedisException extends Exception {
	private static final long serialVersionUID = -5937033443241507841L;

	public RedisException(String message) {
        super(message);
    }

    public RedisException(String message, Throwable cause) {
        super(message, cause);
    }

}
