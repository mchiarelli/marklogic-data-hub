package com.marklogic.hub.central.controllers.mastering;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.hub.HubConfig;
//import com.marklogic.envision.hub.HubClient;
//import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.central.AbstractMvcTest;

import kotlin.jvm.KotlinReflectionNotSupportedError;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.xmlunit.builder.Input;
import org.xmlunit.input.WhitespaceStrippedSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

import com.marklogic.hub.central.controllers.mastering.dataservices.Mastering;

public class MasteringControllerTests extends AbstractMvcTest {
	private static final String GET_DOC_URL = "/api/mastering/doc";
	private static final String GET_HISTORY_URL = "/api/mastering/history";
	private static final String GET_NOTIFICATION_URL = "/api/mastering/notification";
	private static final String GET_NOTIFICATIONS_URL = "/api/mastering/notifications";
	private static final String GET_BLOCKS_URL = "/api/mastering/blocks";
	private static final String SET_BLOCK_URL = "/api/mastering/block";
	private static final String UNSET_BLOCK_URL = "/api/mastering/unblock";
	private static final String UPDATE_NOTIFICATION_URL = "/api/mastering/notifications";
	private static final String DELETE_NOTIFICATION_URL = "/api/mastering/notifications/delete";
	private static final String MERGE_URL = "/api/mastering/notifications/merge";

//	@Autowired
//	ModelService modelService;
	
	protected String getResource(String resourceName) {
		InputStream inputStream = null;
		String output;
		try {
			inputStream = getResourceStream(resourceName);
			output = IOUtils.toString(inputStream);
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		return output;
	}
	
	protected static File getResourceFile(String resourceName) {
		return new File(AbstractMvcTest.class.getClassLoader().getResource(resourceName).getFile());
	}

	protected InputStream getResourceStream(String resourceName) {
		return AbstractMvcTest.class.getClassLoader().getResourceAsStream(resourceName);
	}
	
	protected void jsonAssertEquals(JsonNode expected, JsonNode actual) throws Exception {
		jsonAssertEquals(expected, actual,true);
	}

	protected void jsonAssertEquals(JsonNode expected, JsonNode actual, Boolean strict) throws Exception {
		jsonAssertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(actual), strict);
	}

	protected void jsonAssertEquals(JsonNode expected, String actual) throws Exception {
		jsonAssertEquals(expected, actual,true);
	}

	protected void jsonAssertEquals(JsonNode expected, String actual, Boolean strict) throws Exception {
		jsonAssertEquals(objectMapper.writeValueAsString(expected), actual, strict);
	}

	protected void jsonAssertEquals(String expected, JsonNode actual) throws Exception {
		jsonAssertEquals(expected, actual,true);
	}

	protected void jsonAssertEquals(String expected, JsonNode actual, Boolean strict) throws Exception {
		jsonAssertEquals(expected, objectMapper.writeValueAsString(actual), strict);
	}

	protected void jsonAssertEquals(String expected, JsonNode actual, JSONComparator comparator) throws Exception {
		jsonAssertEquals(expected, objectMapper.writeValueAsString(actual), comparator);
	}

	protected void jsonAssertEquals(String expected, String actual, JSONComparator comparator) throws Exception {
		JSONAssert.assertEquals(expected, actual, comparator);
	}

	protected void jsonAssertEquals(String expected, String actual) throws Exception {
		JSONAssert.assertEquals(expected, actual, true);
	}

	protected void jsonAssertEquals(String expected, String actual, Boolean strict) throws Exception {
		JSONAssert.assertEquals(expected, actual, strict);
	}

	
	protected void installDoc(DatabaseClient client, String resource, String uri, String... collections) {
		FileHandle handle = new FileHandle(getResourceFile(resource));
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().addAll(collections);
		client.newDocumentManager().write(uri, meta, handle);
	}

	
	protected void clearStagingFinalAndJobDatabases() {
		clearDatabases(HubConfig.DEFAULT_FINAL_NAME, HubConfig.DEFAULT_STAGING_NAME, HubConfig.DEFAULT_JOB_NAME);
	}

