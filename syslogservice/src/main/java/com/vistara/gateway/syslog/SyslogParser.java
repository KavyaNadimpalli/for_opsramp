package com.vistara.gateway.syslog;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A Syslog protocol parser.
 *
 * It should be capable of parsing RFC 3164 (BSD syslog) streams as well as RFC
 * 5424 (defined in 2009.) They are widely different, so a few if-statements
 * look awful, most prominently the is-this-a-version-number-or-a-year check.
 *
 * Since the tag and PID parts of RFC 3164 are highly optional and part of the
 * MSG, rather than the header, parsing them can be turned off.
 *
 * This is basically an InputStream, so it takes an InputStream as primary
 * argument.
 *
 * Since Flume hostnames are Strings, we also give the user an opportunity to
 * specify the character set used.
 *
 * XXX: We need push backs, and this is implemented here for now. Maybe inherit
 * PushbackInputStream instead?
 */
public class SyslogParser implements Closeable {
	// These two are compatible with flume.handlers.syslog.SyslogConsts
	public static final String SYSLOG_FACILITY = "syslogfacility";
	public static final String SYSLOG_SEVERITY = "syslogseverity";
	public static final String UNEXPECTED_CHARACTER = "Unexpected syslog character: ";

	/// Version 0 is the RFC 3164 format.

	// As defined in RFC 5424.
	public static final int MAX_SUPPORTED_VERSION = 1;

	private InputStream in;
	private boolean parseTag;
	private static Charset charset = StandardCharsets.UTF_8;

	/// Push back buffer. -1 indicates that it is empty.
	private int pushBack = -1;

	/**
	 * Construct a new Syslog protocol parser.
	 *
	 * Tags are parsed, and the encoding is assumed to be UTF-8.
	 */
	public SyslogParser(InputStream in) {
		this(in, true, charset);
	}

	/**
	 * Construct a new Syslog protocol parser.
	 *
	 * @param in       the stream to read data from. The InputStream#read() function
	 *                 is heavily used, so make sure it is buffered.
	 * @param parseTag true to parse the "tag[pid]:", false to leave it as part of
	 *                 the message body.
	 * @param encodingType the encoding to use for various string conversions, most
	 *                 notably the hostname.
	 */
	public SyslogParser(InputStream in, boolean parseTag, Charset encodingType) {
		this.in = in;
		this.parseTag = parseTag;
	}

	/**
	 * Free the resources used by this parser.
	 *
	 * Note that the parser cannot be reused. Closes the underlying input stream.
	 */
	public void close() throws IOException {
		in.close();
	}

	/**
	 * Read the next Syslog message from the stream.
	 * 
	 * @return
	 *
	 * @return a Flume Event, or null on EOF.
	 * @throw EOFException if EOF is found in an inappropriate place.
	 * @throw IOException if the underlying stream fails, or unexpected bytes are
	 *        seen.
	 */
	public SyslogEvent readEvent() throws IOException {
		int priority = 0;
		boolean isPriorityAvailable = false;
		int c = read(false);

		// Return null on initial EOF.
		if (c == -1)
			return null;

		if (c == '<') {
			isPriorityAvailable = true;
			priority = readInt();

			expect('>');
		}

		int version = 0;
		Calendar cal = null;

		if (Character.isDigit(peek(true))) {
			// Assume ISO date and time
			int y = readInt();

			c = read(true);

			if (c == ' ') {
				// Assume this is a RFC 5424 message.
				version = y;

				if (version > MAX_SUPPORTED_VERSION)
					throw new IOException(UNEXPECTED_CHARACTER + version);

				skipSpaces();
				y = readInt();
				expect('-');
			} else if (c != '-') {
				throw new IOException(UNEXPECTED_CHARACTER + (char) c);
			}

			int m = readInt();
			expect('-');
			int d = readInt();

			c = read(true);

			if (c != 'T' && c != ' ')
				throw new IOException(UNEXPECTED_CHARACTER + (char) c);

			int hh = readInt();
			expect(':');
			int mm = readInt();
			expect(':');
			int ss = readInt();
			double subss = 0;

			c = read(true);

			if (c == '.') {
				// Fractions of seconds
				subss = readFractions();
				c = read(true);
			}

			int tz = 0;

			if (c == 'Z') {
				// UTC
			} else if (c == '-') {
				tz = readInt();

				if (peek(true) == ':') {
					read(true);
					tz = -(tz * 60 + readInt());
				}
			} else if (c == '+') {
				tz = readInt();

				if (peek(true) == ':') {
					read(true);
					tz = tz * 60 + readInt();
				}
			}

			cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.getDefault());

			cal.set(y, m - 1, d, hh, mm, ss);
			cal.set(Calendar.MILLISECOND, (int) (subss * 1000));
			cal.add(Calendar.MINUTE, tz);
		} else {

			if (!isPriorityAvailable) {
				unread(c);
			}
			// Assume BSD date and time
			int m = readMonthAbbreviation();

			expect(' ');
			skipSpaces();

			int d = readInt();

			expect(' ');
			skipSpaces();

			int hh = readInt();

			expect(':');

			int mm = readInt();

			expect(':');

			int ss = readInt();

			cal = new GregorianCalendar(Locale.ROOT);

			cal.set(Calendar.MONTH, m);
			cal.set(Calendar.DAY_OF_MONTH, d);
			cal.set(Calendar.HOUR_OF_DAY, hh);
			cal.set(Calendar.MINUTE, mm);
			cal.set(Calendar.SECOND, ss);
		}

