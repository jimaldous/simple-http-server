package org.aldous.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the communication with a client that has established a connection
 * with the server.
 *
 * This connection runs as a thread that will live for the duration of the
 * session. The session will end if the socket times out with no input or
 * if the clients specifies that the connection should close after the
 * response is sent.
 */
public class Connection implements Runnable {
	private static Logger LOG = LoggerFactory.getLogger(Connection.class);

	private final Config config;
	private final Socket clientSocket;
	private final InputStream istream;
	private final PrintWriter outWriter;

	/**
	 * Constructor
	 * @throws IOException
	 */
	public Connection(Socket clientSocket, Config config)
			throws IOException {
		this.clientSocket = clientSocket;
		this.config = config;
		this.istream = this.clientSocket.getInputStream();
		this.outWriter = new PrintWriter(this.clientSocket.getOutputStream(), true);
	}

	/**
	 * Closes the client socket and associated streams.
	 */
	public void close() {
		LOG.info("Closing Connection");
		try {
			if (istream != null) {
				istream.close();
			}
			if (outWriter != null) {
				outWriter.close();
			}
			if (clientSocket != null) {
				clientSocket.close();
			}
		} catch (IOException e) {
			LOG.warn("Error occured when closing Connection: ", e);
		}
	}

	/**
	 * Provides the main loop of activity for the connection. While the socket
	 * is available:
	 * - Read the requests
	 * - Dispatch the request to a handler and get the response
	 * - Write the response
	 */
	@Override
	public void run() {
		LOG.info("Started a new Connection ----------");
		while (!this.clientSocket.isClosed()) {
			Request req = null;
			try {
				req = Request.parseHttpRequest(istream);
				if (req == null) {
					LOG.info("Response was null");
					close();
					continue;
				}
			} catch (SocketTimeoutException e) {
				LOG.info("Input socket has timed out.... closing connection");
				close();
				continue;
			}

			if (req != null) {
				Response res = Handler.handleRequest(req, config.getDocumentRoot());

				if (res != null) {
					// Reflect request's keep-alive status to response
					if (req.isKeepAlive()) {
						res.getHeaders().put("Connection", "keep-alive");
					} else {
						res.getHeaders().put("Connection", "close");
					}
					LOG.info("result: " + res);
					if (res.getStatusLine() != null) {
						LOG.info("Writing result (" + res.getStatusLine() + ")");
						outWriter.print(res.toString());
						outWriter.flush();
					}
				}
			}
		}

		LOG.info("Connection has ended");
	}

}
