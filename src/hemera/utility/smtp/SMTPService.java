package hemera.utility.smtp;

import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import hemera.core.utility.config.TimeData;
import hemera.core.utility.logging.FileLogger;
import hemera.utility.smtp.enumn.ESMTPConfig;

/**
 * <code>SMTPService</code> defines the implementation
 * of a singleton utility unit that is responsible for
 * sending of <code>Mail</code> via SMTP protocol.
 * <p>
 * <code>SMTPService</code> depends on the values in
 * <code>ESMTPConfig</code> for establishing connection
 * with the service provider.
 * <p>
 * <code>SMTPService</code> provides the thread-safety
 * guarantees by relying on the underlying Java library
 * units, which provides complete synchronization.
 * <p>
 * <code>SMTPService</code> will attempt to establish
 * a connection with the specified service provider if
 * there is no existing connection or the existing one
 * has timed-out.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public enum SMTPService {
	/**
	 * The singleton instance.
	 */
	instance;
	
	/**
	 * The <code>FileLogger</code> instance.
	 */
	private final FileLogger logger;
	/**
	 * The <code>ReadLock</code> used to guard the data
	 * field access.
	 */
	private final ReadLock readlock;
	/**
	 * The <code>WriteLock</code> used to guard the
	 * connect process.
	 */
	private final WriteLock writelock;
	/**
	 * The <code>Session</code> instance used to create
	 * the <code>Mail</code> instances.
	 * <p>
	 * This field is guarded by the read-write lock
	 * and only created once. Direct access to this
	 * field should never occur. Rather the method
	 * <code>getSession</code> should be used.
	 */
	private Session session;
	/**
	 * The <code>Transport</code> instance used to
	 * send the <code>Mail</code>.
	 * <p>
	 * This field is guarded by the read-write lock
	 * and only created once. Direct access to this
	 * field should never occur. Rather the method
	 * <code>getTransport</code> should be used.
	 */
	private Transport transport;
	
	/**
	 * Constructor of <code>SMTPService</code>.
	 */
	private SMTPService() {
		this.logger = FileLogger.getLogger(this.getClass());
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		this.readlock = lock.readLock();
		this.writelock = lock.writeLock();
	}
	
	/**
	 * Connect the SMTP service to its service provider
	 * based on configuration values specified in the
	 * <code>ESMTPConfig</code>.
	 * <p>
	 * If the transport is already connected, this method
	 * returns directly.
	 * <p>
	 * This method relies on external locking. It does
	 * not lock internally, though write-lock must be
	 * obtained first before invoking this method. It
	 * only provides the connection logic.
	 */
	private void connect() {
		// Check JVM encoding.
		final String encoding = Charset.defaultCharset().displayName();
		if (!encoding.equals("UTF-8") && !encoding.equals("UTF-16")) {
			this.logger.warning("JVM encoding is not UTF-8 nor UTF-16. Mail contents may not be encoded correctly.");
		}
		// Retrieve configuration values.
		final String host = (String)ESMTPConfig.SMTPHost.value();
		final int port = (Integer)ESMTPConfig.SMTPPort.value();
		final String username = (String)ESMTPConfig.SMTP_Auth_Username.value();
		final String password = (String)ESMTPConfig.SMTP_Auth_Password.value();
		final Boolean requiresTLS = (Boolean)ESMTPConfig.SMTP_Auth_RequireSTARTTLS.value();
		final TimeData timeout = (TimeData)ESMTPConfig.SMTP_Timeout.value();
		final long timeoutmilli = timeout.unit.toMillis(timeout.value);
		// Create session.
		final Properties properties = new Properties();
		properties.put("mail.transport.protocol", "smtp");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", requiresTLS.toString());
		properties.put("mail.smtp.timeout", timeoutmilli);
		properties.put("mail.smtp.connectiontimeout", timeoutmilli);
		this.session = Session.getInstance(properties);
		// Retrieve and connect transport.
		try {
			this.transport = this.session.getTransport("smtp");
			this.transport.connect(host, port, username, password);
		} catch (final Exception e) {
			this.logger.severe("Failed to connect SMTP Transport");
			this.logger.exception(e);
		}
	}
	
	/**
	 * Send the given mail using the SMTP service.
	 * @param mail The <code>Mail</code> to be sent.
	 * @throws MessagingException If sending failed.
	 */
	public void sendMail(final Mail mail) throws MessagingException {
		if (mail == null) return;
		final Transport transport = this.getTransport();
		transport.sendMessage(mail, mail.getRecipients(Message.RecipientType.TO));
	}
	
	/**
	 * Disconnect the SMTP service from its provider.
	 * If the SMTP service is not connected, this method
	 * returns directly.
	 */
	public void disconnect() {
		this.writelock.lock();
		try {
			if (this.transport == null || !this.transport.isConnected()) return;
			try {
				this.transport.close();
				this.session = null;
				this.transport = null;
			} catch (final Exception e) {
				this.logger.severe("Failed to disconnect SMTP Transport");
				this.logger.exception(e);
			}
			this.logger.info("SMTP service disconnected");
		} finally {
			this.writelock.unlock();
		}
	}
	
	/**
	 * Retrieve the session instance. The returned
	 * instance is guaranteed to be valid and its
	 * transport is connected.
	 * @return The <code>Session</code> instance.
	 */
	public Session getSession() {
		this.readlock.lock();
		try {
			if (this.transport == null || !this.transport.isConnected()) {
				this.readlock.unlock();
				this.writelock.lock();
				try {
					if (this.transport == null || !this.transport.isConnected()) {
						this.connect();
					}
				} finally {
					this.readlock.lock();
					this.writelock.unlock();
				}
			}
			return this.session;
		} finally {
			this.readlock.unlock();
		}
	}
	
	/**
	 * Retrieve the transport instance. The returned
	 * instance is guaranteed to be connected.
	 * @return The <code>Transport</code> instance.
	 */
	public Transport getTransport() {
		this.readlock.lock();
		try {
			if (this.transport == null || !this.transport.isConnected()) {
				this.readlock.unlock();
				this.writelock.lock();
				try {
					if (this.transport == null || !this.transport.isConnected()) {
						this.connect();
					}
				} finally {
					this.readlock.lock();
					this.writelock.unlock();
				}
			}
			return this.transport;
		} finally {
			this.readlock.unlock();
		}
	}
}
