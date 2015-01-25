package receive;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import callback.ReceiveListener;
import thread.Suspendable;
import thread.ThreadPool;
import utility.Constants;


/**
 * A receiver of API messages. Accepts socket connections with a server socket and uses a thread pool to wait on messages.
 *
 * @author Simeon Andreev
 *
 */
public class MessageReceiver extends Suspendable {

	/** The logger for this class. */
	private final Logger logger = Logger.getLogger(Constants.receiverlogger);
	/** The server socket attended by this waiter. */
	private final ServerSocket server;
	/** The thread pool workers to use. */
	private final ReceiveThread workers[];
	/** The thread pool to use when receiving from multiple origins. */
	private final ThreadPool<Origin, ReceiveThread> threadPool;


	/**
	 * Constructor method.
	 *
	 * @param port The port number on which to start the server socket.
	 * @param threads The number of threads the message receiver may use.
	 * @param pollInterval The poll interval (in milliseconds) at which worker threads check the open sockets for incoming data.
	 * @throws IOException Propagates any IOException thrown during the server socket creation.
	 */
	public MessageReceiver(int port, int threads, int pollInterval) throws IOException {
		// Create the server socket.
		server = new ServerSocket(port);
		// Create the thread pool.
		workers = new ReceiveThread[threads];
		for (int i = 0; i < threads; ++i) workers[i] = new ReceiveThread(pollInterval);
		threadPool = new ThreadPool<Origin, ReceiveThread>(workers);

		logger.log(Level.INFO, "Message receiver object created with threads number: " + threads);
	}


	/**
	 * @see Suspendable
	 */
	@Override
	public void run() {
		running.set(true);
		while (condition.get()) {
			try {
				// Wait on a connection.
				Socket socket = server.accept();
				// Add the socket to the thread pool.
				addConnection(socket);
				logger.log(Level.INFO, "Message receiver accepted connection.");
			} catch (IOException e) {
				// Stopping the message receiver causes an IOException here, otherwise something went wrong.
				if (condition.get())
					logger.log(Level.WARNING, "Message receiver caught an IOException while listening for connections: " + e.getMessage());
			}
		}
		running.set(false);
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void stop() {
		logger.log(Level.INFO, "Stopping message dispatcher.");
		if (!running.get()) return;
		condition.set(false);
		threadPool.stop();

		// Close the server socket to wake this thread.
		try {
			server.close();
		} catch (IOException e) {
			// Server socket is already closed. Do nothing.
		}
	}


	/**
	 * Adds a socket connection to the thread pool, assigning a thread to poll the connection for incoming data.
	 *
	 * @param socket The socket connection to poll for incoming data.
	 */
	public synchronized void addConnection(Socket socket) {
		// Add the connection handling to the thread.
		ReceiveThread worker = threadPool.getWorker();
		// Enqueue the socket.
		worker.enqueue(new Origin(socket));
		logger.log(Level.INFO, "Message receiver accepted connection.");
	}

	/**
	 * Sets the listener to notify on received messages.
	 *
	 * @param listener The listener to notify on received messages.
	 */
	public void setListener(ReceiveListener listener) {
		for (int i = 0; i < workers.length; ++i) workers[i].setListener(listener);
	}

	/**
	 * Returns the port number on which the server socket is open.
	 *
	 * @return The port number on which the server socket is open.
	 */
	public int getPort() {
		return server.getLocalPort();
	}

}
