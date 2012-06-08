package hemera.utility.smtp.enumn;

import java.util.concurrent.TimeUnit;

import hemera.core.utility.config.TimeData;

/**
 * <code>ESMTPConfig</code> defines the enumeration
 * of all the configuration values needed to support
 * the <code>SMTPService</code>. Each enumeration is
 * associated with an <code>Object</code> value. The
 * value reference guarantees its memory consistency.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public enum ESMTPConfig {
	/**
	 * The <code>String</code> SMTP host address.
	 */
	SMTPHost,
	/**
	 * The <code>Integer</code> SMTP host port number.
	 */
	SMTPPort,
	/**
	 * The <code>String</code> SMTP host authentication
	 * user name.
	 */
	SMTP_Auth_Username,
	/**
	 * The <code>String</code> SMTP host authentication
	 * password.
	 */
	SMTP_Auth_Password,
	/**
	 * The <code>Boolean</code> flag indicating if the
	 * SMTP host authentication requires a command of
	 * <code>STARTTLS</code> for establishing connection.
	 */
	SMTP_Auth_RequireSTARTTLS,
	/**
	 * The <code>TimeData</code> SMTP service timeout
	 * value. The default value is 10 seconds.
	 */
	SMTP_Timeout(new TimeData(10, TimeUnit.SECONDS));
	
	/**
	 * The <code>Object</code> value associated with
	 * the enumeration.
	 */
	private volatile Object value;
	
	/**
	 * Constructor of <code>ESMTPConfig</code>.
	 */
	private ESMTPConfig() {
		this.value = null;
	}

	/**
	 * Constructor of <code>ESMTPConfig</code>.
	 * @param value The default <code>Object</code>
	 * value.
	 */
	private ESMTPConfig(final Object value) {
		this.value = value;
	}
	
	/**
	 * Set the value associated with the enumeration.
	 * <p>
	 * This method guarantees the memory consistency
	 * of the value.
	 * @param value The <code>Object</code> value to
	 * be set.
	 */
	public void set(final Object value) {
		this.value = value;
	}
	
	/**
	 * Retrieve the value associated with enumeration.
	 * <p>
	 * This method guarantees the memory consistency
	 * of the value.
	 * @return The <code>Object</code> value.
	 */
	public Object value() {
		return this.value;
	}
}
