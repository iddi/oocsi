import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.behavior.OOCSISpatial;
import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.services.OOCSICall;

public class ClientSpatialTest {

	private static final String DESTINATION = "oocsi_spatial_destination_request";
	private static final String DESTINATION_RESPONSE = "oocsi_spatial_destination_distance";
	private static final String ROUTING_PATH = "oocsi_spatial_routing_path";

	@Test
	public void testSpatialIntegerNeighbors() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);

		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel", "integer_distance", 4, 2);

		Map<String, OOCSIClient> clients = getClients(
				new String[] { "os_spatial_2", "os_spatial_3", "os_spatial_4", "os_spatial_5", "os_spatial_6" });

		{
			OOCSIClient client2 = clients.get("os_spatial_2");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 3, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_3");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 1, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_4");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 5, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_5");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 6, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_6");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 7, 2);
		}

		// initialization should be reference value
		List<String> neighbors = os1.getNeighbors();
		assertTrue(neighbors.contains("os_spatial_2"));
		assertTrue(!neighbors.contains("os_spatial_3"));
		assertTrue(neighbors.contains("os_spatial_4"));
		assertTrue(neighbors.contains("os_spatial_5"));
		assertTrue(!neighbors.contains("os_spatial_6"));

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	@Test
	public void testSpatialIntegerNeighborsMessages() throws InterruptedException {

		final List<String> eventSink = new LinkedList<String>();

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);

		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel", "integer_distance", 4, 2);

		Map<String, OOCSIClient> clients = getClients(
				new String[] { "os_spatial_2", "os_spatial_3", "os_spatial_4", "os_spatial_5", "os_spatial_6" });

		{
			OOCSIClient client2 = clients.get("os_spatial_2");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 3, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("ok 2");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_3");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 1, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("not ok 3");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_4");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 5, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("ok 4");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_5");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 6, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("ok 5");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_spatial_6");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 7, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("not ok 6");
				}
			});
		}

		// send a message to all neighbors
		os1.neighbors().data("hello", "world").send();

		// wait longer than timeout
		Thread.sleep(200);

		assertTrue(eventSink.contains("ok 2"));
		assertTrue(!eventSink.contains("not ok 3"));
		assertTrue(eventSink.contains("ok 4"));
		assertTrue(eventSink.contains("ok 5"));
		assertTrue(!eventSink.contains("not ok 6"));
		assertEquals(3, eventSink.size());

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	@Test
	public void testSpatialIntegerClosestNeighbor() throws InterruptedException {

		final List<String> eventSink = new LinkedList<String>();

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel", "integer_distance", 4, 2);

		Map<String, OOCSIClient> clients = getClients(
				new String[] { "os_rt_2", "os_rt_3", "os_rt_4", "os_rt_5", "os_rt_6" });

		{
			OOCSIClient client2 = clients.get("os_rt_2");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 3, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("ok 2");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_rt_3");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 1, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("not ok 3");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_rt_4");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 5, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("ok 4");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_rt_5");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 6, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("ok 5");
				}
			});
		}

		{
			OOCSIClient client2 = clients.get("os_rt_6");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 7, 2);
			client2.subscribe(new Handler() {
				public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
						String recipient) {
					eventSink.add("not ok 6");
				}
			});
		}

		// send a message to all neighbors
		assertEquals("os_rt_2", os1.getClosestNeighbor());

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	@Test
	public void testSpatial1DRouting() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);

		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel", "integer_distance", 4, 2);

		Map<String, OOCSIClient> clients = getClients(
				new String[] { "os_rt_2", "os_rt_3", "os_rt_4", "os_rt_5", "os_rt_6", "os_rt_7", "os_rt_8" });

		{
			OOCSIClient client2 = clients.get("os_rt_2");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 2, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_3");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 0, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_4");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 5, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_5");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 7, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_6");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 7, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_7");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 8, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_8");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 9, 2);
		}

		// starting routing...

		{
			// do a call test with chance of success (right direction; not neighbor)
			OOCSICall oc = new OOCSICall(client1, "os_rt_5", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_6");
			oc.sendAndWait();
			assertTrue(oc.hasResponse());
			assertEquals(0, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
		}

		{
			// do a call test with chance of success (right direction; direct neighbor)
			OOCSICall oc = new OOCSICall(client1, "os_rt_4", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_6");
			oc.sendAndWait();
			assertTrue(oc.hasResponse());
			assertEquals(2, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
		}

		{
			// do a call test with success chance (asking a direct neighbor; right direction)
			OOCSICall oc = new OOCSICall(client1, "os_rt_4", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_5");
			oc.sendAndWait();
			assertTrue(oc.hasResponse());
			assertEquals(2, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
		}

		{
			// do a call test without chance of success (asking a direct neighbor; wrong direction)
			OOCSICall oc = new OOCSICall(client1, "os_rt_2", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_6");
			oc.sendAndWait();
			assertTrue(oc.hasResponse());
			assertEquals(5, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
		}

		{
			// do a call test without chance of success (asking a direct neighbor; wrong direction)
			OOCSICall oc = new OOCSICall(client1, "os_rt_2", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_6")
					.data(ROUTING_PATH, client1.getName() + ",");
			oc.sendAndWait();
			assertTrue(oc.hasResponse());
			assertEquals(Float.MAX_VALUE, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
		}

		// send a message to all neighbors (one hop)
		assertEquals("os_rt_2", os1.routing("os_rt_2"));

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_2", os1.routing("os_rt_3"));

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_2", os1.routing("os_rt_3"));

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_4", os1.routing("os_rt_8"));

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	@Test
	public void testSpatial1DRoutingWithBreak() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);

		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel", "integer_distance", 4, 2);

		Map<String, OOCSIClient> clients = getClients(
				new String[] { "os_rt_2", "os_rt_3", "os_rt_4", "os_rt_5", "os_rt_6", "os_rt_7", "os_rt_8" });

		{
			OOCSIClient client2 = clients.get("os_rt_2");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 2, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_3");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 0, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_4");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 5, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_5");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 6, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_6");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 7, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_7");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 8, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_8");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel", "integer_distance", 9, 2);
		}

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_4", os1.routing("os_rt_8"));

		clients.get("os_rt_4").disconnect();

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_5", os1.routing("os_rt_8"));

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	@Test
	public void testSpatial1DRouting2() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);

		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel2", "integer_distance2", 2, 2);

		Map<String, OOCSIClient> clients = getClients(new String[] { "os_rt_2", "os_rt_3", "os_rt_4", "os_rt_5" });

		{
			OOCSIClient client2 = clients.get("os_rt_2");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel2", "integer_distance2", 3, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_3");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel2", "integer_distance2", 5, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_4");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel2", "integer_distance2", 7, 2);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_5");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel2", "integer_distance2", 8, 2);
		}

		// start routing...

		{
			// one hop
			OOCSICall oc = new OOCSICall(client1, "os_rt_2", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_3")
					.data(ROUTING_PATH, client1.getName() + ",");
			oc.sendAndWait();
			// no response, because of timeout
			assertTrue(oc.hasResponse());
			assertEquals(2, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
			assertEquals("os_rt_2", oc.getFirstResponse().getSender());
		}

		{
			// one hop
			OOCSICall oc = new OOCSICall(client1, "os_rt_3", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_4");
			oc.sendAndWait();
			assertTrue(oc.hasResponse());
			assertEquals(2, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
			assertEquals("os_rt_3", oc.getFirstResponse().getSender());
		}

		{
			// two hops
			OOCSICall oc = new OOCSICall(client1, "os_rt_2", DESTINATION, 2000, 10).data(DESTINATION, "os_rt_4");
			oc.sendAndWait();
			assertTrue(oc.hasResponse());
			assertEquals(4, oc.getFirstResponse().getFloat(DESTINATION_RESPONSE, -1), 0);
			assertEquals("os_rt_2", oc.getFirstResponse().getSender());
		}

		// send a message to all neighbors (one hop)
		assertEquals("os_rt_2", os1.routing("os_rt_2"));

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_2", os1.routing("os_rt_3"));

		// repeat
		assertEquals("os_rt_2", os1.routing("os_rt_3"));

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_2", os1.routing("os_rt_5"));

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	@Test
	public void testSpatial2DRouting() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);

		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel2", "integer_distance2", 0, 0, 1.4f);

		Map<String, OOCSIClient> clients = getClients(new String[] { "os_rt_2", "os_rt_3", "os_rt_4" });

		{
			OOCSIClient client2 = clients.get("os_rt_2");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel2", "integer_distance2", 0, 0.9f, 1.4f);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_3");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel2", "integer_distance2", 1, 0, 1.4f);
		}

		{
			OOCSIClient client2 = clients.get("os_rt_4");
			client2.connect("localhost", 4444);
			OOCSISpatial.createSpatial(client2, "spatialChannel2", "integer_distance2", 1.5f, 1, 1.4f);
		}

		// start routing...

		// send a message to all neighbors (one hop)
		assertEquals("os_rt_2", os1.routing("os_rt_2"));

		// send a message to all neighbors (two hops)
		assertEquals("os_rt_3", os1.routing("os_rt_3"));

		// routing from _2: 0.9 + 1.86
		// routing from _3: 1.0 + 1.11
		assertEquals("os_rt_3", os1.routing("os_rt_4"));

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	@Test
	public void testSpatial2DRoutingWithBreak() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);

		OOCSISpatial os1 = OOCSISpatial.createSpatial(client1, "spatialChannel2", "double_distance2", 0f, 0f, 1.4f);

		Map<String, OOCSIClient> clients = getClients(
				new String[] { "os_rt_2", "os_rt_3", "os_rt_4", "os_rt_5", "os_rt_6" });

		OOCSIClient client2 = clients.get("os_rt_2");
		client2.connect("localhost", 4444);
		OOCSISpatial.createSpatial(client2, "spatialChannel2", "double_distance2", 0f, 1f, 1.4f);

		OOCSIClient client3 = clients.get("os_rt_3");
		client3.connect("localhost", 4444);
		OOCSISpatial.createSpatial(client3, "spatialChannel2", "double_distance2", 0f, 2f, 1.4f);

		OOCSIClient client4 = clients.get("os_rt_4");
		client4.connect("localhost", 4444);
		OOCSISpatial.createSpatial(client4, "spatialChannel2", "double_distance2", 1f, 0f, 1.4f);

		OOCSIClient client5 = clients.get("os_rt_5");
		client5.connect("localhost", 4444);
		OOCSISpatial.createSpatial(client5, "spatialChannel2", "double_distance2", 1f, 1f, 1.4f);

		OOCSIClient client6 = clients.get("os_rt_6");
		client6.connect("localhost", 4444);
		OOCSISpatial.createSpatial(client6, "spatialChannel2", "double_distance2", 1f, 2f, 1.4f);

		// start routing...

		assertEquals("os_rt_2", os1.routing("os_rt_3"));

		// disconnect _2
		clients.get("os_rt_2").disconnect();

		assertEquals("os_rt_4", os1.routing("os_rt_3"));

		// clean up
		client1.disconnect();
		for (OOCSIClient c : clients.values()) {
			c.disconnect();
		}
	}

	public Map<String, OOCSIClient> getClients(String[] clientNames) {

		Map<String, OOCSIClient> clients = new HashMap<String, OOCSIClient>();
		for (String client : clientNames) {
			clients.put(client, new OOCSIClient(client));
		}

		return clients;
	}

}