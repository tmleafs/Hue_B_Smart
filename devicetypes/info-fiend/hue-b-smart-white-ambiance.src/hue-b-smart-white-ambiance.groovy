/**
 *  Hue B Smart White Ambiance
 *
 *  Copyright 2016 Anthony Pastor
 *
 *  Thanks to @tmleafs for his help on this addition to the Hue B Smart DTHs!
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Version 1 TMLeafs Fork
 *	1.2 Added command flashCoRe for webcore
 *	1.4 Fixed IDE Logging Information + Other Bug Fixes
 *	1.5 Added Light Capability for smartapps
 *
 */
preferences {
	input("tt", "integer", defaultValue: 2, title: "Time it takes for the lights to transition (default: 2 = 200ms)")   
	input("notiSetting", "enum", required:true ,title: "Notifications", description: "Level of IDE Notifications for this Device?", options: ["All", "Only On / Off", "None"], defaultValue: "All")
}  
 
metadata {
	definition (name: "Hue B Smart White Ambiance", namespace: "info_fiend", author: "Anthony Pastor") {
	capability "Switch Level"
	capability "Actuator"
	capability "Color Temperature"
	capability "Switch"
	capability "Polling"
	capability "Refresh"
	capability "Sensor"
	capability "Configuration"
	capability "Light"

	command "reset"
	command "refresh"
	command "flash"
	command "flashCoRe"
	command "flash_off"
	command "setColorTemperature"        
	command "updateStatus"
	command "getHextoXY"
	command "sendToHub"
	command "applyRelax"
	command "applyConcentrate"
	command "applyReading"
	command "applyEnergize"
	command "scaleLevel"
	attribute "colorTemperature", "number"
	attribute "bri", "number"
	attribute "sat", "number"
	attribute "level", "number"
	attribute "reachable", "string"
	attribute "hue", "number"
	attribute "on", "string"
	attribute "transitionTime", "NUMBER"
	attribute "hueID", "STRING"
	attribute "host", "STRING"
	attribute "hhName", "STRING"
	attribute "colormode", "enum", ["XY", "CT", "HS"]
	attribute "effect", "enum", ["none", "colorloop"]
	attribute "idelogging", "string"
		
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#FFFFFF", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#FFFFFF", nextState:"turningOn"
				}
				tileAttribute ("device.level", key: "SLIDER_CONTROL") {
					attributeState "level", action:"switch level.setLevel", range:"(0..100)"
				}

		}

	valueTile("valueCT", "device.colorTemperature", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
		state "colorTemperature", label: 'Color Temp:  ${currentValue}'
	}
        
	controlTile("colorTemperature", "device.colorTemperature", "slider", inactiveLabel: false,  width: 4, height: 1, range:"(2200..6500)") { 
		state "setCT", action:"setColorTemperature"
	}
    
	standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-multi"
	}
     
    standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"Reset", action:"reset", icon:"st.lights.philips.hue-multi"
	}
    
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
	}
    
	valueTile("transitiontime", "device.transitionTime", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
		state "transitiontime", label: 'Transition Time: ${currentValue}'
    }
	
	valueTile("reachable", "device.reachable", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
		state "default", label: 'Reachable: ${currentValue}'
	}
    
	}
	main(["rich-control"])
	details(["rich-control","valueCT", "colorTemperature","flash","reset","refresh", "transitiontime", "reachable"])
}

void installed() {
	sendEvent(name: "DeviceWatch-Enroll", value: "{\"protocol\": \"LAN\", \"scheme\":\"untracked\", \"hubHardwareId\": \"${device.hub.hardwareID}\"}", displayed: false)
}

private configure() {		
    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "${commandData = commandData}"
    sendEvent(name: "hueID", value: commandData.deviceId, displayed:true, isStateChange: true)
    sendEvent(name: "host", value: commandData.ip, displayed:false, isStateChange: true)
    sendEvent(name: "username", value: commandData.username, displayed:false, isStateChange: true)
}


// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	sendEvent(name: "transitionTime", value: tt)
	initialize()
}

def updated(){
	log.debug "Updated with settings: ${settings}"
	sendEvent(name: "transitionTime", value: tt)
	idelogs()
}

def idelogs() {
	if (notiSetting == null || notiSetting == "Only On / Off"){
	sendEvent(name: "idelogging", value: "OnOff")
	}else if(notiSetting == "All"){
	state.IDELogging = All
	sendEvent(name: "idelogging", value: "All")
	}else {
	sendEvent(name: "idelogging", value: "None")
	}
}

def initialize() {
	state.xy = [:]
	if (notiSetting == null){sendEvent(name: "idelogging", value: "OnOff")}
}

/** 
 * capability.switchLevel 
 **/

def setLevel(inLevel) {
	if(device.currentValue("idelogging") == "All"){
	log.trace "Hue B Smart Ambience Bulb: setLevel( ${inLevel} ): "}
    
	def level = scaleLevel(inLevel, true, 254)
	def commandData = parent.getCommandData(device.deviceNetworkId)    
	def tt = this.device.currentValue("transitionTime") ?: 0
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
	)    
}


