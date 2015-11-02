package de.tum.in.opcua.server.basic;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.tum.in.opcua.server.core.ClientIdentity;
import de.tum.in.opcua.server.core.RequestContext;
import de.tum.in.opcua.server.core.Session;
import de.tum.in.opcua.server.core.auth.IUserPasswordAuthenticator;

public class SampleAuthenticator implements IUserPasswordAuthenticator {

	private static final Logger LOG = Logger
			.getLogger(SampleAuthenticator.class);

	private final Map<String, String> users;

	public SampleAuthenticator() {
		users = new HashMap<String, String>(2);
		users.put("Stanley", "Smith");
		users.put("Francine", "Smith");
	}

	@Override
	public boolean authenticate(ClientIdentity clientIdentity) {
		LOG.info(String.format("user %s authenticates with password %s",
				clientIdentity.getUsername(), clientIdentity.getPassword()));

		/*
		 * actual authentication would be done here
		 */
		boolean authenticated = false;
		final String passwd = users.get(clientIdentity.getUsername());
		if (passwd != null && passwd.equals(clientIdentity.getPassword())) {
			authenticated = true;

			/*
			 * in the session we can set a custom User object or whatever we
			 * want to. therefore we have the userobject in all operations.
			 * 
			 * The session contains already the given ClientIdendentity so we do
			 * not have to store it
			 */
			final Session s = RequestContext.get().getSession();
			s.setCustomObj(new SampleUser(clientIdentity.getUsername()));
		}

		return authenticated;
	}

}
