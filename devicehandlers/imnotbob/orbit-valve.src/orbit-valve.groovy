/**
 *  Orbit Water Faucet Valve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  20180609 - Initial Version based on what others have done. Thanks to all who have worked on this.
 */
/*
 * Capabilities
 * - Battery
 * - Configuration
 * - Refresh
 * - Switch
 * - Valve
*/

metadata {
	definition (name: "Orbit Valve", namespace: "E_Sch", author: "Eric, Gene Ussery, others") {
		capability "Actuator"
		capability "Battery"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Valve"

		command "checkdev"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0006,0201", outClusters: "000A,0019"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	preferences {
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: true, 
			displayDuringSetup: false, 
			required: false
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'closed', action: "on", icon: "st.Outdoor.outdoor16", backgroundColor: "#ffffff"
			state "on", label: 'open', action: "off", icon: "st.Outdoor.outdoor16", backgroundColor: "#53a7c0"
		}
		valueTile("battery", "device.battery", decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		standardTile("refresh", "device.refresh", decoration: "flat") {
			state "refresh", label:'', action:"checkdev", icon:"st.secondary.refresh"
		}
		standardTile("reInit", "device.refresh", decoration: "flat") {
			state "refresh", label:'reInit', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("open", "device.switch", decoration: "flat", inactiveLabel: false) {
			state "on", label:'open', action:"open", icon:"st.Outdoor.outdoor16", backgroundColor: "#53a7c0"
		}
		standardTile("close", "device.switch", decoration: "flat", inactiveLabel: false) {
			state "off", label:'close', action:"close", icon:"st.Outdoor.outdoor16", backgroundColor: "#ffffff"
		}
		main "switch"
		details(["switch","battery","refresh","reInit","open", "close"])
	}
}

// Public methods
def installed() {
	log.trace "installed()"
}

def uninstalled() {
	log.trace "uninstalled()"
}

def configure() {
	log.trace "configure"
/*
	"zdo bind 0x${device.deviceNetworkId} 1 1 6 {${device.zigbeeId}} {}"
	zigbee.configureReporting(0x0001, 0x0020, 0x20, 30, 21600, 0x01)
	return zigbee.readAttribute(0x0006, 0x0000) +
		zigbee.readAttribute(0x0001, 0x0020) +
		zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null) +
		zigbee.configureReporting(0x0001, 0x0020, 0x20, 30, 21600, 0x01)
*/
	return zigbee.onOffConfig(0,(3600*6)) +
		zigbee.configureReporting(0x0001, 0x0020, 0x20, 600, (3600*6), 0x01) + // Configure Battery reporting
		zigbee.writeAttribute(0x0020, 0x0000, 0x23, 0x3840) + // Poll control set to 1 hour
		zigbee.onOffRefresh() +
		zigbee.readAttribute(0x0001, 0x0020) // get a Battery Report
}

def checkdev() {
	if(!state.refreshLastRanAt || now() >= state.refreshLastRanAt + 4000) {
		state.refreshLastRanAt = now()
		log.trace "sending check device status"
	} else {
		log.debug "refresh(): Ran within last 4 seconds - SKIPPING"
		return
	}
	fireCommand(zigbee.onOffRefresh() +
		zigbee.readAttribute(0x0001, 0x0020)) // get a Battery Report
}

def refresh() {
	refresh1(true)
}

def refresh1(logMsg) {
	if(!state.refreshLastRanAt || now() >= state.refreshLastRanAt + 4000) {
		state.refreshLastRanAt = now()
		if(logMsg) { log.trace "sending refresh command" }
	} else {
		log.debug "refresh(): Ran within last 4 seconds - SKIPPING"
		return
	}
/*
	"zdo bind 0x${device.deviceNetworkId} 1 1 6 {${device.zigbeeId}} {}"
	zigbee.configureReporting(0x0001, 0x0020, 0x20, 30, 21600, 0x01)
	"st rattr 0x${device.deviceNetworkId} 1 6 0"
	zigbee.readAttribute(0x0001, 0x0020) // battery voltage
	return zigbee.readAttribute(0x0006, 0x0000) +
		zigbee.readAttribute(0x0001, 0x0020) +
		zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null) +
		zigbee.configureReporting(0x0001, 0x0020, 0x20, 30, 21600, 0x01)
*/
	return	zigbee.onOffRefresh() +
		zigbee.readAttribute(0x0001, 0x0020) + // get a Battery Report
		zigbee.readAttribute(0x0020, 0x0000) + // get poll control checkin
		zigbee.onOffConfig(0,(3600*6)) +
		zigbee.configureReporting(0x0001, 0x0020, 0x20, 600, (3600*6), 0x01) + // Configure Battery reporting
		zigbee.writeAttribute(0x0020, 0x0000, 0x23, 0x3840) // poll control to 1 hour
}


def initialize() {
	if(!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 4000) {
		log.info "initialize..."
		state.updatedLastRanAt = now()
		state.isInstalled = true
		unschedule()
		fireCommand(refresh())
	} else {
		log.trace "initialize(): Ran within last 4 seconds - SKIPPING"
		return
	}
}

def updated() {
	initialize()
	log.info "updated..."
}

// Parse incoming device messages to generate events
def parse(String description) {
	//logDebug("parse: $description")
	state.parseLastRanAt = now()
	def result = zigbee.getEvent(description)

	def res = []
	if(result) {
/*
 This should be able to parse switch message
*/
		log.trace "parse_result:  $result"
		if(result?.name == "switch") {
			if(result?.value == "off") {
				res << createEvent(name: "valve", value: "closed")
				unschedule("myPoll")
			} else {
				res << createEvent(name: "valve", value: "open")
			}
		}
		res << createEvent(result)
		return res
	} else {
		if(description?.startsWith('read attr -')) {
			return parseReportAttributeMessage(description)
		} else {
			Map descMap = zigbee.parseDescriptionAsMap(description)
			if (descMap.clusterId == "8021") {   // Bind_rsp
				log.trace "Received Bind rsp"
			} else if( (descMap.clusterId == "0001" || descMap.clusterId == "0006") && descMap.commandInt == 7) {  
				if(descMap.data[0] == "00") {
					log.trace "Received read attribute response SUCCESS ${descMap.clusterId}"
				} else {
					log.debug "attribute ERROR ${descMap}"
				}
			} else if(descMap.clusterId == "0020") {  
				//if(descMap.data[0] == "00") {
					log.trace "Received Poll Control response ${descMap.clusterId}"
				//} else {
				//	log.debug "attribute ERROR ${descMap}"
				//}
			} else {
				log.warn "DID NOT PARSE MESSAGE for description : $description"
				log.debug "${descMap}"
				def msg = zigbee.parse(description)
				log.trace "parse msg: $msg"
			}
		}
	}
}

def myPoll() {
	def cmd = ""
	def howLong =  now() - state?.parseLastRanAt
	if( howLong > (12 * 60 * 1000)) {
		sendEvent(name: "switch", value:"off")
		sendEvent(name: "valve", value:"closed")
		cmd = refresh1(false)
		log.debug "myPoll refresh ${howLong}ms"
	} else {
		log.trace "myPoll()"
		cmd = zigbee.readAttribute(0x0006, 0x0000) +
			zigbee.readAttribute(0x0001, 0x0020)
	}
	fireCommand(cmd)
}

// Commands to device
def on() {
	log.trace "on()"
/*
	This device only turns on for 10 mins at a time
*/
	runIn((10*60+40), "myPoll", [overwrite: true])
	sendEvent(name: "switch", value:"on")
	sendEvent(name: "valve", value:"open")
	return zigbee.on() // +
		//zigbee.readAttribute(0x0006, 0x0000)
}

def off() {
	log.trace "off()"
	sendEvent(name: "switch", value:"off")
	sendEvent(name: "valve", value:"closed")
	unschedule("myPoll")
	return zigbee.off() //+
		//zigbee.readAttribute(0x0006, 0x0000)
}

def open() {
	log.trace "open()"
	return on()
}

def close() {
	log.trace "close()"
	return off()
}

private parseReportAttributeMessage(String description) {
/*
	Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
*/

	Map descMap = zigbee.parseDescriptionAsMap(description)
	def results = []
	
	//if (descMap.cluster == "0001" && descMap.attrId == "0020") {
	if (descMap.clusterInt == 1 && descMap.attrInt == 0x20) {   // Cluster Power,  Voltage
		results = createEvent(getBatteryResult(Integer.parseInt(descMap.value, 16)))
		log.trace "Received battery level report ${results}"
	} else if (descMap.clusterInt == 0 && descMap.attrInt == 5) {   // ModelIdentifier
		log.trace "Received model identifier"
	} else if (descMap.clusterInt == 32 && descMap.attrInt == 0) {   // Cluster Poll
		log.trace "Received Cluster Poll: ${descMap}"
	} else {
		log.debug "UNKNOWN Desc Map: $descMap"
	}

	return results
}

private getBatteryResult(rawValue) {
	def linkText = getLinkText(device)

	def result = [name: 'battery']

	def volts = rawValue / 10
	if (volts > 3.5) {
		result.descriptionText = "${linkText} battery has too much power (${volts} volts)."
	} else {
		def minVolts = 2.1
		def maxVolts = 3.0
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		result.value = Math.min(100, (int) pct * 100)
		result.descriptionText = "${linkText} battery was ${result.value}%"
	}
	return result
}
// Private methods
private fireCommand(List commands) { //Function used from SmartThings Lightify Dimmer Switch support by Adam Outler
	if (commands != null && commands.size() > 0) {
		log.trace("Executing commands:" + commands)
		for (String value : commands) {
			sendHubCommand([value].collect {new physicalgraph.device.HubAction(it)})
		}
	}
}

private String swapEndianHex(String hex) {
	reverseArray(hex.decodeHex()).encodeHex()
}

private logDebug(msg) {
	if (settings?.debugOutput != false) {
		log.debug "$msg"
	}
}
