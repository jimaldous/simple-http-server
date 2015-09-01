package org.aldous.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The main class of the simple HTTP server.
 *
 * It implements that top-level server functionality as it does the
 * following:
 * - Gets the configuration properties
 * - Creates a thread pool
 * - Starts a thread that listens for client communication and starts a new
 *   Connection thread in the thread pool to handle the connection.
 * - Provides for graceful shutdown of resources on exit
 * - Provides a main() method for command-line invocation
 */
public class Server {
	private static Logger LOG = LoggerFactory.getLogger(Server.class);

	private Config config = null;
	private ServerSocket serverSocket = null;
	private ClientListener clientListener = null;
	private Thread serverThread = null;
	private ThreadPoolExecutor threadPool = null;
	private boolean isShutdown = false;

	/**
	 * Inner class that listens for a connection with a client.
	 *
	 * This class is instantiated as a thread and loops to look for
	 * a connection with a client.
	 */
	public class ClientListener implements Runnable {

		// Private variables here are visible to the outer class
		private final int clientTimeout;
		private boolean isBound = false;
		private IOException bindingException = null;

		/**
		 * Constructor
		 */
		private ClientListener() {
			this.clientTimeout = config.getClientTimeout();
		}

		/**
		 * Bind the server socket and then listens for a connection with a
		 * client. When a connection is found, it creates a socket for
		 * communication back with the client and starts a Connection object
		 * in a new thread.
		 */
		@Override
		public void run() {
			LOG.debug("Client listener thread has started");
			try {
				serverSocket.bind(new InetSocketAddress(config.getPort()));
				isBound = true;
			} catch (IOException e) {
				// If there was an error binding on the given port, capture it
				// for later reporting and stop end the thread.
				this.bindingException = e;
				LOG.warn("Client listener thread is ending now because of binding exception");
				return;
			}

			while ((serverSocket != null) && !serverSocket.isClosed()) {
				try {
					LOG.debug("Server socket ready to connect with client");
					final Socket clientSocket = serverSocket.accept();
					if (clientTimeout > 0) {
						clientSocket.setSoTimeout(clientTimeout);
					}
					Connection connection = new Connection(clientSocket, config);
					Thread connectionThread = new Thread(connection);
					try {
						threadPool.execute(connectionThread);
						LOG.info("Added thead: " + connectionThread.getName() +
								", threadPool active count: " + threadPool.getActiveCount() +
								", task count: " + threadPool.getTaskCount());
					} catch (RejectedExecutionException e) {
						LOG.warn("RejectedExecutionException: ", e);
						Response res = Handler.handleRequest(null, "");	// Generates 500 error
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						out.println(res.toString());
						out.close();
					}
				} catch (SocketException e) {
					if (!isShutdown) {	// Outer class variable
						LOG.warn("Socket exception occured: ", e);
					}
				} catch (IOException e) {
					if (!isShutdown) {	// Outer class variable
						LOG.warn("Cannot communicate with the client: ", e);
					}
				}
			}
			LOG.info("Client listener thread has finished");
		}

	};

	/**
	 * Default constructor
	 */
	public Server() {
		this.config = new Config();
		System.out.println("port: " + this.config.getPort() + "; documentRoot: " + this.config.getDocumentRoot());

		// https://docs.oracle.com/javase/tutorial/essential/concurrency/pools.html
		// Create a queue that is 10 times the number of threads.
		int threadLimit = config.getThreadLimit();
		LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(threadLimit * 10);
		threadPool = new ThreadPoolExecutor(threadLimit / 2, threadLimit, 30, TimeUnit.SECONDS, workQueue);
	}

	/**
	 * Configures and starts a ClientListener thread.
	 */
	public void start() throws IOException {

		// Set up socket communication with clients
		// See https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
		try {
			this.serverSocket = new ServerSocket();
		} catch (IOException e) {
			LOG.warn("Could not create server socket: ", e);
		}

		// Start a thread for the ClientListener (inner class)
		// See https://docs.oracle.com/javase/tutorial/essential/concurrency/simple.html
		this.clientListener = new ClientListener();
		serverThread = new Thread(clientListener);
		serverThread.setDaemon(true);
		serverThread.setName("ServerThread");
		serverThread.start();

		// Wait until the server socket is ready for communication
		while (!clientListener.isBound && (clientListener.bindingException == null)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		// Check for any error from the serverThread (which may have ended)
		if (clientListener.bindingException != null) {
			LOG.warn("Exception occurred during server socket binding: ", clientListener.bindingException);
			stop();
		}
	}

	/**
	 * Gracefully shuts down this server instance.
	 */
	public void stop() {

		this.isShutdown = true;	// Set to prevent unnecessary exception messages

		// Shutdown thread pool
		if (threadPool != null) {
			LOG.info("Server shutting down thread pool");
			threadPool.shutdown();
			try {
				threadPool.awaitTermination(2, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				LOG.warn("Could not shutdown of thread pool: ", e);
			}

			if (!threadPool.isTerminated()) {
				LOG.info("Server shutting down threadPool now");
				threadPool.shutdownNow();
				try {
					threadPool.awaitTermination(2, TimeUnit.SECONDS);
					LOG.info("ThreadPool was forced to shut down");
				} catch (InterruptedException e) {
					LOG.warn("Could not force shutdown of thread pool: ", e);
				}
			} else {
				LOG.info("ThreadPool is shut down");
			}
		}

		// Close server socket
		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
				LOG.info("Server socket has closed");
				serverSocket = null;
			} catch (IOException e) {
				LOG.warn("Could not close server socket: ", e);
			}
		} else {
			LOG.info("Server socket was already closed");
		}

		// Just in case, shutdown server thread
		if ((clientListener != null) && (serverThread != null) && serverThread.isAlive()) {
			LOG.info("Shutting down server thread");
			serverThread.interrupt();
			try {
				LOG.info("Waiting for server thread to end");
				if (serverThread.isAlive()) {
					serverThread.join();
					LOG.info("Server thread has ended");
				} else {
					LOG.info("Server thread has already ended after interrupt");
				}
			} catch (InterruptedException e) {
				LOG.info("Interruption while waiting for server thread to terminate: ", e);
			}
		} else {
			LOG.info("Server thread has already ended before shutdown");
		}

	}


	/**
	 * The main method of the application.
	 *
	 * This method creates an instance of the Server, starts it in a new
	 * thread, and then blocks until a keyboard enter key is detected.
	 * All configuration is done through the properties file.
	 * @param args Arguments are ignored.
	 */
	public static void main(String[] args) {
		System.out.println("----- simple-http-server -----");

		final Server server = new Server();
		try {
			server.start();
		} catch (IOException e) {
			LOG.warn("Could not start server: ", e);
		}

		// Set up a shutdown hook to handle cleanup in all cases
		Thread shutdownThread = new Thread() {
			@Override public void run()
			{
				LOG.info("Server is shutting down...");
				server.stop();
				LOG.info("Server stopped.");
			}
		};
		shutdownThread.setName("ShutdownThread");
		Runtime.getRuntime().addShutdownHook(shutdownThread);

		// Block here until keyboard input (triggered by <Enter>)
		System.out.println("\nPress <Enter> to exit\n");
		try {
			System.in.read();
		} catch (Throwable ignored) {
		}

		LOG.info("main is finished");
		System.exit(0);	// Force use of shutdown hook
	}

}
