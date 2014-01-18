package nl.tue.id.oocsi.server.discovery;

/**
 * @author jhu
 *
 */
public interface ServiceExplorerListener {
	public abstract void serviceReply(ServiceDescription descriptor);
}
