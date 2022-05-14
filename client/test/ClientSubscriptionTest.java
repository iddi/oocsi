import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

public class ClientSubscriptionTest {

	@Test
	public void testFilteringBrokenSpec() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_filtering_1b");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("channel_filter1[filter((size>9&&size<22))", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});
		o1.subscribe("channel_filter2[filter(size>9&&size<22))]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});
		o1.subscribe("channel_filter3[filter((size>9&&size<22)]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});
		o1.subscribe("channel_filter4[", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_filtering_2b");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_filter1").data("size", 10).send();
		new OOCSIMessage(o2, "channel_filter2").data("size", 11).send();
		new OOCSIMessage(o2, "channel_filter3").data("size", 12).send();
		new OOCSIMessage(o2, "channel_filter4").data("size", 13).send();
		new OOCSIMessage(o2, "channel_filter4[").data("size", 14).send();

		Thread.sleep(100);

		assertEquals(0, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testFilteringSingleVarExpression() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_filtering_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("channel_filter[filter((size>9&&size<22))]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_filtering_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 8).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", -1).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 20).send();

		Thread.sleep(100);

		assertEquals(2, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testFilteringMultipleVarsExpression() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_filtering_3");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("channel_filter[filter(size>9&&pos<22)]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_filtering_4");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 10).data("pos", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 10).send();

		Thread.sleep(100);

		new OOCSIMessage(o2, "channel_filter").data("pos", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", -1).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 2).data("pos", 222).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 20).data("pos", 20).send();

		Thread.sleep(100);

		assertEquals(2, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testFilteringMultipleExpressionVars() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_filtering_5");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("channel_filter[filter(size>9);filter(pos<22)]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_filtering_6");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 10).data("pos", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 10).send();

		Thread.sleep(100);

		new OOCSIMessage(o2, "channel_filter").data("pos", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", -1).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 2).data("pos", 222).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 20).data("pos", 20).send();

		Thread.sleep(100);

		assertEquals(2, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testFilteringMultipleVarsFctCall() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_filtering_7");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("channel_filter[filter(abs(size)>9&&CEILING(pos)<22)]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_filtering_8");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 10).data("pos", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 10).send();

		Thread.sleep(100);

		new OOCSIMessage(o2, "channel_filter").data("pos", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", -1).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 2).data("pos", 222).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter").data("size", 20).data("pos", 20).send();

		Thread.sleep(100);

		assertEquals(2, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testFilteringAggregate() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_filtering_agg_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("channel_filter_ag[filter(mean(size,2)>9)]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_filtering_agg_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_filter_ag").data("size", 10).send();

		Thread.sleep(100);

		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_filter_ag").data("size", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter_ag").data("size", -1).send();

		Thread.sleep(100);

		assertEquals(1, list.size());

		new OOCSIMessage(o2, "channel_filter_ag").data("size", 100).send();

		Thread.sleep(100);

		assertEquals(2, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testTransform() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_transform_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		// o1.subscribe("channel_filter", new DataHandler() {
		o1.subscribe("channel_transform[transform(supersize,size*10)]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_transform_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_transform").data("size", 10).send();

		Thread.sleep(100);

		assertEquals(1, list.size());
		assertTrue(list.get(0).contains("supersize"));
		assertTrue(list.get(0).contains("100.0"));

		new OOCSIMessage(o2, "channel_transform").data("size", 10).send();

		Thread.sleep(100);

		assertEquals(2, list.size());
		assertTrue(list.get(1).contains("supersize"));
		assertTrue(list.get(1).contains("100.0"));

		new OOCSIMessage(o2, "channel_transform").data("pos", 10).send();

		Thread.sleep(100);

		assertEquals(3, list.size());
		assertTrue(list.get(2).contains("supersize"));
		assertTrue(list.get(2).contains("0.0"));

		new OOCSIMessage(o2, "channel_transform").data("size", -1).send();

		Thread.sleep(100);

		assertEquals(4, list.size());
		assertTrue(list.get(3).contains("supersize"));
		assertTrue(list.get(3).contains("-10.0"));

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testDoubleTransform() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_transform_3");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		// o1.subscribe("channel_filter", new DataHandler() {
		o1.subscribe("channel_transform[transform(supersize,size*10);transform(superpos,pos*1000)]", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_channel_transform_4");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 3).send();

		Thread.sleep(100);

		assertEquals(1, list.size());
		assertTrue(list.get(0).contains("supersize"));
		assertTrue(list.get(0).contains("100.0"));
		assertTrue(list.get(0).contains("superpos"));
		assertTrue(list.get(0).contains("3000.0"));

		new OOCSIMessage(o2, "channel_transform").data("pos", -10).send();

		Thread.sleep(100);

		assertEquals(2, list.size());
		assertTrue(list.get(1).contains("supersize"));
		assertTrue(list.get(1).contains("0.0"));
		assertTrue(list.get(1).contains("superpos"));
		assertTrue(list.get(1).contains("-10000.0"));

		new OOCSIMessage(o2, "channel_transform").data("size", -10).data("pos", -0.4).send();

		Thread.sleep(100);

		assertEquals(3, list.size());
		assertTrue(list.get(2).contains("supersize"));
		assertTrue(list.get(2).contains("-100.0"));
		assertTrue(list.get(2).contains("superpos"));
		assertTrue(list.get(2).contains("-400.0"));

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testAggregatedTransform() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_transform_5");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("channel_transform[transform(minsize,stdev(size*10,2));transform(superpos,mean(pos*1000,2))]",
		        new DataHandler() {
			        public void receive(String sender, Map<String, Object> data, long timestamp) {
				        list.add(sender + data.toString());
			        }
		        });

		OOCSIClient o2 = new OOCSIClient("test_channel_transform_6");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 3).send();
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 4).send();
		new OOCSIMessage(o2, "channel_transform").data("size", 20).data("pos", 5).send();
		new OOCSIMessage(o2, "channel_transform").data("size", 20).data("pos", 6).send();
		new OOCSIMessage(o2, "channel_transform").data("size", 20).data("pos", 7).send();
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 8).send();
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 9).send();

		Thread.sleep(100);

		assertEquals(7, list.size());
		assertTrue(list.get(0).contains("minsize"));
		assertTrue(list.get(0).contains("=35.35"));
		assertTrue(list.get(6).contains("minsize"));
		assertTrue(list.get(6).contains("=0.0"));
		assertTrue(list.get(6).contains("superpos"));
		assertTrue(list.get(6).contains("=8500.0"));

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testAggregatedTransformMinMax() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_transform_7");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		// o1.subscribe("channel_filter", new DataHandler() {
		o1.subscribe("channel_transform[transform(maxval,EMAX(size,5));transform(minval,EMIN(pos,3))]",
		        new DataHandler() {
			        public void receive(String sender, Map<String, Object> data, long timestamp) {
				        list.add(sender + data.toString());
			        }
		        });

		OOCSIClient o2 = new OOCSIClient("test_channel_transform_8");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		new OOCSIMessage(o2, "channel_transform").data("size", 20).data("pos", 2).send();
		Thread.sleep(10);
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 4).send();
		Thread.sleep(10);
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 4).send();
		Thread.sleep(10);
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 4).send();
		Thread.sleep(10);
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 2).send();
		Thread.sleep(10);
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 4).send();
		Thread.sleep(10);
		new OOCSIMessage(o2, "channel_transform").data("size", 10).data("pos", 4).send();

		Thread.sleep(200);

		assertEquals(7, list.size());
		assertTrue(list.get(0).contains("maxval"));
		assertTrue(list.get(0).contains("=20.0"));
		assertTrue(list.get(0).contains("minval"));
		assertTrue(list.get(0).contains("=2.0"));

		assertTrue(list.get(2).contains("minval"));
		assertTrue(list.get(2).toString(), list.get(2).contains("=2.0"));

		assertTrue(list.get(3).contains("minval"));
		assertTrue(list.get(3).toString(), list.get(3).contains("=4.0"));

		assertTrue(list.get(4).contains("maxval"));
		assertTrue(list.get(4).contains("=20.0"));

		assertTrue(list.get(5).contains("maxval"));
		assertTrue(list.get(5).contains("=10.0"));

		o1.disconnect();
		o2.disconnect();
	}

}
