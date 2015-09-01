package org.aldous.http;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * Represents an HTTP response to send to a client.
 *
 * See https://tools.ietf.org/html/rfc7230 and
 * https://tools.ietf.org/html/rfc7231
 */
public class Response {
//	private static Logger LOG = LoggerFactory.getLogger(Response.class);

	private String statusLine;
	private Map<String, String> headers = new HashMap<String, String>();
	private String body = null;


	/**
	 * Default constructor
	 */
	Response() {
		// Set the Date header
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		headers.put("Date", dateFormat.format(calendar.getTime()));
	}

	// Getters and setters
	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(String statusLine) {
		this.statusLine = statusLine;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	private void calculateAndSetContentLength() {
		int length = 0;
		if (body != null) {
			length = body.length();
		}
		headers.put("Content-Length", Integer.toString(length));
	}

	@Override
	public String toString() {
		calculateAndSetContentLength();
		StringBuilder sb = new StringBuilder();
		sb.append(this.statusLine + "\r\n");
		for (String key : this.headers.keySet()) {
			sb.append(key + ": " + this.headers.get(key) + "\r\n");
		}
		sb.append("\r\n");
		if (body != null) {
			sb.append(this.body);
		}
		return sb.toString();
	}
}
