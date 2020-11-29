import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.tue.id.oocsi.client.data.JSONWriter;
import nl.tue.id.oocsi.server.protocol.Base64Coder;

public class MessageParsingSpeedTest {

	@SuppressWarnings("unchecked")
	@Test
	public void test() throws IOException, ClassNotFoundException {
		Map<String, Object> messageMap = new HashMap<String, Object>();
		messageMap.put("number", 1.0);
		messageMap.put("bool", false);
		messageMap.put("hello", "world");
		messageMap.put("hello1", "world");
		messageMap.put("array", new boolean[] { true, false, true, false });
		messageMap.put("array2", new int[] { 1, 2, 3, 4, 5 });
		messageMap.put("array3", new String[] { "1", "2", "3" });

		long time1 = 0, time2 = 0;
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < 1000000; i++) {
				// to string
				String msg = new JSONWriter().write(messageMap);

				// to map
				JsonObject jo = JsonParser.parseString(msg).getAsJsonObject();
				String joStr = jo.toString();
				if (!joStr.equals(msg)) {
					System.out.println("Problem:" + joStr + "\n\n" + msg);
				}
			}
			time1 = System.currentTimeMillis() - start;
			System.out.println("JSON time: " + time1);
		}

		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < 1000000; i++) {

				// to byte stream
				final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
				final ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(messageMap);
				final byte[] rawData = baos.toByteArray();
				String data = new String(Base64Coder.encode(rawData));

				// to map
				ByteArrayInputStream bais = new ByteArrayInputStream(Base64Coder.decode(data));
				ObjectInputStream ois = new ObjectInputStream(bais);
				Object outputObject = ois.readObject();
				Map<String, Object> parsedMap = (Map<String, Object>) outputObject;
				parsedMap.get("bool");
			}
			time2 = System.currentTimeMillis() - start;
			System.out.println("Java time: " + time2);
		}

		System.out.println("Speed-up: " + time2 / (double) time1);

	}

}
