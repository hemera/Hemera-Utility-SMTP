package hemera.utility.smtp;

import hemera.core.utility.logging.FileLogger;

import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * <code>SMTPService</code> defines the implementation
 * of a singleton transport service that allows sending
 * messages via the SMTP protocol.
 * <p>
 * <code>SMTPService</code> provides necessary thread
 * safety measures to ensure that the service can be
 * accessed in different threads.
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
	 * The <code>ReadLock</code> of the transport.
	 */
	private final ReadLock readLock;
	/**
	 * The <code>WriteLock</code> of the transport.
	 */
	private final WriteLock writeLock;
	/**
	 * The <code>String</code> current SMTP host address.
	 */
	private String host;
	/**
	 * The <code>int</code> current SMTP host port.
	 */
	private int port;
	/**
	 * The <code>String</code> current SMTP login user
	 * name.
	 */
	private String username;
	/**
	 * The <code>String</code> current SMTP login
	 * password.
	 */
	private String password;
	/**
	 * The <code>boolean</code> current TLS flag.
	 */
	private boolean requireTLS;
	/**
	 * The <code>Session</code> instance.
	 */
	private Session session;
	/**
	 * The <code>Transport</code> instance used to
	 * send <code>Mail</code>.
	 */
	private Transport transport;
	
	/**
	 * Constructor of <code>SMTPService</code>.
	 */
	private SMTPService() {
		this.logger = FileLogger.getLogger(this.getClass());
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
	}

	/**
	 * Connect the service to the given host.
	 * @param host The <code>String</code> SMTP host
	 * address.
	 * @param port The <code>int</code> host port to
	 * connect to.
	 * @param username The <code>String</code> login
	 * user name.
	 * @param password The <code>String</code> login
	 * password.
	 * @param requireTLS <code>true</code> if login
	 * requires TLS. <code>false</code> otherwise.
	 * @throws MessagingException If establishing the
	 * SMTP connection failed.
	 */
	public void connect(final String host, final int port, final String username,
			final String password, final boolean requireTLS) throws MessagingException {
		this.writeLock.lock();
		try {
			this.host = host;
			this.port = port;
			this.username = username;
			this.password = password;
			this.requireTLS = requireTLS;
			this.connectToCurrentHost();
		} finally {
			this.writeLock.unlock();
		}
	}
	
	/**
	 * Connect to the current host.
	 * <p>
	 * This method acquires the write lock.
	 * @throws MessagingException If establishing the
	 * SMTP connection failed.
	 */
	private void connectToCurrentHost() throws MessagingException {
		this.writeLock.lock();
		try {
			// Disconenct old transport if necessary.
			if (this.transport != null) {
				this.disconnect();
			}
			// Check JVM encoding.
			final String encoding = Charset.defaultCharset().displayName();
			if (!encoding.equals("UTF-8") && !encoding.equals("UTF-16")) {
				this.logger.warning("JVM encoding is not UTF-8 nor UTF-16. Mail contents may not be encoded correctly.");
			}
			// Create session.
			final Properties properties = new Properties();
			properties.put("mail.transport.protocol", "smtp");
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.starttls.enable", String.valueOf(this.requireTLS));
			this.session = Session.getInstance(properties);
			// Retrieve and connect transport.
			this.transport = this.session.getTransport("smtp");
			this.transport.connect(this.host, this.port, this.username, this.password);
		} finally {
			this.writeLock.unlock();
		}
	}
	
	/**
	 * Send the given mail using this SMTP transport.
	 * @param mail The <code>Mail</code> to be sent.
	 * @throws MessagingException If sending failed.
	 */
	public void sendMail(final Mail mail) throws MessagingException {
		if (mail == null) return;
		this.checkConnection();
		this.readLock.lock();
		try {
			this.transport.sendMessage(mail, mail.getRecipients(Message.RecipientType.TO));
		} finally {
			this.readLock.unlock();
		}
	}
	
	/**
	 * Disconnect the SMTP transport from its provider.
	 * @throws MessagingException If closing connection
	 * failed.
	 */
	public void disconnect() throws MessagingException {
		this.writeLock.lock();
		try {
			this.transport.close();
			this.session = null;
			this.transport = null;
		} finally {
			this.writeLock.unlock();
		}
	}
	
	/**
	 * Check if the connection is still valid and
	 * reconnect if not.
	 * <p>
	 * This method acquires the write lock.
	 * @throws MessagingException If establishing
	 * the connection failed.
	 */
	private void checkConnection() throws MessagingException {
		this.writeLock.lock();
		try {
			if (this.transport == null) {
				throw new MessagingException("SMTP service is not yet created.");
			} else if (!this.transport.isConnected()) {
				this.connectToCurrentHost();
			}
		} finally {
			this.writeLock.unlock();
		}
	}
	
	/**
	 * Retrieve the session instance.
	 * @return The <code>Session</code> instance.
	 * @throws MessagingException If establishing
	 * the connection failed.
	 */
	Session getSession() throws MessagingException {
		this.checkConnection();
		this.readLock.lock();
		try {
			return this.session;
		} finally {
			this.readLock.unlock();
		}
	}
}
