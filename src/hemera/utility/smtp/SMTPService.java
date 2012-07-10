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

import hemera.core.utility.data.TimeData;
import hemera.core.utility.logging.FileLogger;

/**
 * <code>SMTPService</code> defines the implementation
 * of a singleton utility unit that is responsible for
 * sending of <code>Mail</code> via SMTP protocol.
 * <p>
 * <code>SMTPService</code> provides the thread-safety
 * guarantees by relying on the underlying Java library
 * units, which provides complete synchronization.
 * <p>
 * <code>SMTPService</code> will attempt to establish
 * a connection with the specified service provider if
 * there is no existing connection or the existing one
 * has timed-out. It caches the connected host for all
 * subsequent mail sending unless the service invoked
 * to connect to a different host. In which case, the
 * new host will be cached and replace the old one.
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
	 * The <code>String</code> of current host address.
	 */
	private String host;
	/**
	 * The <code>int</code> current host port.
	 */
	private int port;
	/**
	 * The <code>String</code> current host login user
	 * name.
	 */
	private String username;
	/**
	 * The <code>String</code> current host login
	 * password.
	 */
	private String password;
	/**
	 * The <code>boolean</code> current host login
	 * require-TLS flag.
	 */
	private boolean requireTLS;
	/**
	 * The <code>TimeData</code> of current host timeout.
	 */
	private TimeData timeout;
	/**
	 * The <code>Session</code> instance used to create
	 * the <code>Mail</code> instances.
	 */
	private Session session;
	/**
	 * The <code>Transport</code> instance used to
	 * send the <code>Mail</code>.
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
	 * Connect the SMTP service to the service provider
	 * based on specified values if it is not already
	 * connected.
	 * @param host The <code>String</code> host address.
	 * @param port The <code>int</code> port to connect.
	 * @param username The <code>String</code> login
	 * user name.
	 * @param password The <code>String</code> login
	 * password.
	 * @param requireTLS <code>true</code> if login
	 * requires TLS. <code>false</code> otherwise.
	 * @param timeout The <code>TimeData</code> of the
	 * connection timeout.
	 * @return <code>true</code> if the service connected
	 * to the specified host. <code>false</code> if the
	 * service is already connected to the host.
	 */
	public boolean connect(final String host, final int port, final String username,
			final String password, final boolean requireTLS, final TimeData timeout) {
		this.writelock.lock();
		try {
			// Already connected.
			if (this.host != null && this.host.equals(host)) return false;
			// Connect to new host.
			else {
				this.host = host;
				this.port = port;
				this.username = username;
				this.password = password;
				this.requireTLS = requireTLS;
				this.timeout = timeout;
				return this.connectToCurrentHost();
			}
		} finally {
			this.writelock.unlock();
		}
	}

	/**
	 * Connect to the current host.
	 * @return <code>true</code> if connection succeeded.
	 * <code>false</code> otherwise.
	 */
	private boolean connectToCurrentHost() {
		this.writelock.lock();
		try {
			// Check JVM encoding.
			final String encoding = Charset.defaultCharset().displayName();
			if (!encoding.equals("UTF-8") && !encoding.equals("UTF-16")) {
				this.logger.warning("JVM encoding is not UTF-8 nor UTF-16. Mail contents may not be encoded correctly.");
			}
			// Create session.
			final long timeoutmilli = this.timeout.unit.toMillis(this.timeout.value);
			final Properties properties = new Properties();
			properties.put("mail.transport.protocol", "smtp");
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.starttls.enable", String.valueOf(this.requireTLS));
			properties.put("mail.smtp.timeout", timeoutmilli);
			properties.put("mail.smtp.connectiontimeout", timeoutmilli);
			this.session = Session.getInstance(properties);
			// Retrieve and connect transport.
			try {
				this.transport = this.session.getTransport("smtp");
				this.transport.connect(this.host, this.port, this.username, this.password);
			} catch (final Exception e) {
				this.logger.severe("Failed to connect SMTP Transport");
				this.logger.exception(e);
				return false;
			}
			return true;
		} finally {
			this.writelock.unlock();
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
				this.host = null;
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
			// Not initialized yet.
			if (this.host == null) return null;
			// Timed out, reconnect.
			else if (!this.transport.isConnected()) {
				this.connectToCurrentHost();
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
	private Transport getTransport() {
		this.readlock.lock();
		try {
			// Not initialized yet.
			if (this.host == null) return null;
			// Timed out, reconnect.
			else if (!this.transport.isConnected()) {
				this.connectToCurrentHost();
			}
			return this.transport;
		} finally {
			this.readlock.unlock();
		}
	}
}
