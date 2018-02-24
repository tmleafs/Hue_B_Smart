/**
 *  Hue B Smart Lux Group
 *
 *  Copyright 2016 Anthony Pastor
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
 *	1.5 Added Light capability for smartapps
 *
 */
preferences {
	input("tt", "integer", defaultValue: 4, title: "Time it takes for the lights to transition (default: 2 = 200ms)")
	input("notiSetting", "enum", required:true ,title: "Notifications", description: "Level of IDE Notifications for this Device?", options: ["All", "Only On / Off", "None"], defaultValue: "All")
}  
 
metadata {
	definition (name: "Hue B Smart Lux Group", namespace: "info_fiend", author: "Anthony Pastor") {
	capability "Switch Level"
	capability "Actuator"
	capability "Switch"
	capability "Polling"
	capability "Refresh"
	capability "Sensor"
	capability "Configuration"
	capability "Light"
                
	command "reset"
	command "refresh"
	command "updateStatus"
	command "flash"
	command "flashCoRe"
	command "flash_off"
	command "sendToHub"
	command "setLevel"
	command "scaleLevel"
	               
	attribute "lights", "STRING"       
	attribute "transitionTime", "NUMBER"
	attribute "bri", "number"
	attribute "level", "number"
	attribute "on", "string"
	attribute "groupID", "string"
	attribute "host", "string"
	attribute "username", "string"
	attribute "idelogging", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            		tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
			}
	}

	standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"Reset", action:"reset", icon:"st.lights.philips.hue-multi"
	}
	
	standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
	}
        
      	standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-multi"
	}
        
	valueTile("transitiontime", "device.transitionTime", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "transitiontime", label: 'Transition Time: ${currentValue}'
        }

	valueTile("groupID", "device.groupID", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
		state "default", label: 'GroupID: ${currentValue}'
	}	

	}
	main(["rich-control"])
	details(["rich-control","flash", "reset", "refresh", "transitiontime", "groupID"])
}

private configure() {		
    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "${commandData = commandData}"
    sendEvent(name: "groupID", value: commandData.deviceId, displayed:true, isStateChange: true)
    sendEvent(name: "host", value: commandData.ip, displayed:false, isStateChange: true)
    sendEvent(name: "username", value: commandData.username, displayed:false, isStateChange: true)
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def installed() {
	log.debug "Installed with settings: ${settings}"
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
	if(device.currentValue("idelogging") == "All"){log.trace "Hue B Smart Lux Group: setLevel ( ${inLevel} ): "}
	def level = scaleLevel(inLevel, true, 254)

	def commandData = parent.getCommandData(device.deviceNetworkId)
	def tt = this.device.currentValue("transitionTime") ?: 0
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
	)    
}


/** 
 * capability.switch
 **/
def on() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
	log.trace "Hue B Smart Lux Group: on(): "}

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
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
}

def off() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
	log.trace "Hue B Smart Lux Group: off(): "}
    
	def commandData = parent.getCommandData(device.deviceNetworkId)
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    
	return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
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
	if(device.currentValue("idelogging") == 'All'){log.trace "Hue B Smart Lux Group: poll(): "}
	refresh()
}

/**
 * capability.refresh
 **/
def refresh() {
	if(device.currentValue("idelogging") == 'All'){log.trace "Hue B Smart Lux Group: refresh(): "}
	parent.doDeviceSync()
	//configure()
}

def reset() {
	if(device.currentValue("idelogging") == 'All'){log.trace "Hue B Smart Lux Group: reset(): "}
	def value = [level:70, saturation:56, hue:23]
	sendToHub(value)
}

/**
 * capability.alert (flash)
 **/

def flash() {
	if(device.currentValue("idelogging") == 'All'){log.trace "Hue B Smart Lux Group: flash(): "}
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flashCoRe() {
	if(device.currentValue("idelogging") == 'All'){log.trace "Hue B Smart Lux Group: flashCoRe(): "}
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flash_off() {
	if(device.currentValue("idelogging") == 'All'){log.trace "Hue B Smart Lux Group: flash_off(): "}
    	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
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
	//log.trace "Hue B Lux Group: updateStatus ( ${param}:${val} )"
	if (action == "action") {
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
				if(idelogging == "All" || idelogging == "OnOff"){ 
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
	case "transitiontime":
		curValue = device.currentValue("transitionTime")
                if (curValue != val) {
                	if(idelogging == 'All'){
               		log.debug "Update Needed: Current Value of transitionTime = ${curValue} & newValue = ${val}"}                	
	            	sendEvent(name: "transitionTime", value: val, displayed: true, isStateChange: true)
                	} else {
	                //log.debug "NO Update Needed for transitionTime."                	
                }    
                break
	case "alert":
            	if (val == "none" && idelogging == 'All') {
            		log.debug "Not Flashing"            		
                	} else if (val != "none" && idelogging == 'All'){
                	log.debug "Flashing"
                }
                break
	case "lights":
            	curValue = device.currentValue("lights")
                if (curValue != val) {
			if(idelogging == 'All'){ 
               		log.debug "Update Needed: Current Value of lights = ${curValue} & newValue = ${val}"}
	            	sendEvent(name: "lights", value: val, displayed: false, isStateChange: true) 
			} else {
			//log.debug "NO Update Needed for lights"
                }
                break
            case "scene":
		if(idelogging == 'All'){
            	log.trace "received scene ${val}"}
                break 
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}

def getDeviceType() { return "lux group" }