	public void clearDatabases(String... databases) {
		ServerEvaluationCall eval = this.getHubClient().getStagingClient().newServerEval();
		String installer =
			"declare option xdmp:mapping \"false\";\n" +
			"declare variable $databases external;\n" +
			"for $database in fn:tokenize($databases, \",\")\n" +
			"return\n" +
			"  try {\n" +
			"    xdmp:eval('\n" +
			"      cts:uris() ! xdmp:document-delete(.)\n" +
			"    ',\n" +
			"    (),\n" +
			"    map:entry(\"database\", xdmp:database($database))\n" +
			"    )\n" +
			"  } catch($ex) {()}";;
		eval.addVariable("databases", String.join(",", databases));
		EvalResultIterator result = eval.xquery(installer).eval();
		if (result.hasNext()) {
			logger.error(result.next().getString());
		}
	}
	

	@BeforeEach
	public void beforeEachMvcTest()  {
		//super.beforeEachMvcTest();
		//super.setup();

		//removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();

		//registerAccount();
		runAsDataHubDeveloper();
		// HubClient hubClient = getNonAdminHubClient();
		//HubClient hubClient = this.getHubClient();
		DatabaseClient finalDbClient = getHubClient().getFinalClient();
		// modelService.saveModel(hubClient, getResourceStream("models/model.json"));
		installDoc(finalDbClient, "mastering/entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee");
		installDoc(finalDbClient, "mastering/entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee");
		installDoc(finalDbClient, "mastering/entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee");
		installDoc(finalDbClient, "mastering/entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee");
		installDoc(finalDbClient, "mastering/entities/employee3.json", "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(finalDbClient, "mastering/entities/employee4.json", "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(finalDbClient, "mastering/entities/employee5.json", "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee");

		installDoc(finalDbClient, "mastering/entities/department2.json", "/departments/department2.json", "Department", "sm-Department-archived");
		installDoc(finalDbClient, "mastering/entities/department2_2.json", "/departments/department2_2.json", "Department", "sm-Department-archived");
		installDoc(finalDbClient, "mastering/entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered");
		installDoc(finalDbClient, "mastering/entities/department3.json", "/departments/department3.json", "Department");
		installDoc(finalDbClient, "mastering/entities/department4.json", "/departments/department4.json", "Department");

	//	installDoc(finalDbClient, "mastering/notification.xml", "/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml", "sm-Employee-notification", "Employee", "MasterEmployees");
	//	installDoc(finalDbClient, "mastering/notification-audit.xml", "/provenance/3122fb9289441afe347e3c38d30dfcf38ac45052df9543e61e48994c2e6a6dd6.xml", "http://marklogic.com/provenance-services/record");
	 
	}

	@Test
	void getDoc() throws Exception {
		//getJson(GET_DOC_URL)
		//	.andExpect(status().isUnauthorized());

//		loginAsTestUser();
/*
		getJson(GET_DOC_URL + "?docUri=/testFile.json")
			.andExpect(status().isNotFound());

		getJson(GET_DOC_URL + "?docUri=/testFile.xml")
			.andExpect(status().isNotFound());
*/
		//installDoc(getHubClient().getFinalClient(), "mastering/data/testFile.json", "/testFile.json");
		runAsDataHubDeveloper();
		installDoc(getHubClient().getFinalClient(),"mastering/data/testFile.json", "/testFile.json","TEMP");
	        
	//	installDoc(getNonAdminHubClient().getFinalClient(), "mastering/data/testFile.xml", "/testFile.xml");
		loginAsTestUser();
		
		getJson(GET_DOC_URL + "?docUri=/testFile.json")
			.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					jsonAssertEquals(getResource("mastering/data/testFile.json"), result.getResponse().getContentAsString());
				})
			.andExpect(status().isOk());
/*
		getJson(GET_DOC_URL + "?docUri=/testFile.xml")
			.andDo(
				result -> {
					assertEquals("application/xml", result.getResponse().getHeader("Content-Type"));
					assertThat(new WhitespaceStrippedSource(Input.from(getResource("mastering/data/testFile.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(result.getResponse().getContentAsString()).build())));
				})
			.andExpect(status().isOk());
*/
		clearStagingFinalAndJobDatabases();

