package nl.tue.id.oocsi.client.data;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

/**
 * OOCSIDevice allows to configure one or more devices for an OOCSI client that can be recognized by HomeAssistant (and
 * the OOCSI server) and will then be displayed or treated otherwise in a semantically correct way. Tbc.
 * 
 * @author matsfunk
 *
 */
public class OOCSIDevice {

	final private OOCSIClient client;
	final private String deviceName;

	final private OOCSIMessage deviceMessage;
	private Map<String, Object> properties = new HashMap<>();
	private Map<String, Object> components = new HashMap<>();
	private Map<String, Object> location = new HashMap<>();

	/**
	 * create a new OOCSI device
	 * 
	 * @param client
	 * @param deviceName
	 */
	public OOCSIDevice(OOCSIClient client, String deviceName) {
		this.client = client;
		this.deviceName = deviceName;
		deviceMessage = new OOCSIMessage(client, "heyOOCSI!");
		deviceMessage.data("clientHandle", deviceName);
		deviceMessage.data("properties", properties);
		deviceMessage.data("components", components);
		deviceMessage.data("location", location);
		client.log("Created device " + deviceName);
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
		client.log("Added property " + name + " to the properties list of device " + deviceName);
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
		client.log("Added location " + name + " to the location list of device " + deviceName);
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
	 * @param numberUnit
	 * @param numberDefault
	 * @param icon          Get an icon from the set at: https://materialdesignicons.com/ Copy the title of the icon
	 *                      page
	 * @return
	 */
	public OOCSIDevice addNumberComponent(String name, String channel, float min, float max, String numberUnit,
	        float numberDefault, String icon) {
		try {
			addNumberComponent(name, channel, min, max, SensorUnit.valueOf(numberUnit), numberDefault, icon);
		} catch (Exception e) {
			client.log("Error in number unit of component " + name + " for device " + deviceName);
		}
		return this;
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
	public OOCSIDevice addNumberComponent(String name, String channel, float min, float max, SensorUnit numberUnit,
	        float numberDefault, String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "number");
		componentMap.put("unit", numberUnit.name());
		componentMap.put("min_max", new float[] { min, max });
		componentMap.put("value", numberDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);
		client.log("Added component " + name + " to the components list of device " + deviceName);
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
	public OOCSIDevice addSensorComponent(String name, String channel, String sensorType, String sensorUnit,
	        float sensorDefault, String icon) {
		try {
			addSensorComponent(name, channel, BinarySensorType.valueOf(sensorType), SensorUnit.valueOf(sensorUnit),
			        sensorDefault, icon);
		} catch (Exception e) {
			client.log("Error in sensor type or unit of component " + name + " for device " + deviceName);
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
	public OOCSIDevice addSensorComponent(String name, String channel, BinarySensorType sensorType,
	        SensorUnit sensorUnit, float sensorDefault, String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "sensor");
		componentMap.put("sensor_type", sensorType.name());
		componentMap.put("unit", sensorUnit.name());
		componentMap.put("value", sensorDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);
		client.log("Added component " + name + " to the components list of device " + deviceName);
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
	public OOCSIDevice addBinarySensorComponent(String name, String channel, String sensorType, boolean sensorDefault,
	        String icon) {
		try {
			addBinarySensorComponent(name, channel, BinarySensorType.valueOf(sensorType), sensorDefault, icon);
		} catch (Exception e) {
			client.log("Error in sensor type of component " + name + " for device " + deviceName);
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
	public OOCSIDevice addBinarySensorComponent(String name, String channel, BinarySensorType sensorType,
	        boolean sensorDefault, String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "binary_sensor");
		componentMap.put("sensor_type", sensorType.name());
		componentMap.put("state", sensorDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);
		client.log("Added component " + name + " to the components list of device " + deviceName);
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
	public OOCSIDevice addSwitchComponent(String name, String channel, String switchType, boolean switchDefault,
	        String icon) {
		try {
			addSwitchComponent(name, channel, SwitchType.valueOf(switchType), switchDefault, icon);
		} catch (Exception e) {
			client.log("Error in sensor type of component " + name + " for device " + deviceName);
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
	public OOCSIDevice addSwitchComponent(String name, String channel, SwitchType switchType, boolean switchDefault,
	        String icon) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put("channel_name", channel);
		componentMap.put("type", "switch");
		componentMap.put("sensor_type", switchType.name().toLowerCase());
		componentMap.put("state", switchDefault);
		componentMap.put("icon", icon);
		components.put(name, componentMap);
		client.log("Added component " + name + " to the components list of device " + deviceName);
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
	public OOCSIDevice addLightComponent(String name, String channel, String lightType, String spectrum, int min,
	        int max, boolean lightDefault, int defaultBrightness, String icon) {
		try {
			addLightComponent(name, channel, LedType.valueOf(lightType), LightSpectrum.valueOf(spectrum), min, max,
			        lightDefault, defaultBrightness, icon);
		} catch (Exception e) {
			client.log("Error in sensor type or unit of component " + name + " for device " + deviceName);
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
	public OOCSIDevice addLightComponent(String name, String channel, LedType lightType, LightSpectrum spectrum,
	        int min, int max, boolean lightDefault, int defaultBrightness, String icon) {
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
		client.log("Added component " + name + " to the components list of device " + deviceName);
		return this;
	}

	//
	// Submission
	//

	/**
	 * submit the heyOOCSI! message to the server
	 */
	public void submit() {
		deviceMessage.send();
		client.log("Sent heyOOCSI! message for device " + deviceName);
	}

	/**
	 * alternative call for submit()
	 * 
	 */
	public void sayHi() {
		submit();
	}

	//
	// Types
	//

	/**
	 * from: https://developers.home-assistant.io/docs/core/entity/binary-sensor/#available-device-classes
	 *
	 */
	static enum BinarySensorType {
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
	static enum SensorUnit {
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
	static enum SwitchType {
		/** Device is an outlet for power */
		OUTLET,
		/** Device is switch for some type of entity */
		SWITCH
	}

	/**
	 * from: ?
	 *
	 */
	static enum LedType {
		RGB, RGBW, RGBWW, CCT, DIMMABLE, ONOFF
	}

	/**
	 * from: ?
	 *
	 */
	static enum LightSpectrum {
		CCT, WHITE, RGB
	}
}
