package org.openiot.scheduler.client.rest;

/**
 * Copyright (c) 2011-2014, OpenIoT
 *
 * This library is free software; you can redistribute it and/or
 * modify it either under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation
 * (the "LGPL"). If you do not alter this
 * notice, a recipient may use your version of this file under the LGPL.
 *
 * You should have received a copy of the LGPL along with this library
 * in the file COPYING-LGPL-2.1; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTY
 * OF ANY KIND, either express or implied. See the LGPL  for
 * the specific language governing rights and limitations.
 *
 * Contact: OpenIoT mailto: info@openiot.eu
 */

import java.io.FileNotFoundException;
import java.io.StringReader;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;

import org.openiot.commons.sensortypes.model.MeasurementCapability;
import org.openiot.commons.sensortypes.model.SensorType;
import org.openiot.commons.sensortypes.model.SensorTypes;
import org.openiot.commons.sensortypes.model.Unit;

import org.openiot.commons.osdspec.model.OAMO;
import org.openiot.commons.osdspec.model.OSDSpec;
import org.openiot.commons.osdspec.model.OSMO;
import org.openiot.commons.osdspec.model.PresentationAttr;
import org.openiot.commons.osdspec.model.Widget;

import org.openiot.commons.osdspec.utils.Utilities;
import org.openiot.commons.osdspec.utils.Utilities.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikos Kefalakis (nkef) e-mail: nkef@ait.edu.gr
 * @author Stavros Petris (spet) e-mail: spet@ait.edu.gr
 */
public class SchedulerClient 
{
	//logger
	final static Logger logger = LoggerFactory.getLogger(SchedulerClient.class);
	
	private ClientRequestFactory clientRequestFactory;
	
	public SchedulerClient() 
	{
		clientRequestFactory = new ClientRequestFactory(UriBuilder.fromUri(
				"http://localhost:8080/scheduler.core").build());
	}
	public SchedulerClient(String schedulerURL) 
	{
		clientRequestFactory = new ClientRequestFactory(UriBuilder.fromUri(
				schedulerURL).build());
	}
	
	/**
 	 * Prints the available services of the scheduler interface. 
	 * Can be used to check that the scheduler service is alive.
	 * 
	 * @return the welcome message 
	 */
	public String welcomeMessage() 
	{
		ClientRequest welcomeMessageClientRequest = clientRequestFactory
				.createRelativeRequest("/rest/services");

		welcomeMessageClientRequest.accept(MediaType.TEXT_PLAIN);
		try {
			String str = welcomeMessageClientRequest.get(String.class).getEntity();
			logger.debug(str);
			return str;
		} catch (Exception e) {
			logger.error("WelcomeMessage getEntity error",e);
			return null;
		}
	}

	/**
	 * Returns the properties of all the sensors deployed in the area defined 
	 * by lon,lat and radius.
	 *  
	 * @param longitude 
	 * @param lat
	 * @param radius
	 * 
	 * @return the sensortypes discovered 
	 */
	public SensorTypes discoverSensors(double longitude, double lat, float radius) 
	{
		ClientRequest discoverSensorsClientRequest = clientRequestFactory
				.createRelativeRequest("/rest/services/discoverSensors");
		
		//Prepare the request
		discoverSensorsClientRequest.queryParameter("userID", "userIDString");
		discoverSensorsClientRequest.queryParameter("longitude", longitude);
		discoverSensorsClientRequest.queryParameter("latitude", lat);
		discoverSensorsClientRequest.queryParameter("radius", radius);

		discoverSensorsClientRequest.accept("application/xml");
		
		//Handle the response		
		String str = null;
		try {
			ClientResponse<String> response = discoverSensorsClientRequest.get(String.class);

			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
			}

			str = response.getEntity();
			logger.debug(str);
		} catch (Exception e) {
			logger.error("discoverSensors getEntity error",e);
			//no need to proceed to umarshalling
			return null;
		}