		expect(' ');
		skipSpaces();

		String hostname = readWordString(32);

		expect(' ');

		byte[] appname = null;
		byte[] procId = null;
		byte[] msgId = null;
		byte[] structuredData = null;

		if (version >= 1) {
			appname = readWordOrNil(20);
			expect(' ');
			procId = readWordOrNil(20);
			expect(' ');
			msgId = readWordOrNil(20);
			expect(' ');
			structuredData = readStructuredData();
			expect(' ');
		} else if (version == 0 && parseTag) {
			// Try to find a colon terminated tag.
			appname = readTag();
			if (peek(true) == '[')
				procId = readPid();
			expect(':');
		}

		skipSpaces();

		byte[] msg = readLine(128);

		return createEvent(priority, cal, hostname, appname, procId, msgId, structuredData, msg);
	}

	/**
	 * Skip an entire line.
	 *
	 * The line is terminated by NL. EOF is silently ignored.
	 *
	 * Useful if a parsing failure has occurred and you want to skip the message.
	 */
	public void skipLine() throws IOException {
		int c;

		do {
			c = read(false);
		} while (c != '\n' && c != -1);
	}

	/**
	 * Create a Flume event from the given parameters.
	 *
	 * @param version        the syslog version, 0 for RFC 3164
	 * @param priority       the syslog priority, according to RFC 5424
	 * @param date           the timestamp of the message. Note that timezone
	 *                       matters
	 * @param hostname       the hostname
	 * @param appname        the RFC 5424 appname
	 * @param procId         the RFC 5424 proc-id
	 * @param msgId          the RFC 5424 msg-id
	 * @param structuredData the RFC 5424 structured-data
	 * @param body           the message body
	 */
	private SyslogEvent createEvent(int priority, Calendar date, String hostname, byte[] appname, byte[] procId,
			byte[] msgId, byte[] structuredData, byte[] body) {

		SyslogEvent event = new SyslogEvent();
		event.setPriority(priority);
		event.setSeverity(priority % 8);
		event.setFacility(priority / 8);
		event.setDate(date);
		event.setHostname(hostname);
		if (appname != null)
			event.setAppname(convertToString(appname));
		if (procId != null)
			event.setProcId(convertToString(procId));
		if (msgId != null)
			event.setMsgId(convertToString(msgId));
		if (structuredData != null)
			event.setStructuredData(convertToString(structuredData));
		if (body != null)
			event.setBody(convertToString(body));
		return event;

	}

	private String convertToString(byte[] inputBytes) {
		if (inputBytes != null) {
			return new String(inputBytes, 0, inputBytes.length, charset);
		}
		return null;
	}

	/**
	 * Read a month value as an English abbreviation.
	 *
	 * @see RFC 3164, Sec. 4.1.2.
	 */
	private int readMonthAbbreviation() throws IOException {

		switch (read(true)) {
		case 'A':
			switch (read(true)) {
			case 'p':
				skipWord();
				return Calendar.APRIL;

			case 'u':
				skipWord();
				return Calendar.AUGUST;

			default:
				return -1;
			}

		case 'D':
			skipWord();
			return Calendar.DECEMBER;

		case 'F':
			skipWord();
			return Calendar.FEBRUARY;

		case 'J':
			read(true); // Second letter is ambiguous.
			read(true); // Third letter is also ambiguous.

			switch (read(true)) {
			case 'e':
				skipWord();
				return Calendar.JUNE;

			case 'u':
				skipWord();
				return Calendar.JANUARY;

			case 'y':
				skipWord();
				return Calendar.JULY;

			default:
				return -1;
			}

		case 'M':
			read(true); // Second letter is ambiguous.

			switch (read(true)) {
			case 'r':
				skipWord();
				return Calendar.MARCH;

			case 'y':
				skipWord();
				return Calendar.MAY;

			default:
				return -1;
			}

		case 'N':
			skipWord();
			return Calendar.NOVEMBER;

		case 'O':
			skipWord();
			return Calendar.OCTOBER;

		case 'S':
			skipWord();
			return Calendar.SEPTEMBER;

		default:
			return -1;
		}
	}

	/**
	 * Read a byte and assert the value.
	 *
	 * @throw IOException if the character was unexpected
	 */
	private void expect(int c) throws IOException {
		int d = read(true);

		if (d != c)
			throw new IOException(UNEXPECTED_CHARACTER + (char) d);
	}

	/**
	 * Read until a non-whitespace ASCII byte is seen.
	 */
	private void skipSpaces() throws IOException {
		int c;

		while ((c = read(false)) == ' ')
			continue;

		unread(c);
	}

	/**
	 * Read the next byte, but then unread it again.
	 *
	 * @param checkEof true to throw EOFException on EOF, false to return -1.
	 */
	private int peek(boolean checkEof) throws IOException {
		int c = read(checkEof);

		unread(c);

		return c;
	}

	/**
	 * Read the next byte.
	 *
	 * @param checkEof true to throw EOFException on EOF, false to return -1.
	 * @return the byte, or -1 on EOF.
	 */
	private int read(boolean checkEof) throws IOException {
		if (pushBack != -1) {
			int c = pushBack;
			pushBack = -1;
			return c;
		}

		int c = in.read();

		if (checkEof && c == -1)
			throw new EOFException("Unexpected end of syslog stream");

		return c;
	}

	/**
	 * Push back a character.
	 *
	 * Only a single character can be pushed back simultaneously.
	 */
	private void unread(int c) {
		assert c != -1 : "Trying to push back EOF";
		assert pushBack == -1 : "Trying to push back two bytes";
		pushBack = c;
	}

	/**
	 * Read a positive integer and convert it from decimal text form.
	 *
	 * EOF silently terminates the number.
	 */
	private int readInt() throws IOException {
		int c;
		int ret = 0;

		while (Character.isDigit(c = read(false)))
			ret = ret * 10 + (c - '0');

		if (c != -1)
			unread(c);

		return ret;
	}

	/**
	 * Read fractions (digits after a decimal point.)
	 *
	 * @return a value in the range [0, 1).
	 */
	private double readFractions() throws IOException {
		int c;
		int ret = 0;
		int order = 1;

		while (Character.isDigit(c = read(false))) {
			ret = ret * 10 + (c - '0');
			order *= 10;
		}

		if (c != -1)
			unread(c);

		return (double) ret / order;
	}

	/**
	 * Read until EOF or a space.
	 *
	 * The input is discarded.
	 */
	private void skipWord() throws IOException {
		int c;

		do {
			c = read(false);
		} while (c != ' ' && c != -1);

		if (c != -1)
			unread(c);
	}

	/**
	 * Read a word into the given output stream.
	 *
	 * Usually the output stream will be a ByteArrayOutputStream.
	 */
	private void readWord(OutputStream out) throws IOException {
		int c;

		while ((c = read(false)) != ' ' && c != -1)
			out.write(c);

		if (c != -1)
			unread(c);
	}

	/**
	 * Read a word (until next ASCII space or EOF) as a string.
	 *
	 * The encoding chosen while constructing the parser is used for decoding.
	 *
	 * @param sizeHint an guess on how large string will be, in bytes.
	 * @return a valid, but perhaps empty, word.
	 */
	private String readWordString(int sizeHint) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(sizeHint);
		readWord(out);

		return out.toString(charset.name());
	}

	/**
	 * Read a word (until next ASCII space or EOF) as a byte array.
	 *
	 * @param sizeHint an guess on how large string will be, in bytes.
	 * @return a valid, but perhaps empty, word.
	 */
	private byte[] readWord(int sizeHint) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(sizeHint);
		readWord(out);

		return out.toByteArray();
	}

	/**
	 * Read a word (until next space or EOF) as a byte array.
	 *
	 * If the complete word is "-", return null.
	 *
	 * @param sizeHint an guess on how large string will be, in bytes.
	 */
	private byte[] readWordOrNil(int sizeHint) throws IOException {
		byte[] ret = readWord(sizeHint);

		if (ret.length == 1 && ret[0] == '-')
			return null;

		return ret;
	}

	/**
	 * Read a line (until next ASCII NL or EOF) as a byte array.
	 *
	 * @param sizeHint an guess on how large the line will be, in bytes.
	 */
	private byte[] readLine(int sizeHint) throws IOException {
		ByteArrayOutputStream ret = new ByteArrayOutputStream(sizeHint);
		int c;

		while ((c = read(false)) != '\n' && c != -1) {
			if (c != '\r')
				ret.write(c);
		}

		return ret.toByteArray();
	}

	/**
	 * Read a RFC 3164 tag.
	 *
	 * Tags end with left bracket, colon, ASCII CR, or ASCII NL.
	 */
	private byte[] readTag() throws IOException {
		ByteArrayOutputStream ret = new ByteArrayOutputStream(16);
		int c;

		while ((c = read(true)) != ':' && c != '[' && c != '\r' && c != '\n')
			ret.write(c);

		unread(c);

		return ret.toByteArray();
	}

	/**
	 * Read a RFC 3164 pid.
	 *
	 * The format is "[1234]".
	 */
	private byte[] readPid() throws IOException {
		ByteArrayOutputStream ret = new ByteArrayOutputStream(8);
		int c;

		expect('[');

		while ((c = read(true)) != ']' && c != '\r' && c != '\n')
			ret.write(c);

		return ret.toByteArray();
	}

	/**
	 * Read RFC 5424 structured data.
	 *
	 * Just read the structured data, but don't create a map of it.
	 */
	private byte[] readStructuredData() throws IOException {
		int c = read(true);

		if (c == '-')
			return null;

		ByteArrayOutputStream ret = new ByteArrayOutputStream(128);

		if (c != '[')
			throw new IOException(UNEXPECTED_CHARACTER + (char) c);

		while (c == '[') {
			ret.write(c);

			// Read SD-ID
			while ((c = read(true)) != ' ' && c != ']')
				ret.write(c);

			ret.write(c);

			while (c == ' ') {
				// Read PARAM-NAME
				while ((c = read(true)) != '=')
					ret.write(c);

				ret.write(c);

				expect('"');
				ret.write('"');

				// Read PARAM-DATA
				while ((c = read(true)) != '"') {
					ret.write(c);

					if (c == '\\') {
						c = read(true);
						ret.write(c);
					}
				}

				ret.write(c);

				c = read(true);
				ret.write(c);
			}

			if (c != ']')
				throw new IOException(UNEXPECTED_CHARACTER + (char) c);

			c = read(true);
		}

		unread(c);

		return ret.toByteArray();
	}
}
