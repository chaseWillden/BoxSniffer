package boxsniffer;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Session {

    // The user agent for this application
    // You should set this to identify the calling application for logging
    private final String fAgent;
    // The URL to the DLAP server
    private final String fServer;
    // Request timeout in milliseconds
    // Defaults to 30000 (30 seconds)
    private final int fTimeout;
    // Outputs all requests and responses to the console for trouble shooting
    private final Boolean fVerbose;
    // The cookies for the session
    private List<String> fCookies = null;

    private Transformer fTransformer = null;
    private DocumentBuilder fDocBuilder = null;

    // Create a new session object
    public Session(String agent, String server) throws TransformerConfigurationException, ParserConfigurationException {
        fAgent = agent;
        fServer = server;
        fTimeout = 30000;
        fVerbose = false;

        CreateTransformerAndDocumentBuilder();
    }

    // Create a new session object
    public Session(String agent, String server, int timeout, Boolean verbose) throws TransformerConfigurationException, ParserConfigurationException {
        fAgent = agent;
        fServer = server;
        fTimeout = timeout;
        fVerbose = verbose;

        CreateTransformerAndDocumentBuilder();
    }

    private void CreateTransformerAndDocumentBuilder() throws TransformerConfigurationException, ParserConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        fTransformer = transformerFactory.newTransformer();
        fTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        fTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
        fTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        fDocBuilder = docFactory.newDocumentBuilder();
    }

    // Login to DLAP. Call Logout to close session.
    public org.jsoup.nodes.Document Login(String prefix, String username, String password)
            throws ParserConfigurationException, TransformerException, IOException, SAXException {
        Document doc = fDocBuilder.newDocument();
        Element rootElement = doc.createElement("request");
        doc.appendChild(rootElement);
        rootElement.setAttribute("cmd", "login");
        rootElement.setAttribute("username", prefix + "/" + username);
        rootElement.setAttribute("password", password);
        return Post(null, doc);
    }

    // Logout of DLAP
    public org.jsoup.nodes.Document Logout()
            throws TransformerException, IOException, ParserConfigurationException, SAXException {
        org.jsoup.nodes.Document result = Get("logout", null);
        fCookies = null;
        return result;
    }

    // Makes a GET request to DLAP
    public org.jsoup.nodes.Document Get(String cmd, Map<String, String> parameters)
            throws TransformerException, IOException, ParserConfigurationException, SAXException {
        String query = "?cmd=" + cmd;
        if (parameters != null) {
            for (String key : parameters.keySet()) {
                query += "&" + key + "=" + parameters.get(key);
            }
        }
        return Request(query, null);
    }

    // Makes a POST request to DLAP
    public org.jsoup.nodes.Document Post(String cmd, Document xml)
            throws TransformerException, IOException, ParserConfigurationException, SAXException {
        String query = (cmd == null || cmd.length() == 0) ? "" : ("?cmd=" + cmd);
        return Request(query, xml);
    }

    // Makes a raw request to DLAP
    org.jsoup.nodes.Document Request(String query, Document postData)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        if (query.contains("path=ht")) {
            return null;
        }
        if (fVerbose) {
            System.out.println();
            System.out.println("Request: " + fServer + query);
            if (postData != null) {
                StreamResult result = new StreamResult(System.out);
                DOMSource source = new DOMSource(postData);
                fTransformer.transform(source, result);
                System.out.println();
            }
            System.out.println();
        }

        URL url = new URL(fServer + query);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", fAgent);

        // Add any cookies
        if (fCookies != null) {
            for (String cookie : fCookies) {
                connection.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
            }
        }

        connection.setReadTimeout(fTimeout);
        connection.setDoInput(true);

        if (postData != null) {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setDoOutput(true);
            StreamResult result = new StreamResult(connection.getOutputStream());
            DOMSource source = new DOMSource(postData);
            fTransformer.transform(source, result);
        } else {
            connection.setRequestMethod("GET");
        }

        // Get the response
        connection.connect();
        if (connection.getResponseCode() != 200) {
            return null;
        }
        InputStream responseStream = connection.getInputStream();
        String s = getStringFromInputStream(responseStream);
        org.jsoup.nodes.Document response = null;
        if (isHtml) {
            response = Jsoup.parse(s);
        } else {
            Parser xparser = Parser.xmlParser();
            xparser.parseInput(s, fServer + query);
            response = Jsoup.parse(s);
        }

        //Document response = fDocBuilder.parse();
        // Store any cookies that get returned
        List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
        if (cookies != null && !cookies.isEmpty()) {
            fCookies = cookies;
        }

        if (fVerbose) {
            System.out.println("Response:");
//        	StreamResult result = new StreamResult(System.out);
//        	DOMSource source = new DOMSource((Node) response);
//        	fTransformer.transform(source, result);
            System.out.println(response.toString());
            System.out.println();
            System.out.println();
        }

        return response;
    }

    public void setIsHtml(boolean setHTML) {
        this.isHtml = setHTML;
    }

    protected boolean isHtml = false;

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }

        return sb.toString();

    }

    // Checks if the DLAP call was successful
    public static Boolean IsSuccess(org.jsoup.nodes.Document result) {
        return "OK".equals(result.getElementsByTag("response").get(0).attr("code"));
    }

    // Returns the error message for a failed DLAP call
    public static String GetMessage(org.jsoup.nodes.Document result) {
        // Find the first response with a code not equal to OK and return the message
        Elements elements = result.getElementsByTag("response");
        for (org.jsoup.nodes.Element e : elements){
            if (!e.attr("code").equals("OK")){
                return e.attr("message");
            }
        }
        return "Unknown error";
    }
}
