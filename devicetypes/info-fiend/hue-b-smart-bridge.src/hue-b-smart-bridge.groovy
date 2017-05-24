/**
 *  Hue B Smart Bridge
 *
 *  Copyright 2017 Anthony Pastor
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
 *
 */
metadata {
	definition (name: "Hue B Smart Bridge", namespace: "info_fiend", author: "Anthony Pastor") {
	capability "Actuator"
	capability "Bridge"
	capability "Health Check"


	attribute "serialNumber", "string"
	attribute "networkAddress", "string"
	attribute "status", "string"
	attribute "username", "string"
	attribute "host", "string"
        
	command "discoverItems"
        command "discoverBulbs"
        command "discoverGroups"
        command "discoverScenes"
        command "discoverSchedules"
        command "pollItems"
        command "pollBulbs"
        command "pollGroups"
        command "pollScenes"
        command "pollSchedules"
        
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        	standardTile("bridge", "device.username", width: 6, height: 4) {
        		state "default", label:"Hue Bridge", inactivelabel:true, icon:"st.Lighting.light99-hue", backgroundColor: "#F3C200"
        }

	main "bridge"
	details "bridge"
	}
}

void installed() {
	log.debug "Installed with settings: ${settings}"
	sendEvent(name: "DeviceWatch-Enroll", value: "{\"protocol\": \"LAN\", \"scheme\":\"untracked\", \"hubHardwareId\": \"${device.hub.hardwareID}\"}")
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
    def commandData = parent.getCommandHub(device.deviceNetworkId)
    log.debug "Initialize Bridge ${commandData}"
    sendEvent(name: "idNumber", value: commandData.deviceId, displayed:true, isStateChange: true)
    sendEvent(name: "networkAddress", value: commandData.ip, displayed:false, isStateChange: true)
    sendEvent(name: "username", value: commandData.username, displayed:false, isStateChange: true)
    state.host = this.device.currentValue("networkAddress") + ":80"
    state.userName = this.device.currentValue("username")
    state.initialize = true
}


def discoverItems(inItems = null) {
	log.trace "Bridge discovering all items on Hue hub."
	
	if (state.initialize != true ) { initialize() }
 	if (state.host == null ) { initialize() }
	
	def host = state.host
	def username = state.userName

        log.debug "*********** ${host} ********"
	log.debug "*********** ${username} ********"
	def result 
        
    if (!inItems) {
	    result = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/api/${username}/",
			headers: [
				HOST: host
			]
		)
    }    
                 
	return result

}

def pollItems() {
	log.trace "pollItems: polling state of all items from Hue hub."

	def host = state.host
	def username = state.userName
        
	sendHubCommand(new physicalgraph.device.HubAction(
	method: "GET",
	path: "/api/${username}/",
		headers: [
			HOST: host
		]
	))
	    
}

def discoverBulbs() {
	log.trace "discoverBulbs: discovering bulbs from Hue hub."

	def host = state.host
	def username = state.userName
        
	def result = new physicalgraph.device.HubAction(
	method: "GET",
	path: "/api/${username}/lights/",
		headers: [
			HOST: host
		]
	)
	
    return result
}

def pollBulbs() {
	log.trace "ollBulbs: polling bulbs state from Hue hub."

	def host = state.host
	def username = state.userName
        
	sendHubCommand(new physicalgraph.device.HubAction(
	method: "GET",
	path: "/api/${username}/lights/",
		headers: [
			HOST: host
		]
	))
	    
}

def discoverGroups() {
	log.debug("discoverGroups: discovering groups from Hue hub.")

	def host = state.host
	def username = state.userName
        
	def result = new physicalgraph.device.HubAction(
		method: "GET",
		path: "/api/${username}/groups/",
		headers: [
			HOST: host
		]
	)
    
	return result
}

def pollGroups() {
	log.trace "pollGroups: polling groups state from Hue hub."

	def host = state.host
	def username = state.userName
        
	sendHubCommand(new physicalgraph.device.HubAction(
	method: "GET",
	path: "/api/${username}/groups/",
		headers: [
			HOST: host
		]
	))
	    
}

