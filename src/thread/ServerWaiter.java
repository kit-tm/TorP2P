package thread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.logging.Level;

import p2p.ReceiveListener;


/**
 * A waiter attending a server socket. Will wait for new connections and maintain a socket waiter for each socket.
 *
 * @author Simeon Andreev
 *
 * @see Waiter
 */
public class ServerWaiter extends Waiter {

	/** The server socket attended by this waiter. */
	private final ServerSocket server;
	/** The list of socket threads attending the open sockets. */
	private final LinkedList<SocketWaiter> waiters = new LinkedList<SocketWaiter>();


	/**
	 * Constructor method.
	 *
	 * @param port The port number on which the server socket should be opened.
	 * @throws IOException Throws an IOException if unable to create a server socket on the specified port.
	 */
	public ServerWaiter(int port) throws IOException {
		super();
		logger.log(Level.INFO, "Server requested on port: " + port);
		server = new ServerSocket(port);
		logger.log(Level.INFO, "ServerWaiter object created.");
	}


	/**
	 * @see Runnable
	 */
	@Override
	public void run() {
		logger.log(Level.INFO, "Server thread started.");
		try {
			logger.log(Level.INFO, "Server thread entering its execution loop.");
			while (true) {
				logger.log(Level.INFO, "Server thread waiting on a socket connection.");
				Socket client = server.accept();
				add(client);
			}
		} catch (SocketException e) {
			logger.log(Level.WARNING, "Server thread received a SocketException during a server socket operation: " + e.getMessage());
		} catch (IOException e) {
			logger.log(Level.WARNING, "Server thread received an IOException during a server socket operation: " + e.getMessage());
		}
		logger.log(Level.INFO, "Server thread exiting run method.");
	}

	/**
	 * @see Waiter
	 */
	@Override
	public void set(ReceiveListener listener) {
		this.listener = listener;
		for (Waiter waiter : waiters) waiter.set(listener);
		logger.log(Level.INFO, "ServerWaiter set the new listener.");
	}

	/**
	 * @see Waiter
	 */
	@Override
	public synchronized void stop() throws IOException {
		if (server.isClosed()) return;

		logger.log(Level.INFO, "Server thread stopping the socket threads.");
		for (SocketWaiter waiter : waiters) waiter.stop();
		logger.log(Level.INFO, "Clearing socket threads list.");
		waiters.clear();

		logger.log(Level.INFO, "Closing server socket.");
		server.close();
		logger.log(Level.INFO, "Server thread stopped.");
	}

	/**
	 * Returns the port number on which the server socket is running.
	 *
	 * @return The port number on which the server socket is running.
	 */
	public int port() {
		final int port = server.getLocalPort();
		logger.log(Level.INFO, "Server running on: " + port);
		return port;
	}


	/**
	 * Creates a SocketWaiter for a socket connection and adds it to the connections list.
	 *
	 * @param client The socket connection that should be added to the connections list.
	 */
	private synchronized void add(Socket client) {
		// Do not accept connections if the server is closed.
		if (server.isClosed()) return;

		logger.log(Level.INFO, "Server thread accepted socket connection: " + client.getInetAddress().toString());
		SocketWaiter waiter = new SocketWaiter(client);
		waiter.set(listener);
		waiter.start();
		logger.log(Level.INFO, "Server thread adding a waiter for the new socket.");
		waiters.add(waiter);
	}

}
