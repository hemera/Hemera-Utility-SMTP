package hemera.utility.smtp;

import java.io.UnsupportedEncodingException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * <code>Mail</code> defines implementation of Email
 * that can be sent via <code>SMTPService</code>.
 * It provides various convenient methods for setting
 * the parameters of an Email. It internally depends
 * on the <code>SMTPService</code> being connected,
 * thus an instance cannot be constructed until the
 * <code>SMTPService</code> is connected.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class Mail extends MimeMessage {

	/**
	 * Constructor of <code>Mail</code>.
	 * @throws MessagingException If establishing
	 * the connection failed.
	 */
	public Mail() throws MessagingException {
		super(SMTPService.instance.getSession());
	}
	
	/**
	 * Set the sender of this mail.
	 * @param address The <code>String</code> address
	 * of the sender.
	 * @param name The <code>String</code> name to be
	 * displayed as the email sender.
	 * @throws AddressException If address is invalid.
	 * @throws MessagingException If setting to this
	 * mail failed.
	 * @throws UnsupportedEncodingException If the
	 * given name cannot be encoded.
	 */
	public void setSender(final String address, final String name) throws AddressException, MessagingException, UnsupportedEncodingException {
		this.setFrom(new InternetAddress(address, name));
	}
	
	/**
	 * Set the given string as HTML content for this
	 * mail. This method assumes UTF-8 encoding is
	 * used as the current JVM encoding scheme.
	 * @param content The <code>String</code> HTML
	 * content to be set.
	 * @throws MessagingException If setting failed.
	 */
	public void setHTMLContent(final String content) throws MessagingException {
		super.setText(content, "UTF-8", "html");
		super.setHeader("Content-Transfer-Encoding", "quoted-printable");
	}
	
	/**
	 * Add a recipient of this mail.
	 * @param address The <code>String</code> address
	 * of the recipient.
	 * @throws AddressException If address is invalid.
	 * @throws MessagingException If setting to this
	 * mail failed.
	 */
	public void addRecipient(final String address) throws AddressException, MessagingException {
		this.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
	}
}