		// now test w/o sufficient permissions
		installDoc(getHubClient().getFinalClient(), "mastering/data/testFile.json", "/testFile.json");
	//	installDoc(getAdminHubClient().getFinalClient(), "mastering/data/testFile.xml", "/testFile.xml");

		getJson(GET_DOC_URL + "?docUri=/testFile.json")
			.andExpect(status().isNotFound());

	//	getJson(GET_DOC_URL + "?docUri=/testFile.xml")
	//		.andExpect(status().isNotFound());
	}

	@Test
	void getHistory() throws Exception {
		
		//JsonNode jn = Mastering.on(this.getHubClient().getFinalClient()).getHistory("/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json");
		

		//loginAsTestUser();
		postJson(GET_HISTORY_URL, "{ \"uri\": \"/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json\" }")
			.andExpect(status().isUnauthorized());

	
	    loginAsDataHubDeveloper(); // should use test user with roles? 
	    
		postJson(GET_HISTORY_URL, "{ \"uri\": \"/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json\" }")
		.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					jsonAssertEquals(getResource("mastering/output/prov-history.json"), result.getResponse().getContentAsString());
				})
			.andExpect(status().isOk());
	}
	
	@Test
	void getNotifications() throws Exception {
		postJson(GET_NOTIFICATIONS_URL, "{}")
			.andExpect(status().isUnauthorized());

		//login();
		this.loginAsDataHubDeveloper(); // should be test user with specific roles?

		postJson(GET_NOTIFICATIONS_URL, "{}").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals(getResource("mastering/output/notifications.json"), result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());
	}
	

	/*
	 
	@Test
	void getNotification() throws Exception {
		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml")
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals(getResource("output/notification.json"), result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());
	}

	@Test
	void getNotifications() throws Exception {
		postJson(GET_NOTIFICATIONS_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_NOTIFICATIONS_URL, "{}").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals(getResource("output/notifications.json"), result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());
	}

	@Test
	void updateNotification() throws Exception {
		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml")
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals(getResource("output/notification.json"), result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());

		putJson(UPDATE_NOTIFICATION_URL, "{ \"uris\": [\"/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml\"], \"status\": \"read\" }")
			.andExpect(status().isOk());

		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals(getResource("output/notification-updated.json"), result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());
	}

	@Test
	void deleteNotifications() throws Exception {
		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml")
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals(getResource("output/notification.json"), result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());

		postJson(DELETE_NOTIFICATION_URL, "[\"/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml\"]")
			.andExpect(status().isOk());

		getJson(GET_NOTIFICATION_URL + "?uri=/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml")
			.andExpect(status().isNotFound());
	}

	@Test
	void blocks() throws Exception {
		postJson(GET_BLOCKS_URL, "[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]")
			.andExpect(status().isUnauthorized());

		postJson(SET_BLOCK_URL, "[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_BLOCKS_URL, "[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals("{\"/CoastalEmployees/55003.json\":[],\"/MountainTopEmployees/employee4.json\":[]}", result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());

		postJson(SET_BLOCK_URL, "[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]")
			.andExpect(status().isOk());

		postJson(GET_BLOCKS_URL, "[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals("{\"/CoastalEmployees/55003.json\":[\"/MountainTopEmployees/employee4.json\"],\"/MountainTopEmployees/employee4.json\":[\"/CoastalEmployees/55003.json\"]}", result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());

		postJson(UNSET_BLOCK_URL, "[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]")
			.andExpect(status().isOk());

		postJson(GET_BLOCKS_URL, "[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]").andDo(
			result -> {
				assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
				jsonAssertEquals("{\"/CoastalEmployees/55003.json\":[],\"/MountainTopEmployees/employee4.json\":[]}", result.getResponse().getContentAsString());
			})
			.andExpect(status().isOk());
	}
*/
	
//	@Test
//	void merge() {
//		clearStagingFinalAndJobDatabases();
//		HubClient hubClient = getNonAdminHubClient();
//		DatabaseClient finalDbClient = hubClient.getFinalClient();
//		installDoc(finalDbClient, "entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "Employee");
//		installDoc(finalDbClient, "entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "Employee");
//
//		postJson(MERGE_URL)
//
//	}
}