def discoverScenes() {
	log.debug("discoverScenes: discovering scenes from Hue hub.")

	def host = state.host
	def username = state.userName
	
	def result = new physicalgraph.device.HubAction(
		method: "GET",
		path: "/api/${username}/scenes/",
		headers: [
			HOST: host
		]
	)
	
	return result
}

def pollScenes() {
	log.trace "pollGroups: polling scenes state from Hue hub."

	def host = state.host
	def username = state.userName
        
	sendHubCommand(new physicalgraph.device.HubAction(
	method: "GET",
	path: "/api/${username}/scenes/",
		headers: [
			HOST: host
		]
	))
	    
}


def discoverSchedules() {
	log.trace "discoverSchedules: discovering schedules from Hue hub."

	def host = state.host
	def username = state.userName
	
	def result = new physicalgraph.device.HubAction(
		method: "GET",
		path: "/api/${username}/schedules/",
		headers: [
			HOST: host
		]
	)
	
	return result
}


def handleParse(desc) {

	log.trace "handleParse()"
    
	parse(desc)
}



// parse events into attributes

def parse(String description) {

	log.trace "parse()"
	
        //STRANGE PROBLEM DEVICE WAS HANDLING THE LINK BUTTON AND NOT THE APP
    if (state.initialize != true ) {
	log.debug "Forward to APP we are not installed yet"
    def parsedEvent = description
    log.trace "parsedEvent ${parsedEvent}"
    parent.Hubinstall(parsedEvent)    
    }else{
    
	def parsedEvent = parseLanMessage(description)
    log.trace "parsedEvent ${parsedEvent}"
	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = parsedEvent.headers.toString()
        log.trace "headerString ${headerString}"
        def headertrue = false
        if (headerString.contains("xml")) {
			log.debug "HeaderString: XML"	
            /* description.xml reply, verifying bridge */
            parent.processVerifyResponse(parsedEvent.body)
            headertrue = true
        } else if (headerString?.contains("json")) {
			log.debug "HeaderString: JSON"	
            def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
            headertrue = true
        }
        
			if(header){
			def bridge = parent.getBridge(parsedEvent.mac)
            log.trace "Bridge ${bridge}"
			log.trace "Body ${body}"
            def group 
			def commandReturn = []
            log.trace "Body ${body[0]} body success ${body[0].success}"
			/* responses from bulb/group/scene/schedule command. Figure out which device it is, then pass it along to the device. */
			if (body[0] != null && body[0].success != null) {
            	log.trace "${body[0].success}"
				body.each{
					it.success.each { k, v ->
						def spl = k.split("/")
						log.debug "k = ${k}, split1 = ${spl[1]}, split2 = ${spl[2]}, split3 = ${spl[3]}, split4= ${spl[4]}, value = ${v}"                            
						def devId = ""
                        def d
                        def groupScene
						
                      // SCHEDULES
						if (spl[4] == "schedules" || it.toString().contains("command")) {		
                   			/**
                   			devId = bridge.value.mac + "/SCHEDULE" + k
                   	        log.debug "SCHEDULES: k = ${k}, split3 = ${spl[1]}, split4= ${spl[2]}, value = ${v}"
                            sch = parent.getChildDevice(devId)
   	                        schId = spl[2]
//							def username = state.host
//							def username = state.userName
                            
							log.debug "schedule ${schId} successfully enabled/disabled."

//                   	        parent.doDeviceSync("schedules")
                   	        **/

						// SCENES			
						} else if (spl[4] == "scene" || it.toString().contains( "lastupdated") ) {	
							log.trace "HBS Bridge:parse:scene - msg.body == ${body}"
                   			devId = bridge.value.mac + "/SCENE" + v
	                        d = parent.getChildDevice(devId)
    	                        groupScene = spl[2]
//								def username = state.host
//								def username = state.userName
                            
                            d.updateStatus(spl[3], spl[4], v) 
							log.debug "Scene ${d.label} successfully run on group ${groupScene}."
					                        
			     	        //pollGroups() 	// parent.doDeviceSync("groups")
			     	        //pollBulbs() 	// parent.doDeviceSync("bulbs")
                    	
                    	// GROUPS
						} else if (spl[1] == "groups" && spl[2] != 0 ) {    
            	        	devId = bridge.value.mac + "/" + spl[1].toUpperCase()[0..-2] + spl[2]
        	    	        log.debug "GROUP: devId = ${devId}"                            
	
							d = parent.getChildDevice(devId)

							d.updateStatus(spl[3], spl[4], v) 
							
                            //def gLights = []
                            //gLights = parent.getGLightsDNI(spl[2])
                            //gLights.each { gl ->
                            //	gl.updateStatus("state", spl[4], v) 
                            //}
                            
						// LIGHTS		
						} else if (spl[1] == "lights") {
							spl[1] = "BULBS"
								
							devId = bridge.value.mac + "/" + spl[1].toUpperCase()[0..-2] + spl[2]
							d = parent.getChildDevice(devId)
	                    	
	                    	d.updateStatus(spl[3], spl[4], v)
						
						} else {
							log.warn "Response contains unknown device type ${ spl[1] } ."                                               	            
						}
                        
                        commandReturn
					}
				}	
			} else if (body[0] != null && body[0].error != null) {
				log.warn "Error: ${body}"
			} else if (bridge) {
            	
				def bulbs = [:] 
				def groups = [:] 
				def scenes = [:] 
                def schedules = [:] 

				body?.lights?.each { k, v ->
					bulbs[k] = [id: k, label: v.name, type: v.type, state: v.state]
				}
				    
				state.bulbs = bulbs
				    
	            body?.groups?.each { k, v -> 
                   
    	            groups[k] = [id: k, label: v.name, type: v.type, action: v.action, all_on: v.state.all_on, any_on: v.state.any_on, lights: v.lights] //, groupLightDevIds: devIdsGLights]
				}
				
				state.groups = groups
				
	            body.scenes?.each { k, v -> 
                   	log.trace "k=${k} and v=${v}"
                        				
                  	scenes[k] = [id: k, label: v.name, type: "scene", lights: v.lights]
                            
				}
                
                state.scenes = scenes
                    
                body.schedules?.each { k, v -> 
                  	log.trace "schedules k=${k} and v=${v}"
                   	
                   	def schCommand = v.command.address
                  log.debug "schCommand = ${schCommand}"
                
                    def splCmd = schCommand.split("/")
                 log.debug "splCmd[1] = ${splCmd[1]} / splCmd[2] = ${splCmd[2]} / splCmd[3] = ${splCmd[3]} / splCmd[4] = ${splCmd[4]}"                        
                    def schGroupId = splCmd[4] 
					log.debug "schGroupId = ${schGroupId}"
//                 	def schSceneId = bridge.value.mac + "/SCENES" + ${v.command.body.scene}
    	        
    	            schedules[k] = [id: k, name: v.name, type: "schedule", sceneId: v.command.body.scene, groupId: schGroupId, status: v.status]
				}

                	return createEvent(name: "itemDiscovery", value: device.hub.id, isStateChange: true, data: [bulbs, scenes, groups, schedules, bridge.value.mac])

/**
                if (bulbs && groups && scenes) {
                	return createEvent(name: "itemDiscovery", value: device.hub.id, isStateChange: true, data: [bulbs, scenes, groups, schedules, bridge.value.mac])
				} else {

					if (bulbs) {                
    	            	return createEvent(name: "bulbDiscovery", value: device.hub.id, isStateChange: true, data: [bulbs, bridge.value.mac])
					} 
				
					if (groups) {                
                		return createEvent(name: "groupDiscovery", value: device.hub.id, isStateChange: true, data: [groups, bridge.value.mac])
					}
					
					if (scenes) {                
            	    	return createEvent(name: "sceneDiscovery", value: device.hub.id, isStateChange: true, data: [scenes, bridge.value.mac])				
					} 
				
					if (schedules) {                
    	            	return createEvent(name: "scheduleDiscovery", value: device.hub.id, isStateChange: true, data: [schedules, bridge.value.mac])                    
        	     	}       
            	}    
**/                
			}
			
		} else {
			log.debug("Unrecognized messsage: ${parsedEvent.body}")
		}
		
	}

	return []		//	?????????????????? NEEDED ???????
}
}
