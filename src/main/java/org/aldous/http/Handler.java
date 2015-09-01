package org.aldous.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements handlers for Requests as static methods.
 *
 * See https://tools.ietf.org/html/rfc7230 and
 * https://tools.ietf.org/html/rfc7231
 */
public class Handler {
	private static Logger LOG = LoggerFactory.getLogger(Handler.class);

	/**
	 * Response code enum
	 */
	public enum Status {
		OK(200, "OK"),
		CREATED(201, "Created"),
		BAD_REQUEST(400, "Bad Request"),
		NOT_FOUND(404, "Not Found"),
		INTERNAL_ERROR(500, "Internal Server Error"),
		NOT_IMPLEMENTED(501, "Not Implemented");

		private final int code;
		private final String reason;

		Status(int code, String reason) {
			this.code = code;
			this.reason = reason;
		}

		public int getCode() {
			return this.code;
		}

		public String toString() {
			return (this.code + " " + this.reason);
		}

		public String getStatusLine() {
			return ("HTTP/1.1 " + this.code + " " + this.reason);
		}
	}

	/**
	 * Creates a new Response and populates it with appropriate data in
	 * response to the request.
	 */
	public static Response handleRequest(Request req, String documentRoot) {
		Response res = new Response();

		if (req != null) {
			LOG.debug("Request: \n" + req.toString());
		}

		if (req == null) {
			res = Handler.handleNull();
		} else if (req.getMethod() == null) {
			res = Handler.handleNoMethod();
		} else if (req.getMethod() == Request.Method.GET) {
			res = Handler.handleGet(req, documentRoot);
		} else if (req.getMethod() == Request.Method.HEAD) {
			res = Handler.handleHead(req, documentRoot);
		} else {
			LOG.warn("Unexpected case in the handler");
		}

		LOG.debug("Response: \n" + res.toString());
		return res;
	}

	/**
	 * Creates a new Response and populates it in response to a null request.
	 */
	private static Response handleNull() {
		Response res = new Response();
		res.setStatusLine(Status.BAD_REQUEST.getStatusLine());
		return res;
	}

	/**
	 * Creates a new Response and populates it in response to a null method.
	 */
	private static Response handleNoMethod() {
		Response res = new Response();
		res.setStatusLine(Status.NOT_IMPLEMENTED.getStatusLine());
		return res;
	}

	/**
	 * Creates a new Response and populates it in response to a GET request.
	 */
	private static Response handleGet(Request req, String documentRoot) {
		Response res = new Response();

		LOG.info("Processing GET for target: " + req.getRequestTarget());

		String requestTarget = req.getRequestTarget();
		Path rootPath = Paths.get(documentRoot);
		Path targetPath = Paths.get(documentRoot, requestTarget);

		boolean isDirectory = Files.isDirectory(targetPath);
		boolean doesExist = Files.exists(targetPath);

		if (!doesExist) {
			res.setStatusLine(Status.NOT_FOUND.getStatusLine());
			res.setBody("Could not find: " + requestTarget);
		} else if (isDirectory) {
			// Render the name and contents of the directory
			res.setStatusLine(Status.OK.getStatusLine());

			StringBuilder sb = new StringBuilder();
			sb.append("<!DOCTYPE html><html><body>");
			sb.append("<h2>Directory: " + requestTarget + "</h2>");
			try {
				DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath);
				sb.append("<ul>");
				Path relativeTargetPath = rootPath.relativize(targetPath);
				Path relativeParentPath = rootPath.relativize(targetPath.getParent());
				if (relativeTargetPath.toString().length() > 0) {
					sb.append("<li><a href=\"/" + relativeParentPath + "\">..</a></li>");
				}
				for (Path entry: stream) {
					Path relativePath = rootPath.relativize(entry);
					sb.append("<li><a href=\"/" + relativePath + "\">" + entry.getFileName() + "</a></li>");
				}
				sb.append("</ul>");
			} catch (IOException e) {
				LOG.warn("Error reading files from " + requestTarget + ": ", e);
			}
			sb.append("</body></html>");

			res.setBody(sb.toString());
			res.getHeaders().put("Content-Type", "text/html");
		} else {
			// Render the file
			res.setStatusLine(Status.OK.getStatusLine());

			String body = null;
			try {
				body = new String(Files.readAllBytes(targetPath), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				LOG.warn("UnsupportedEncodingException: ", e);
			} catch (IOException e) {
				LOG.warn("Exception when reading target file into response body: ", e);
			}

			res.setBody(body);

			// Set Content-Type based on name of file
			String fileName = targetPath.getFileName().toString().toLowerCase();
			if (fileName.endsWith(".html") || (fileName.endsWith(".htm"))) {
				res.getHeaders().put("Content-Type", "text/html");
			} else {
				res.getHeaders().put("Content-Type", "text/plain");
			}
		}

		return res;
	}

	/**
	 * Creates a new Response and populates it in response to a HEAD request.
	 */
	private static Response handleHead(Request req, String documentRoot) {
		Response res = new Response();

		LOG.info("Processing HEAD for target: " + req.getRequestTarget());

		String requestTarget = req.getRequestTarget();
		Path targetPath = Paths.get(documentRoot, requestTarget);

		boolean doesExist = Files.exists(targetPath);

		if (!doesExist) {
			res.setStatusLine(Status.NOT_FOUND.getStatusLine());
			res.setBody("Could not find: " + requestTarget);
		} else {
			res.setStatusLine(Status.OK.getStatusLine());
		}

		return res;
	}

}