def sendToHub(values) {
	if(device.currentValue("idelogging") == "All"){
	log.trace "Hue B Smart Ambience Bulb: sendToHub ( ${values} ): "}
    
	def validValues = [:]
	def commandData = parent.getCommandData(device.deviceNetworkId)
	def sendBody = [:]

	if (values.level) {
    	def bri = values.level 
    	validValues.bri = parent.scaleLevel(bri, true, 254)
        sendBody["bri"] = validValues.bri
		if (values.level > 0) { 
        	sendBody["on"] = true
        } else {
        	sendBody["on"] = false
		}            
	}

	if (values.switch == "off" ) {
    	sendBody["on"] = false
	} else if (values.switch == "on") {
		sendBody["on"] = true
	}
        
	sendBody["transitiontime"] = device.currentValue("transitionTime") as Integer ?: 0
    
	if (values.hue || values.saturation ) {
		def hue = values.hue ?: this.device.currentValue("hue")
    	validValues.hue = scaleLevel(hue, true, 65535)
		sendBody["hue"] = validValues.hue
		def sat = values.saturation ?: this.device.currentValue("saturation")
	    validValues.saturation = scaleLevel(sat, true, 254)
		sendBody["sat"] = validValues.saturation
        
	}
    if(device.currentValue("idelogging") == "All"){
    log.debug "Sending ${sendBody} "}

	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	       	headers: [
	   	    	host: "${commandData.ip}"
			],
	        body: sendBody 
		])
	)    
	sendEvent(name: "colormode", value: "HS") //, isStateChange: true) 
	sendEvent(name: "hue", value: values.hue) //, isStateChange: true) 
	sendEvent(name: "saturation", value: values.saturation, isStateChange: true) 
    
}

def setHue(inHue) {
	if(device.currentValue("idelogging") == "All"){
	log.debug "Hue B Smart Ambience Bulb: setHue()."}
        
	def sat = this.device.currentValue("saturation") ?: 100
	sendToHub([saturation:sat, hue:inHue])

}

def setSaturation(inSat) {
	if(device.currentValue("idelogging") == "All"){
	log.debug "Hue B Smart Ambience Bulb: setSaturation( ${inSat} )."}

	def hue = this.device.currentValue("hue") ?: 100
	sendToHub([saturation:inSat, hue: hue])
    
}


/**
 * capability.colorTemperature 
**/

def setColorTemperature(inCT) {
	if(device.currentValue("idelogging") == "All"){
	log.debug("Hue B Smart Ambience Bulb: setColorTemperature ( ${inCT} )")}
    
	def colorTemp = inCT ?: this.device.currentValue("colorTemperature")
	colorTemp = Math.round(1000000/colorTemp)    
	def commandData = parent.getCommandData(device.deviceNetworkId)
	def tt = device.currentValue("transitionTime") as Integer ?: 0
        
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [ct: colorTemp, transitiontime: tt]
		])
	)
}

def applyRelax() {
	log.info "applyRelax"
	setColorTemperature(2141)
}

def applyConcentrate() {
	log.info "applyConcentrate"
	setColorTemperature(4329)
}

def applyReading() {
	log.info "applyReading"
	setColorTemperature(2890)
}

def applyEnergize() {
	log.info "applyEnergize"
	setColorTemperature(6410)
}


/** 
 * capability.switch
 **/
def on() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
	log.debug "Hue B Smart Ambience: on()"}

	if(device.currentValue("idelogging") == null){
	idelogs()
	log.trace "IDE Logging Updated" //update old users IDE Logs
	}

	def commandData = parent.getCommandData(device.deviceNetworkId)    
	def tt = device.currentValue("transitionTime") as Integer ?: 0
	def percent = device.currentValue("level") as Integer ?: 100
	def level = scaleLevel(percent, true, 254)
    
        return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
}

def off() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
	log.debug "Hue B Smart Ambience Bulb: off()"}

	def commandData = parent.getCommandData(device.deviceNetworkId)
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    
	return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
			body: [on: false]
		])
}

/** 
 * capability.polling
 **/
def poll() {
	if(device.currentValue("idelogging") == 'All'){
	refresh()}
}

/**
 * capability.refresh
 **/
def refresh() {
	if(device.currentValue("idelogging") == 'All'){
	log.debug "Hue B Smart Ambience Bulb: refresh()."}
	parent.doDeviceSync()
	//configure()
}

def reset() {
	if(device.currentValue("idelogging") == 'All'){
	log.debug "Hue B Smart Ambience Bulb: reset()."}
	sendToHub ([level: 70, sat: 56, hue: 23])
}

/**
 * capability.alert (flash)
 **/
def flash() {
	if(device.currentValue("idelogging") == 'All'){
	log.debug "Hue B Smart Ambience Bulb: flash()"}
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flashCoRe() {
	if(device.currentValue("idelogging") == 'All'){
	log.trace "Hue B Smart Lux Group: flashCoRe(): "}
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flash_off() {
	if(device.currentValue("idelogging") == 'All'){
	log.debug "Hue B Smart Ambience Bulb: flash_off()"}
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "none"]
		])
	)
}

