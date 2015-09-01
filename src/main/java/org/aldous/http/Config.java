package org.aldous.http;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages configuration parameters for the server.
 *
 * Default values are provided and then values are read from the configuration
 * properties file.
 */
public class Config {
	private static Logger LOG = LoggerFactory.getLogger(Config.class);

	public static final String CONFIG_FILE = "config.properties";

	// Default values
	public static final int DEFAULT_PORT = 8080;
	public static final int DEFAULT_THREAD_LIMIT = 4;
	public static final int DEFAULT_CLIENT_TIMEOUT = 10000;
	public static final String DEFAULT_DOCUMENT_ROOT = ".";

	// Configuration data
	private int port;
	private int threadLimit;
	private int clientTimeout;
	private String documentRoot;

	/**
	 * Constructor
	 *
	 * Initializes configuration properties from a properties file. Properties
	 * not found in the properties file get default values.
	 */
	public Config() {
		this.port = DEFAULT_PORT;
		this.threadLimit = DEFAULT_THREAD_LIMIT;
		this.clientTimeout = DEFAULT_CLIENT_TIMEOUT;
		this.documentRoot = DEFAULT_DOCUMENT_ROOT;

		Properties properties = new Properties();

		// Read properties file
		InputStream configStream = Server.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
		try {
			properties.load(configStream);
			String tempString;
			tempString = properties.getProperty("port");
			if (tempString != null) {
				setPort(Integer.parseInt(tempString));
			}
			tempString = properties.getProperty("threadLimit");
			if (tempString != null) {
				setThreadLimit(Integer.parseInt(tempString));
			}
			tempString = properties.getProperty("clientTimeout");
			if (tempString != null) {
				setClientTimeout(Integer.parseInt(tempString));
			}
			tempString = properties.getProperty("documentRoot");
			if (tempString != null) {
				try {
					setDocumentRoot(tempString);
				} catch (FileNotFoundException fnfe) {
					LOG.warn("FileNotFoundException: ", fnfe);
				}
			}
		} catch (IOException ioe) {
			LOG.warn("Could not read configuration properties");
		}
	}


	// Getters and setters.
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getThreadLimit() {
		return threadLimit;
	}

	public void setThreadLimit(int threadLimit) {
		this.threadLimit = threadLimit;
	}

	public int getClientTimeout() {
		return clientTimeout;
	}

	public void setClientTimeout(int clientTimeout) {
		this.clientTimeout = clientTimeout;
	}

	public String getDocumentRoot() {
		return this.documentRoot;
	}

	public void setDocumentRoot(String root) throws FileNotFoundException {
		Path path = Paths.get(root);
		try {
			this.documentRoot = path.toRealPath().toString();
		} catch (IOException e) {
			LOG.warn("Error setting documentRoot to " + path + ": ", e);
		}
		if (!Files.isDirectory(path)) {
			throw new FileNotFoundException("documentRoot was not set to an existing directory: " +
					root);
		}
	}

}
