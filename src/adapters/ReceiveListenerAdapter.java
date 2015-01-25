package adapters;

import api.Message;
import callback.ReceiveListener;


/**
 * A convenience adapter for the ReceiveListener class.
 *
 * @author Simeon Andreev
 *
 * @see ReceiveListener
 */
public class ReceiveListenerAdapter implements ReceiveListener {


	/**
	 * @see ReceiveListener
	 */
	@Override
	public void receivedMessage(Message message) { }

}