/**
 * scaleLevel
 **/
def scaleLevel(level, fromST = false, max = 254) {
    if (fromST) {
        return Math.round( level * max / 100 )
    } else {
    	if (max == 0) {
    		return 0
		} else { 	
        	return Math.round( level * 100 / max )
		}
    }    
}

/**
 * Update Status
 **/
private updateStatus(action, param, val) {
	//log.trace "Hue B Smart White Ambiance: updateStatus ( ${param}:${val} )"
	if (action == "state") {
	def idelogging = device.currentValue("idelogging")      
        def curValue
	switch(param) {
        case "on":
            	curValue = device.currentValue("switch")
                def onoff
            	if (val == true) {
       	         	if (curValue != on) {
				if(idelogging == "All" || idelogging == "OnOff"){ 
                		log.debug "Update Needed: Current Value of switch = false & newValue = ${val}"}
                		sendEvent(name: "switch", value: on, displayed: true, isStateChange: true)                	     
				} else {
				//log.debug "NO Update Needed for switch."                	
        	        }

                } else {
       	         	if (curValue != off) {
				if(idelogging == "All" || idelogging == "OnnOff"){ 
                		log.debug "Update Needed: Current Value of switch = true & newValue = ${val}"}               	                	                
		            	sendEvent(name: "switch", value: off, displayed: true)
				sendEvent(name: "effect", value: "none", displayed: false, isStateChange: true)    
				} else {
				//log.debug "NO Update Needed for switch."                	
	                }

                }    
                break
	case "bri":
	    	curValue = device.currentValue("level")
                val = scaleLevel(val)
                if (curValue != val) {
			if(idelogging == 'All'){ 
               		log.debug "Update Needed: Current Value of level = ${curValue} & newValue = ${val}"} 
	         	sendEvent(name: "level", value: val, displayed: true, isStateChange: true) 
			} else {
			//log.debug "NO Update Needed for level."                	
                }
                
                break
	case "hue":
            	curValue = device.currentValue("hue")
                val = scaleLevel(val, false, 65535)
                if (val > 100) { val = 100 }
                if (curValue != val) { 
                	if(idelogging == 'All'){
               		log.debug "Update Needed: Current Value of hue = ${curValue} & newValue = ${val}"} 
	            	sendEvent(name: "hue", value: val, displayed: true, isStateChange: true) 
			} else {
			//log.debug "NO Update Needed for hue."                	
                }            	
                break
	case "sat":
		curValue = device.currentValue("saturation")
                val = scaleLevel(val)
                if (val > 100) { val = 100 }                
                if (curValue != val) {
			if(idelogging == 'All'){
               		log.debug "Update Needed: Current Value of saturation = ${curValue} & newValue = ${val}" }
	            	sendEvent(name: "saturation", value: val, displayed: true, isStateChange: true) 
			} else {
			//log.debug "NO Update Needed for saturation."                	
                }
                break
	case "ct": 
            	curValue = device.currentValue("colorTemperature")
                val = Math.round(1000000/val)
                if (curValue != val) {
			if(idelogging == 'All'){ 
               		log.debug "Update Needed: Current Value of colorTemperature = ${curValue} & newValue = ${val}"} 
	            	sendEvent(name: "colorTemperature", value: val, displayed: true, isStateChange: true) 
			} else {
			//log.debug "NO Update Needed for colorTemperature."                	
                }
                break
	case "reachable":
		if (val == true){
			sendEvent(name: "reachable", value: true, displayed: false, isStateChange: true)
			}else{
                	sendEvent(name: "reachable", value: false, displayed: false, isStateChange: true)
                }	
                break
	case "colormode":
            	curValue = device.currentValue("colormode")
                if (curValue != val) {
                	if(idelogging == 'All'){
               		log.debug "Update Needed: Current Value of colormode = ${curValue} & newValue = ${val}" }
	            	sendEvent(name: "colormode", value: val, displayed: false, isStateChange: true) 
			} else {
			//log.debug "NO Update Needed for colormode."                	
                }	
                break
	case "transitiontime":
	        curValue = device.currentValue("transitionTime")
                if (curValue != val) {
			if(idelogging == 'All'){ 
               		log.debug "Update Needed: Current Value of transitionTime = ${curValue} & newValue = ${val}" }               	
	            	sendEvent(name: "transitionTime", value: val, displayed: true, isStateChange: true)
        	        } else {
			//log.debug "NO Update Needed for transitionTime."                	
                }    
                break
	case "alert":
            	if (val == "none" && idelogging == "All") {
            		log.debug "Not Flashing"            		
                	} else if (val != "none" && idelogging == "All"){
                	log.debug "Flashing"
                }
                break
	case "effect":
            	curValue = device.currentValue("effect")
                if (curValue != val) {
                	if(idelogging == 'All'){
               		log.debug "Update Needed: Current Value of effect = ${curValue} & newValue = ${val}" }
	            	sendEvent(name: "effect", value: val, displayed: false, isStateChange: true) 
			} else {
			//log.debug "NO Update Needed for effect "                	
                }
                break
 
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}

def getDeviceType() { return "lights" }