		try {
			String sensorTypes_JAXB_CONTEXT = "org.openiot.commons.sensortypes.model";						
			
			JAXBContext context = JAXBContext.newInstance(sensorTypes_JAXB_CONTEXT);
			Unmarshaller um = context.createUnmarshaller();
			SensorTypes sensorTypes = (SensorTypes) um.unmarshal(new StreamSource(new StringReader(str)));

			//debug
			for (SensorType sensorType : sensorTypes.getSensorType()) {
				logger.debug("sensorType.getId():" + sensorType.getId());
				logger.debug("sensorType.getName():" + sensorType.getName());
				
				for (MeasurementCapability measurementCapability : sensorType.getMeasurementCapability()) {
					logger.debug("measurementCapability.getId():" + measurementCapability.getId());
					logger.debug("measurementCapability.getName():" + measurementCapability.getType());

					for (Unit unit : measurementCapability.getUnit()) {
						logger.debug("unit.getName():" + unit.getName());
						logger.debug("unit.getType():" + unit.getType());
					}
				}
			}
			
			return sensorTypes;
		}
		catch (Exception e) {
			logger.error("Unmarshal SensorTypes error",e);
			return null;
		}
	}
	
	/**
	 * Stores a service created by the user.
	 * 
	 * @param osdSpec the service specification
	 * 
	 * @return the response from the server, null if something went wrong
	 * 
	 */	
	public String registerService(OSDSpec osdSpec) 
	{
		ClientRequest registerServiceClientRequest = clientRequestFactory
				.createRelativeRequest("/rest/services/registerService");

		registerServiceClientRequest.accept("application/xml");

		registerServiceClientRequest.body("application/xml", osdSpec);
	
		
		//Handle the response
		try {
			ClientResponse<String> response = registerServiceClientRequest.post(String.class);

			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
			}

			String responseStr = response.getEntity();
			logger.debug("Service registered successfully: " + responseStr);
			return responseStr;
		} catch (Exception e) {
			logger.error("register service get response entity error",e);
			return null;
		}
	}

	
	
	// helper methods //	
	
	/**
	 * Creates a predifined spec and calls registerService(OSDSpec osdSpec) to
	 * register it. 
	 * 
	 * @return the response from the server, null if something went wrong
	 */
	public String registerDemoService() 
	{
		//Prepare the request
		OSDSpec osdSpec = new OSDSpec();
		osdSpec.setUserID("Nikos-Kefalakis");
//		
//		//set it and forget it
//		OAMO oamo1 = new OAMO();
//		
//		oamo1.setId("OpenIoTApplicationModelObject_1");
//		oamo1.setName("OpenIoTApplicationModelObject1Name");
//
//		
//		
//		//equivalent to service entity
//		OSMO osmo1 = new OSMO();
//		
//		osmo1.setDescription("The New Hyper Service test");
//				
//		//(kane select apo thn vash gia to Service ID)
//		osmo1.setId("SensorModelObjectServiceID");
//		osmo1.setDescription("OpenIoT Sensor Model Object 1");
//		osmo1.setName("SensorModelObjectName");
//		
//		
//		
//		//ADD WIDGET
//		Widget widget1 = new Widget();
//		
//		//to WidgetID tha sto stelnei katheytheian o achileas (equivalent to widgetAvailable)
//		widget1.setWidgetID("TheYperwidgetID");
//		
//		PresentationAttr presentationAttr1 = new PresentationAttr();
//		presentationAttr1.setName("widget1PresentationAttr1Name");
//		presentationAttr1.setValue("widget1PresentationAttr1Value");
//		
//		PresentationAttr presentationAttr2 = new PresentationAttr();
//		presentationAttr2.setName("widget1PresentationAttr2Name");
//		presentationAttr2.setValue("widget1PresentationAttr2Value");
//		
//		
//		widget1.getPresentationAttr().add(presentationAttr1);
//		widget1.getPresentationAttr().add(presentationAttr2);
//		
//		osmo1.getRequestPresentation().getWidget().add(widget1);
//
//		
//		
//		//ADD QUERY REQUEST
//		
//		osmo1.getQueryRequest().setQuery("SELECT * FROM <openiot> WHERE {?s ?p ?o}");
//		
//		
//			
//		oamo1.getOSMO().add(osmo1);
//		
//		osdSpec.getOAMO().add(oamo1);
		
		return registerService(osdSpec);
	}
		
	/**
	 * Registers a service that 
	 * 
	 * @param osdSpec  the path of the osdspec XML file
	 * 
	 * @return the response from the server, null if something went wrong
	 */
	public String registerFromFile(String osdSpecFilePathName) throws FileNotFoundException,Exception
	{				
		OSDSpec osdSpec = null;
		
		//Open and Deserialize OSDSPec form file
		try {
			osdSpec = Utilities.Deserializer.deserializeOSDSpecFile(osdSpecFilePathName);
		} catch (FileNotFoundException e) {
			logger.error("File Not Found",e);
			throw e;
		} catch (Exception e) {
			logger.error("error creating osdspec object",e);
			throw e;
		}
		
		return registerService(osdSpec);		
	}
}