package org.aldous.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a buffered reader that
 *
 * See https://tools.ietf.org/html/rfc7230 and
 * https://tools.ietf.org/html/rfc7231
 */
public class Request {
	private static Logger LOG = LoggerFactory.getLogger(Request.class);

	private Method method;
	private String requestTarget;
	private String httpVersion;
	private Map<String, String> headers = new HashMap<String, String>();
	private List<String> body = new ArrayList<String>();

	// HTTP request methods (only supported methods enabled)
	public enum Method {
		GET,
		// PUT,
		// POST,
		// DELETE,
		HEAD;
		// OPTIONS,
		// TRACE,
		// CONNECT,
		// PATCH;

		static Method lookup(String method) {
			for (Method m : Method.values()) {
				if (m.toString().equalsIgnoreCase(method)) {
					return m;
				}
			}
			return null;
		}
	}


	/**
	 * Default constructor
	 */
	Request() {
	}

	/**
	 * Creates a new Request and populates it with HttpRequest data parsed
	 * from an input stream.
	 */
	public static Request parseHttpRequest(InputStream istream)
			throws SocketTimeoutException {
		Request req = new Request();

		// TODO: Consider whether a full-blown, byte-level StreamReader is
		// needed for handling larger requests, multipart data, and chunked
		// data. The use of a new BufferedReader each time here might also be
		// an issue. (It can't be closed because that would also close the
		// underlying InputStream.)
		// For now, use the BufferedReader for convenience.
		BufferedReader reader = new BufferedReader(new InputStreamReader(istream));

		boolean isError = false;
		try {
			LOG.debug("Ready to read a line...");
			String line = reader.readLine();	// Read request line
			LOG.info("Request line: " + line);
			if (line == null) {
				LOG.warn("Could not read the request line");
				return null;
			}
			String[] requestLineParts = line.split(" ", 3);
			if (requestLineParts.length < 3) {
				LOG.warn("Invalid request line (fewer than three parts): " + line);
				isError = true;
			}
			req.setMethod(requestLineParts[0]);
			if (req.getMethod() == null) {
				LOG.warn("Unsupported method found in request: " + requestLineParts[0]);
				isError = true;
			}
			if (requestLineParts.length > 2) {
				req.setRequestTarget(requestLineParts[1]);
				req.setHttpVersion(requestLineParts[2]);

				if (!requestLineParts[2].equals("HTTP/1.1")) {
					LOG.warn("Invalid request line (only HTTP/1.1 is supported): " + line);
					isError = true;
				}
			}

			// Read headers
			line = reader.readLine();
			while ((line != null) && (line.length() > 0)) {
				String[] headerPair = line.split(":");
				if (headerPair.length < 2) {
					LOG.warn("Unexpected non-header found: " + line);
					req.getHeaders().put(headerPair[0], "");
				}
				req.getHeaders().put(headerPair[0], headerPair[1].trim());
				line = reader.readLine();	// Read extra line
			}

			// Read body
// Not supported at this time
//			if ((line != null) && (req.getMethod() == Request.Method.PUT)) {
//				line = reader.readLine();
//				List<String> body = req.getBody();
//				while (line != null) {
//					body.add(line);
//				}
//			}
		} catch (SocketTimeoutException ste) {
			// This exception is caught and thrown here because
			// SocketTimeoutException is derived from IOException.
			throw ste;
		} catch (SocketException se) {
			LOG.warn("A socket exception occured: ", se);
			isError = true;
		} catch (IOException e) {
			LOG.warn("Error reading HTTP request from socket: ", e);
			isError = true;
		}

		if (isError == true) {
			return null;
		}

		return req;
	}

	// Getters and setters
	public Method getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = Method.lookup(method);
	}

	public String getRequestTarget() {
		return requestTarget;
	}

	public void setRequestTarget(String requestTarget) {
		this.requestTarget = requestTarget;
	}

	public String getHttpVersion() {
		return httpVersion;
	}

	public void setHttpVersion(String httpVersion) {
		this.httpVersion = httpVersion;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public List<String> getBody() {
		return body;
	}

	public boolean isKeepAlive() {
		String connection = headers.get("Connection");
		if ((connection != null) && (connection.equalsIgnoreCase("keep-alive"))) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.method + " " + this.requestTarget + " " + this.httpVersion + "\r\n");
		for (String key : this.headers.keySet()) {
			sb.append(key + ": " + this.headers.get(key) + "\r\n");
		}
		if (body.size() > 0) {
			for (int i = 0; i < this.body.size(); i++) {
				sb.append("\r\n" + this.body.get(i));
			}
		}
		return sb.toString();
	}

}
