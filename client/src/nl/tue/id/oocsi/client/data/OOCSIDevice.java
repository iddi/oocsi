package nl.tue.id.oocsi.client.data;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.EventHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

/**
 * OOCSIDevice allows to configure one or more devices for an OOCSI client that can be recognized by HomeAssistant (and
 * the OOCSI server) and will then be displayed or treated otherwise in a semantically correct way.
 * 
 * @author matsfunk
 *
 */
public class OOCSIDevice {

	final private OOCSIClient client;
	final private String deviceName;

	// device data
	private Map<String, String> properties = new HashMap<>();
	private Map<String, Map<String, Object>> components = new HashMap<>();
	private Map<String, float[]> location = new HashMap<>();

	// sending and receiving values for this device (state, value, brightness) depending on type
	private Map<String, Map<String, Object>> componentValues = new HashMap<>();

	/**
	 * create a new OOCSI device
	 * 
	 * @param client
	 * @param deviceName
	 */
	public OOCSIDevice(OOCSIClient client, String deviceName) {
		this.client = client;
		this.deviceName = deviceName;
		properties.put("device_id", client.getName());

		log("created device");
	}

	/**
	 * add a device property
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public OOCSIDevice addProperty(String name, String value) {
		properties.put(name, value);
		log("added property " + name);
		return this;
	}

	/**
	 * add a device location
	 * 
	 * @param name
	 * @param lat
	 * @param lng
	 * @return
	 */
	public OOCSIDevice addLocation(String name, float lat, float lng) {
		location.put(name, new float[] { lat, lng });
		log("added location " + name);
		return this;
	}

	//
	// Components
	//

	/**
	 * add a number component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param min
	 * @param max
	 * @param numberDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addNumber(String name, String channel, float min, float max, float numberDefault, String icon) {
		return addNumber(name, channel, min, max, "", numberDefault, icon);
	}

	/**
	 * add a number component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param min
	 * @param max
	 * @param numberUnit
	 * @param numberDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addNumber(final String name, String channel, float min, float max, String numberUnit,
	        float numberDefault, String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "number");
		componentMap.put("unit", numberUnit);
		componentMap.put("min_max", new float[] { min, max });
		componentMap.put("value", numberDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);

		// receive data
		installValueHandler(name, channel);

		log("added number " + name);
		return this;
	}

	/**
	 * add a sensor component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param sensorType
	 * @param sensorUnit
	 * @param sensorDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addSensor(String name, String channel, String sensorType, String sensorUnit, float sensorDefault,
	        String icon) {
		try {
			addSensor(name, channel, SensorType.valueOf(sensorType), sensorUnit, sensorDefault, icon);
		} catch (Exception e) {
			log("Error in sensor type or unit of component " + name);
		}
		return this;
	}

	/**
	 * add a sensor component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param sensorType
	 * @param sensorUnit
	 * @param sensorDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addSensor(final String name, String channel, SensorType sensorType, String sensorUnit,
	        float sensorDefault, String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "sensor");
		componentMap.put("sensor_type", sensorType.name());
		componentMap.put("unit", sensorUnit);
		componentMap.put("value", sensorDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);

		// receive data
		installValueHandler(name, channel);

		log("added sensor " + name);
		return this;
	}

	/**
	 * add a binary sensor component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param sensorType
	 * @param sensorDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addBinarySensor(String name, String channel, String sensorType, boolean sensorDefault,
	        String icon) {
		try {
			addBinarySensor(name, channel, BinarySensorType.valueOf(sensorType), sensorDefault, icon);
		} catch (Exception e) {
			log("Error in sensor type of component " + name);
		}
		return this;
	}

	/**
	 * add a binary sensor component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param sensorType
	 * @param sensorDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addBinarySensor(final String name, String channel, BinarySensorType sensorType,
	        boolean sensorDefault, String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "binary_sensor");
		componentMap.put("sensor_type", sensorType.name());
		componentMap.put("state", sensorDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);

		// receive data
		installValueHandler(name, channel);

		log("added binary sensor " + name);
		return this;
	}

	/**
	 * add a switch component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param switchType
	 * @param switchDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addSwitch(String name, String channel, String switchType, boolean switchDefault, String icon) {
		try {
			addSwitch(name, channel, SwitchType.valueOf(switchType), switchDefault, icon);
		} catch (Exception e) {
			log("Error in sensor type of component " + name);
		}
		return this;
	}

	/**
	 * add a switch component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param switchType
	 * @param switchDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addSwitch(final String name, String channel, SwitchType switchType, boolean switchDefault,
	        String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "switch");
		componentMap.put("sensor_type", switchType.name().toLowerCase());
		componentMap.put("state", switchDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);

		// receive data
		installValueHandler(name, channel);

		log("added switch " + name);
		return this;
	}

	/**
	 * add a sensor component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param lightType
	 * @param spectrum
	 * @param min
	 * @param max
	 * @param lightDefault
	 * @param defaultBrightness
	 * @param icon              Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                          page
	 * @return
	 */
	public OOCSIDevice addLight(String name, String channel, String lightType, String spectrum, int min, int max,
	        boolean lightDefault, int defaultBrightness, String icon) {
		try {
			addLight(name, channel, LedType.valueOf(lightType), LightSpectrum.valueOf(spectrum), min, max, lightDefault,
			        defaultBrightness, icon);
		} catch (Exception e) {
			log("Error in sensor type or unit of component " + name);
		}
		return this;
	}

