package hemera.utility.smtp;

import hemera.core.utility.logging.FileLogger;

import java.nio.charset.Charset;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * <code>SMTPTransport</code> defines the implementation
 * of a transport unit that allows sending messages via
 * the SMTP protocol.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class SMTPTransport {
	/**
	 * The <code>FileLogger</code> instance.
	 */
	private final FileLogger logger;
	/**
	 * The <code>Session</code> instance.
	 */
	final Session session;
	/**
	 * The <code>Transport</code> instance used to
	 * send <code>Mail</code>.
	 */
	final Transport transport;

	/**
	 * Constructor of <code>SMTPTransport</code>.
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
	public SMTPTransport(final String host, final int port, final String username,
			final String password, final boolean requireTLS) throws MessagingException {
		this.logger = FileLogger.getLogger(this.getClass());
		// Check JVM encoding.
		final String encoding = Charset.defaultCharset().displayName();
		if (!encoding.equals("UTF-8") && !encoding.equals("UTF-16")) {
			this.logger.warning("JVM encoding is not UTF-8 nor UTF-16. Mail contents may not be encoded correctly.");
		}
		// Create session.
		final Properties properties = new Properties();
		properties.put("mail.transport.protocol", "smtp");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", String.valueOf(requireTLS));
		this.session = Session.getInstance(properties);
		// Retrieve and connect transport.
		this.transport = this.session.getTransport("smtp");
		this.transport.connect(host, port, username, password);
	}
	
	/**
	 * Send the given mail using this SMTP transport.
	 * @param mail The <code>Mail</code> to be sent.
	 * @throws MessagingException If sending failed.
	 */
	public void sendMail(final Mail mail) throws MessagingException {
		if (mail == null) return;
		this.transport.sendMessage(mail, mail.getRecipients(Message.RecipientType.TO));
	}
	
	/**
	 * Disconnect the SMTP transport from its provider.
	 * @throws MessagingException If closing connection
	 * failed.
	 */
	public void disconnect() throws MessagingException {
		this.transport.close();
	}
}
