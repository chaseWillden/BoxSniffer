/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package boxsniffer;

import boxsniffer.Session;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

/**
 *
 * @author willdech
 */
public final class SnifferTest {
    private Session session;
    
    // Stats variables
    private int totalDlapCalls;
    
    public SnifferTest() throws TransformerConfigurationException, ParserConfigurationException{
        setSession(new Session("Java Sniffer", "http://gls.agilix.com/dlap.ashx"));
        totalDlapCalls = 0;
    }
    
    /**
     * Get a list of all commands
     * @return
     * @throws TransformerException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException 
     */
    public Document listCommands() throws TransformerException, ParserConfigurationException, IOException, SAXException{
        return getSession().Get("listdomains", null);
    }
    
    /**
     * Returns logout information
     * @return
     * @throws TransformerException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException 
     */
    public Document logout() throws TransformerException, IOException, ParserConfigurationException, SAXException{
        return getSession().Logout();
    }
    
    /**
     * Login to agilix system
     * @param username
     * @param password
     * @param prefix
     * @return
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws IOException
     * @throws SAXException 
     */
    public boolean login(String username, String password, String prefix) throws ParserConfigurationException, TransformerException, IOException, SAXException{
        Document response = getSession().Login(prefix, username, password);
        return getSession().IsSuccess(response);
    }
    
    /**
     * Query elements based on text
     * @param query
     * @param doc
     * @return 
     */
    public Elements textQuery(String query, Document doc){
        return doc.getElementsContainingText(query);
    }
    
    /**
     * Return the size of a text query
     * @param query
     * @param doc
     * @return 
     */
    public int textQuerySize(String query, Document doc){
        return doc.getElementsContainingText(query).size();
    }
    
    /**
     * Query elements based on CSS selectors
     * @param query
     * @param doc
     * @return 
     */
    public Elements cssQuery(String query, Document doc){
        return doc.select(query);
    }
    
    /**
     * Returns the size of the CSS query
     * @param query
     * @param doc
     * @return 
     */
    public int cssQuerySize(String query, Document doc){
        return doc.select(query).size();
    }
    
    /**
     * Core for all Dlap Calls to aglix.com
     * @param call
     * @param params
     * @return
     * @throws TransformerException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException 
     */
    public Document dlapCalls(String call, Map<String, String> params) throws TransformerException, IOException, ParserConfigurationException, SAXException{
        Document response = getSession().Get(call, params);
        this.totalDlapCalls++;
        return response;
    }

    /**
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * @param session the session to set
     */
    public void setSession(Session session) {
        this.session = session;
    }
}
