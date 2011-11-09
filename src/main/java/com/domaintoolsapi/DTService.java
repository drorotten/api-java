package com.domaintoolsapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.jackson.JsonNode;
import org.omg.CORBA.portable.UnknownException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.domaintoolsapi.exceptions.BadRequestException;
import com.domaintoolsapi.exceptions.DomainToolsException;
import com.domaintoolsapi.exceptions.InternalServerException;
import com.domaintoolsapi.exceptions.NotAuthorizedException;
import com.domaintoolsapi.exceptions.NotFoundException;
import com.domaintoolsapi.exceptions.ServiceUnavailableException;
import com.domaintoolsapi.exceptions.UnkownException;

/**
 * This class allows to execute the DomainTools's requests
 * @author Julien SOSIN
 */
public class DTService {

	/**
	 * HttpURLConnection object used to do HTTP connection.
	 */
	private static HttpURLConnection httpConnection;
	/**
	 * Default line separator
	 */
	private static String lineSeparator ;
	private static URL url;

	protected static DTResponse execute(DTRequest domainToolsRequest) throws DomainToolsException{
		//If no format specified, set Object
		if(domainToolsRequest.getFormat().isEmpty()) domainToolsRequest.setFormat(DTConstants.OBJECT);
		getLineSeparator();		
		url = DTURLService.buildURL(domainToolsRequest);
		return doRequest(domainToolsRequest);
	}

	private static DTResponse doRequest(DTRequest domainToolsRequest) throws DomainToolsException{
		int response_code = 0;
		DTResponse domainToolsResponse = new DTResponse(domainToolsRequest.getFormat(), domainToolsRequest.getDomain(), domainToolsRequest.getProduct(), domainToolsRequest.getParameters(), domainToolsRequest.getParametersMap());
		StringBuilder sbResponse = new StringBuilder();
		String sLine = "";

		try{
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setRequestMethod("GET");
			httpConnection.setDoOutput(true);
			response_code = httpConnection.getResponseCode();

			if(response_code >= 400 ){ manageError(domainToolsRequest,response_code); }

			//Read the response
			InputStream  response = httpConnection.getInputStream();
			BufferedReader bufReader = new BufferedReader(new InputStreamReader(response));
			while ((sLine = bufReader.readLine()) != null){
				sbResponse.append(sLine);
				sbResponse.append(lineSeparator);
			}
			if(domainToolsRequest.getFormat().equals(DTConstants.XML)){
				domainToolsResponse.setXML(sbResponse.toString());
			}
			else if(domainToolsRequest.getFormat().equals(DTConstants.HTML)){
				domainToolsResponse.setHTML(sbResponse.toString());
			}
			else if(domainToolsRequest.getFormat().equals(DTConstants.OBJECT)){
				domainToolsResponse.setObject(DTNodesService.getDomainToolsNode(sbResponse.toString()));
			}
			else{
				domainToolsResponse.setJSON(sbResponse.toString());
			}
			//We set the response in the request to not reuse it later
			domainToolsRequest.setDomainToolsResponse(domainToolsResponse);

		}catch (ProtocolException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}finally{
			httpConnection.disconnect();
		}
		return domainToolsResponse;
	}

	private static void manageError(DTRequest domainToolsRequest, int response_code) throws DomainToolsException{
		String sLine;
		StringBuilder sbResponse = new StringBuilder();
		String errorMessage = "error message";
		//The request fail
		//We try to get some informations
		InputStream  errorResponse = httpConnection.getErrorStream();
		BufferedReader errorBufReader = new BufferedReader(new InputStreamReader(errorResponse));

		try {
			while ((sLine = errorBufReader.readLine()) != null){
				sbResponse.append(sLine);
				sbResponse.append(lineSeparator);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(domainToolsRequest.getFormat().equals(DTConstants.JSON)){
			JsonNode jsonNode = DTNodesService.getDomainToolsNode(sbResponse.toString());
			errorMessage = jsonNode.get("error").get("message").getTextValue();
		}
		else if(domainToolsRequest.getFormat().equals(DTConstants.XML)){
			DocumentBuilder parser = null;
			Document document = null;
			try {
				parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				document = parser.parse(new InputSource(new StringReader(sbResponse.toString())));
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			errorMessage = document.getElementsByTagName("message").item(0).getTextContent();
		}
		switch(response_code){
		case 400 : throw new BadRequestException(errorMessage);
		case 401 :
		case 403 : throw new NotAuthorizedException(errorMessage);
		case 404 : throw new NotFoundException(errorMessage);
		case 500 : throw new InternalServerException(errorMessage);
		case 503 : throw new ServiceUnavailableException(errorMessage);
		default : throw new UnkownException("Unkown exception");
		}
	}

	private static void getLineSeparator() {
		try{
			lineSeparator = System.getProperty("line.separator");
		}catch (Exception e){
			lineSeparator = "\n";
		}
	}
}