	/**
	 * add a sensor component to this device
	 * 
	 * @param name
	 * @param channel
	 * @param lightType
	 * @param spectrum
	 * @param min
	 * @param max
	 * @param lightDefault
	 * @param defaultBrightness
	 * @param icon              Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                          page
	 * @return
	 */
	public OOCSIDevice addLight(final String name, String channel, LedType lightType, LightSpectrum spectrum, int min,
	        int max, boolean lightDefault, int defaultBrightness, String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "light");
		componentMap.put("ledtype", lightType.name());
		componentMap.put("spectrum", spectrum.name());
		componentMap.put("min_max", new int[] { min, max });
		componentMap.put("state", lightDefault);
		componentMap.put("brightness", defaultBrightness);
		componentMap.put("icon", icon);
		components.put(name, componentMap);

		// receive data
		installValueHandler(name, channel);

		log("added light " + name);
		return this;
	}

	//
	// Submission
	//

	/**
	 * submit the heyOOCSI! message to the server
	 */
	public void submit() {
		HashMap<String, Object> messageData = new HashMap<>();
		messageData.put("properties", properties);
		messageData.put("components", components);
		messageData.put("location", location);

		new OOCSIMessage(client, "heyOOCSI!").data(deviceName, messageData).send();
		log("sent heyOOCSI! message");
	}

	/**
	 * alternative call for submit()
	 * 
	 */
	public void sayHi() {
		submit();
	}

	//
	// Values
	//

	/**
	 * internal value receiver handler that is added to components
	 * 
	 * @param name
	 * @param channel
	 */
	private void installValueHandler(final String name, String channel) {
		client.subscribe(channel, new EventHandler() {
			@Override
			public void receive(OOCSIEvent event) {
				if (event.getString("clientHandle", "").equals(name)) {
					// retrieve the value map
					componentValues.putIfAbsent(name, new HashMap<String, Object>());
					Map<String, Object> values = componentValues.get(name);
					if (event.has("state")) {
						values.put("value", event.getObject("state"));
					} else if (event.has("value")) {
						values.put("value", event.getObject("value"));
					}
					if (event.has("brightness")) {
						values.put("brightness", event.getObject("brightness"));
					}
				}
			}
		});
	}

	/**
	 * retrieve the currently set state for the only component -- or default value
	 * 
	 * @param defaultValue
	 * @return
	 */
	public boolean getState(boolean defaultValue) {
		if (componentValues.size() == 1) {
			String componentName = components.keySet().iterator().next();
			return getStateOfComponent(componentName, defaultValue);
		}
		return defaultValue;
	}

	/**
	 * retrieve the currently set state for the given component -- or default value
	 * 
	 * @param defaultValue
	 * @return
	 */
	public boolean getStateOfComponent(String componentName, boolean defaultValue) {
		return getValueOfComponent(componentName, "value", defaultValue ? 1 : 0) != 0;
	}

	/**
	 * retrieve the currently set value for the only component -- or default value
	 * 
	 * @param defaultValue
	 * @return
	 */
	public float getValue(float defaultValue) {
		if (componentValues.size() == 1) {
			String componentName = components.keySet().iterator().next();
			return getValueOfComponent(componentName, "value", defaultValue);
		}
		return defaultValue;
	}

	/**
	 * retrieve the currently set value for a given key for the only component -- or default value
	 * 
	 * @param defaultValue
	 * @return
	 */
	public float getValue(String key, float defaultValue) {
		if (componentValues.size() == 1) {
			String componentName = components.keySet().iterator().next();
			return getValueOfComponent(componentName, key, defaultValue);
		}
		return defaultValue;
	}

	/**
	 * retrieve the currently set default value for the given component -- or default value
	 * 
	 * @param componentName
	 * @param defaultValue
	 * @return
	 */
	public float getValueOfComponent(String componentName, float defaultValue) {
		return getValueOfComponent(componentName, "value", defaultValue);
	}

	/**
	 * retrieve the currently set value for the given component and key -- or default value
	 * 
	 * @param componentName
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public float getValueOfComponent(String componentName, String key, float defaultValue) {
		Map<String, Object> values = componentValues.get(componentName);
		if (values != null) {
			try {
				return Float.parseFloat((String) values.get(key));
			} catch (Exception e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * set the default value for the only component
	 * 
	 * @param state
	 */
	public void setState(boolean state) {
		if (components.size() == 1) {
			String componentName = components.keySet().iterator().next();
			setValueForComponent(componentName, "value", state);
		}
	}

	/**
	 * set the default value for the only component
	 * 
	 * @param value
	 */
	public void setValue(Number value) {
		if (components.size() == 1) {
			String componentName = components.keySet().iterator().next();
			setValueForComponent(componentName, "value", value);
		}
	}

	/**
	 * set the default value for the only component
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		if (components.size() == 1) {
			String componentName = components.keySet().iterator().next();
			setValueForComponent(componentName, "value", value);
		}
	}

	/**
	 * set the value for a given key for the only component
	 * 
	 * @param key
	 * @param value
	 */
	public void setValue(String key, Number value) {
		if (components.size() == 1) {
			String componentName = components.keySet().iterator().next();
			setValueForComponent(componentName, key, value);
		}
	}

	/**
	 * set the value for a given key for the only component
	 * 
	 * @param key
	 * @param value
	 */
	public void setValue(String key, String value) {
		if (components.size() == 1) {
			String componentName = components.keySet().iterator().next();
			setValueForComponent(componentName, key, value);
		}
	}

	/**
	 * set the default value for the given component
	 * 
	 * @param componentName
	 * @param value
	 */
	public void setValueForComponent(String componentName, Number value) {
		setValueForComponent(componentName, "value", value);
	}

	/**
	 * set the default value for the given component
	 * 
	 * @param componentName
	 * @param value
	 */
	public void setValueForComponent(String componentName, String value) {
		setValueForComponent(componentName, "value", value);
	}

	/**
	 * set a value for the given component
	 * 
	 * @param componentName
	 * @param key
	 * @param value
	 */
	public void setValueForComponent(String componentName, String key, boolean value) {
		if (components.containsKey(componentName)) {
			// store value internally
			componentValues.putIfAbsent(componentName, new HashMap<String, Object>());
			Map<String, Object> values = componentValues.get(componentName);
			values.put(key, value);
			// broadcast value
			String channel = (String) components.get(componentName).get("channel_name");
			new OOCSIMessage(client, channel).data(key, value).data("component", componentName).send();
		}
	}

	/**
	 * set a value for the given component
	 * 
	 * @param componentName
	 * @param key
	 * @param value
	 */
	public void setValueForComponent(String componentName, String key, Number value) {
		if (components.containsKey(componentName)) {
			// store value internally
			componentValues.putIfAbsent(componentName, new HashMap<String, Object>());
			Map<String, Object> values = componentValues.get(componentName);
			values.put(key, value);
			// broadcast value
			String channel = (String) components.get(componentName).get("channel_name");
			new OOCSIMessage(client, channel).data(key, value.doubleValue()).data("component", componentName).send();
		}
	}

	/**
	 * set a value for the given component
	 * 
	 * @param componentName
	 * @param key
	 * @param value
	 */
	public void setValueForComponent(String componentName, String key, String value) {
		if (components.containsKey(componentName)) {
			// store value internally
			componentValues.putIfAbsent(componentName, new HashMap<String, Object>());
			Map<String, Object> values = componentValues.get(componentName);
			values.put(key, value);
			// broadcast value
			String channel = (String) components.get(componentName).get("channel_name");
			new OOCSIMessage(client, channel).data(key, value).data("component", componentName).send();
		}
	}

	/**
	 * internal log helper that prepends the log message with heyOOCSI; for better readability of the logs
	 * 
	 * @param message
	 */
	private void log(String message) {
		client.log(" - heyOOCSI (" + deviceName + "): " + message);
	}

	//
	// Types
	//

	/**
	 * from: https://developers.home-assistant.io/docs/core/entity/binary-sensor/#available-device-classes
	 *
	 */
	public static enum BinarySensorType {
		/** On means low, Off means normal. */
		battery,
		/** On means charging, Off means not charging. */
		battery_charging,
		/** On means cold, Off means normal. */
		cold,
		/** On means connected, Off means disconnected. */
		connectivity,
		/** On means open, Off means closed. */
		door,
		/** On means open, Off means closed. */
		garage_door,
		/** On means gas detected, Off means no gas (clear). */
		gas,
		/** On means hot, Off means normal. */
		heat,
		/** On means light detected, Off means no light. */
		light,
		/** On means open (unlocked), Off means closed (locked). */
		lock,
		/** On means wet, Off means dry. */
		moisture,
		/** On means motion detected, Off means no motion (clear). */
		motion,
		/** On means moving, Off means not moving (stopped). */
		moving,
		/** On means occupied, Off means not occupied (clear). */
		occupancy,
		/** On means open, Off means closed. */
		opening,
		/** On means plugged in, Off means unplugged. */
		plug,
		/** On means power detected, Off means no power. */
		power,
		/** On means home, Off means away. */
		presence,
		/** On means problem detected, Off means no problem (OK). */
		problem,
		/** On means running, Off means not running. */
		running,
		/** On means unsafe, Off means safe. */
		safety,
		/** On means smoke detected, Off means no smoke (clear). */
		smoke,
		/** On means sound detected, Off means no sound (clear). */
		sound,
		/** On means tampering detected, Off means no tampering (clear) */
		tamper,
		/** On means update available, Off means up-to-date. */
		update,
		/** On means vibration detected, Off means no vibration. */
		vibration,
		/** On means open, Off means closed. */
		window
	}

	/**
	 * from: https://developers.home-assistant.io/docs/core/entity/sensor/#available-device-classes
	 *
	 */
	public static enum SensorType {
		/** Air Quality Index */
		aqi,
		/** % Percentage of battery that is left */
		battery,
		/** ppm Concentration of carbon dioxide. */
		carbon_dioxide,
		/** ppm Concentration of carbon monoxide. */
		carbon_monoxide,
		/** A Current */
		current,
		/** Date, must be formatted according to ISO8601. */
		date,
		/** Wh, kWh, MWh Energy, statistics will be stored in kWh. */
		energy,
		/**
		 * m³, ft³ Volume of gas, statistics will be stored in m³. Gas consumption measured as energy in kWh instead of
		 * a volume should be classified as energy.
		 */
		gas,
		/** % Relative humidity */
		humidity,
		/** lx, lm Light level */
		illuminance,
		/** ISO 4217 Monetary value with a currency. */
		monetary,
		/** µg/m³ Concentration of nitrogen dioxide */
		nitrogen_dioxide,
		/** µg/m³ Concentration of nitrogen monoxide */
		nitrogen_monoxide,
		/** µg/m³ Concentration of nitrous oxide */
		nitrous_oxide,
		/** µg/m³ Concentration of ozone */
		ozone,
		/** µg/m³ Concentration of particulate matter less than 1 micrometer */
		pm1,
		/** µg/m³ Concentration of particulate matter less than 2.5 micrometers */
		pm25,
		/** µg/m³ Concentration of particulate matter less than 10 micrometers */
		pm10,
		/** W, kW Power, statistics will be stored in W. */
		power,
		/** % Power Factor */
		power_factor,
		/** cbar, bar, hPa, inHg, kPa, mbar, Pa, psi Pressure, statistics will be stored in Pa. */
		pressure,
		/** dB, dBm Signal strength */
		signal_strength,
		/** µg/m³ Concentration of sulphure dioxide */
		sulphur_dioxide,
		/** °C, °F Temperature, statistics will be stored in °C. */
		temperature,
		/** Timestamp, must be formatted according to ISO8601. */
		timestamp,
		/** µg/m³ Concentration of volatile organic compounds */
		volatile_organic_compounds,
		/** V Voltage */
		voltage
	}

	/**
	 * from: https://developers.home-assistant.io/docs/core/entity/switch/#available-device-classes
	 *
	 */
	public static enum SwitchType {
		/** Device is an outlet for power */
		OUTLET,
		/** Device is switch for some type of entity */
		SWITCH
	}

	/**
	 * from: ? Tbd.
	 *
	 */
	public static enum LedType {
		RGB, RGBW, RGBWW, CCT, DIMMABLE, ONOFF
	}

	/**
	 * from: ? Tbd.
	 *
	 */
	public static enum LightSpectrum {
		CCT, WHITE, RGB
	}
}
