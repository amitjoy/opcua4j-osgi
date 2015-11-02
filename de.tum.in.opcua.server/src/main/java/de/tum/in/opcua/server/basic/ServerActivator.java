package de.tum.in.opcua.server.basic;

import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.UserTokenPolicy;
import org.opcfoundation.ua.core.UserTokenType;
import org.opcfoundation.ua.transport.Endpoint;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.SecurityMode;
import org.opcfoundation.ua.transport.security.SecurityPolicy;
import org.opcfoundation.ua.utils.CertificateUtils;
import org.osgi.framework.BundleContext;

import de.tum.in.opcua.server.annotation.AnnotationNodeManager;
import de.tum.in.opcua.server.annotation.IAnnotatedNodeSource;
import de.tum.in.opcua.server.annotation.IAnnotationHistoryManager;
import de.tum.in.opcua.server.basic.history.SampleHistoryNodeManager;
import de.tum.in.opcua.server.basic.nodes.Floor;
import de.tum.in.opcua.server.basic.nodes.HumiditySensor;
import de.tum.in.opcua.server.basic.nodes.Room;
import de.tum.in.opcua.server.basic.nodes.TemperatureSensor;
import de.tum.in.opcua.server.core.UAServer;
import de.tum.in.opcua.server.core.addressspace.INodeManager;

/**
 * A simple OPC UA Server. It binds itself to an TCP port and uses annotations
 * to map Java Beans to OPC UA Nodes. All annotated beans are located in the
 * subpackage "nodes". All OPC UA communications is abstracted by the
 * {@link UAServer}. The {@link UAServer} uses several {@link INodeManager}s to
 * build the address space and to access the actual data represented by OPC UA
 * nodes. Accessing data for annotated beans, an {@link IAnnotatedNodeSource}
 * has to be implemented which is done in the class {@link SampleNodeManager}.
 * 
 * @author harald
 * 
 */
public class ServerActivator extends DependencyActivatorBase {

	private static final String SERVER_ENDPOINT = "opc.tcp://127.0.0.1:6001/sampleuaserver";
	private UAServer server;

	/**
	 * A custom {@link UserTokenPolicy} which uses username and password
	 * authentication but without any encryption. This means the client should
	 * send username and password in plaintext when he calls the ActivateSession
	 * service. Such a policy should only be used if the connection between
	 * client and server is secure on itself.
	 */
	private static UserTokenPolicy UNSECURE_USERNAME_PASSWORD = new UserTokenPolicy(
			"username_plain", UserTokenType.UserName, null, null,
			SecurityPolicy.NONE.getPolicyUri());

	/**
	 * creates a an X509 certificate
	 * 
	 * @return
	 * @throws ServiceResultException
	 * @throws IOException
	 * @throws CertificateException
	 */
	private static KeyPair getApplicationInstanceCertificate()
			throws ServiceResultException, IOException, CertificateException {
		/*
		 * KeyPair kp = null;
		 * 
		 * File certFile = FileUtils.getFileFromResource("/pki/server.pem");
		 * File keyFile = FileUtils.getFileFromResource("/pki/server.key");
		 * final String passPhrase = "";
		 * 
		 * 
		 * Cert cert = Cert.load(certFile); // PrivKey key =
		 * PrivKey.load(keyFile, passPhrase);
		 * 
		 * 
		 * final PEMReader pemReader = new PEMReader(new FileReader(keyFile),
		 * new PasswordFinder() {
		 * 
		 * @Override public char[] getPassword() { return
		 * passPhrase.toCharArray(); } });
		 * 
		 * java.security.KeyPair javaSecKp = (java.security.KeyPair)
		 * pemReader.readObject();
		 * 
		 * PrivKey key = null; try { key = new
		 * PrivKey(javaSecKp.getPrivate().getEncoded()); }catch
		 * (InvalidKeySpecException e) { e.printStackTrace(); } kp = new
		 * KeyPair(cert, key);
		 */
		KeyPair kp = null;
		try {
			// create one on the fly
			kp = CertificateUtils.createApplicationInstanceCertificate(
					"sampleserver", "tu vienna", SERVER_ENDPOINT, 365);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return kp;
	}

	@Override
	public void init(BundleContext context, DependencyManager manager)
			throws Exception {
		// create a UAServer instance
		server = new UAServer();

		/*
		 * set the allowed authentication policies. here we support
		 * username+password and anonymous sessions. the
		 * UNSECURE_USERNAME_PASSWORD policy uses UserTokenType.UserName so that
		 * the client should send username and password but a
		 * SecurityPolicy.None so that the password is sent in plain text (not
		 * encrypted).
		 */
		server.addAnonymousTokenPolicy();
		server.addUserTokenPolicy(new SampleAuthenticator(),
				UserTokenPolicy.SECURE_USERNAME_PASSWORD,
				UserTokenPolicy.SECURE_USERNAME_PASSWORD_BASIC256,
				UNSECURE_USERNAME_PASSWORD);

		// Set some applicationdescriptions
		final ApplicationDescription appDesc = new ApplicationDescription();
		server.setServerDesc(appDesc);

		// choose address server is bound to
		// String hostname = InetAddress.getLocalHost().getHostName();

		final Endpoint endpoint = new Endpoint(new URI(SERVER_ENDPOINT),
				SecurityMode.NONE);
		server.setEndpoint(endpoint);

		// set a X.509 certificate for the server - this is mandatory
		server.addApplicationInstanceCertificate(getApplicationInstanceCertificate());

		// set nodemanger
		final AnnotationNodeManager annoNMgr = new AnnotationNodeManager(
				new SampleNodeManager(), "my building",
				"contains some sample nodes of a building", "sampleBuilding");
		// add nodes to get introspected at startup -> this is a good practice
		annoNMgr.addObjectToIntrospect(new Floor());
		annoNMgr.addObjectToIntrospect(new Room());
		annoNMgr.addObjectToIntrospect(new HumiditySensor());
		annoNMgr.addObjectToIntrospect(new TemperatureSensor());

		// add a history manager
		final IAnnotationHistoryManager myHistNMgr = new SampleHistoryNodeManager();
		annoNMgr.setHistoryManager(myHistNMgr);

		server.addNodeManager(annoNMgr);

		// start the server so that it is ready to serve requests.
		server.start();

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		server.stop();
	}
}
