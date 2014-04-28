/*
 * Author: Chase Willden
 * 
 * Error Numbers:
 * 132: Ping Error
 * 133: Login Error
 * 136: Dlap getresource error
 */
package boxsniffer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

/**
 *
 * @author willdech
 */
public class LinkSnifferNew {

    /**
     * The first letter 'd' indicates that this is a result from a Dlap
     * call.<div><br></div><div>Holds CourseActivityXML</div>
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    private List dCourseActivityDOM = new ArrayList();
    private List errorLog = new ArrayList();
    private Document dCourseItemList;
    private Session session;
    private List brokenLinks = new ArrayList();
    private String baseUrl;
    private List allCourses = new ArrayList();
    private List listCourses = new ArrayList();
    private int totalPinged = 0;
    private int totalExt = 0;
    private int totalInt = 0;
    private int totalImg = 0;
    private int totalDlap = 0;
    private double totalItems = 0.0;
    private double progress = 1.0;
    private boolean isRunning = false;
    private String baseCourseid = "";
    private String uName = "";
    private boolean interrupt = false;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc --> @generated
     */
    public LinkSnifferNew() {
        try {
            this.session = new Session("Link Sniffer", "http://gls.agilix.com/dlap.ashx");
        } catch (TransformerConfigurationException | ParserConfigurationException ex) {
            Logger.getLogger(LinkSnifferNew.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void reset(){
        dCourseActivityDOM = new ArrayList();
        errorLog = new ArrayList();
        dCourseItemList = null;
        brokenLinks = new ArrayList();
        baseUrl = "";
        allCourses = new ArrayList();
        listCourses = new ArrayList();
        totalPinged = 0;
        totalExt = 0;
        totalInt = 0;
        totalItems = 0.0;
        progress = 1.0;
        isRunning = false;
        baseCourseid = "";
    }
    /**
     * A function to ping the url.<div><br></div><div>Returns true if a 200
     * message is returned.</div>
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param address
     * @return
     * @generated
     * @ordered
     */
    public boolean pingUrl(String address) {
        this.totalPinged++;
        try {
            if (address.contains("///") || address.isEmpty() || address == null || address.contains("data:")) {
                return false;
            }
            int status = Jsoup.connect(address).ignoreContentType(true).execute().statusCode();
            return status < 400;

        } catch (IOException ex) {
            addError("132", "Unable to pings the address: " + address);
        }
        return false;
    }

    /**
     * Adds Errors to the Error log.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param errorNum
     * @param errorMsg
     * @generated
     * @ordered
     */
    public void addError(String errorNum, String errorMsg) {
        this.errorLog.add(errorNum + ": " + errorMsg);
    }

    /**
     * Sets the base url
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param url
     * @generated
     * @ordered
     */
    public void setUrl(String url) {
        this.baseUrl = url;
    }

    /**
     * Login to Brainhoney's API through "http://gls.agilix.com/dlap.ashx".
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param username
     * @param password
     * @param prefix
     * @return
     * @generated
     * @ordered
     */
    public boolean login(String username, String password, String prefix) {
        // Login
        org.jsoup.nodes.Document result = null;
        if (username.isEmpty() || password.isEmpty() || prefix.isEmpty()) {
            return false;
        }
        try {
            result = session.Login(prefix, username, password);
            this.totalDlap++;
            System.out.println(result);
        } catch (ParserConfigurationException | TransformerException | IOException | SAXException ex) {
            Logger.getLogger(LinkSnifferNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!Session.IsSuccess(result)) {
            addError("133", "Unable to login: " + Session.GetMessage(result));
            return false;
        }
        this.uName = result.getElementsByTag("user").get(0).attr("firstname") + " " + result.getElementsByTag("user").get(0).attr("lastname");
        return true;
    }
    
    public String getUser(){
        return this.uName;
    }

    /**
     * Using Brainhoney's DLAP call, it gets the "html" content of the activity.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param entityid
     * @param path
     * @return
     * @generated
     * @ordered
     */
    public String dlapGetResource(String entityid, String path) {
        if (entityid.isEmpty() || path.isEmpty()) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        params.put("entityid", entityid);
        params.put("path", path);
        try {
            Document r = session.Get("getresource", params);
            this.totalDlap++;
            if (r == null) {
                return null;
            }
            return r.toString();
        } catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            addError("136", "Resource couldn't be downloaded");
        }
        return null;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc --> @generated @ordered
     */
    public void logout() {
        try {
            this.session.Logout();
            this.totalDlap++;
        } catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(LinkSnifferNew.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a list of links in the activity
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param elements
     * @return
     * @generated
     * @ordered
     */
    public List getLinks(Elements elements) {
        List links = new ArrayList();
        for (Element ele : elements) {
            if (!ele.attr("href").isEmpty()) {
                links.add(ele.attr("href"));
            }
            if (!ele.attr("src").isEmpty()) {
                links.add(ele.attr("src"));
            }
        }
        return links;
    }

    /**
     * Returns a list of the broken links within the course
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param courseContent
     * @param activity
     * @generated
     * @ordered
     */
    public void processBrokenLinks(String courseContent, String activity) {
        Document parsed = Jsoup.parse(courseContent);
        Elements hrefs = parsed.getElementsByAttribute("href");
        Elements srcs = parsed.getElementsByAttribute("src");
        List hrefList = getLinks(hrefs);
        List srcList = getLinks(srcs);
        if (!hrefList.isEmpty()) {
            int hrefsize = hrefList.size();
            for (int i = 0; i < hrefsize; i++) {
                String url = formatUrl(hrefList.get(i).toString(), true);
                if (!pingUrl(url)) {
                    String a = "\n\nurl: " + url;
                    a += "\nLink Text: " + hrefs.get(i).text();
                    a += "\nLink: " + hrefs.get(i).attr("href");
                    a += "\nLocation url: https://byui.brainhoney.com/Frame/Component/CoursePlayer?enrollmentid=" + this.baseCourseid + "&itemid=" + activity;
                    this.brokenLinks.add(a);
                }
            }
        }
        if (!srcList.isEmpty()) {
            int srcsize = srcList.size();
            for (int i = 0; i < srcsize; i++) {
                String url = formatUrl(srcList.get(i).toString(), true);
                if (!pingUrl(url)) {
                    String a = "\n\nurl: " + url;
                    a += "\nLink Text: " + srcs.get(i).text();
                    a += "\nLink: " + srcs.get(i).attr("src");
                    a += "\nLocation url: https://byui.brainhoney.com/Frame/Component/CoursePlayer?enrollmentid=" + this.baseCourseid + "&itemid=" + activity;
                    this.brokenLinks.add(a);
                }
            }
        }
        linkCount++;
    }
    
    public void getImages(String courseContent, String id){
        Document parsed = Jsoup.parse(courseContent);
        this.totalImg += parsed.getElementsByTag("img").size();
    }

    protected int linkCount = 0;

    /**
     * get list of all items within a course using the Brainhoney Dlap call
     * "getitemlist" and setting it into a global variable which can be accessed
     * by getItemInfo
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param courseId
     * @generated
     * @ordered
     */
    public void dlapGetItemList(String courseId) {
        // TODO : to implement	
        this.baseCourseid = courseId;
        Map<String, String> getitemlist = new HashMap<>();
        getitemlist.put("entityid", courseId);
        session.setIsHtml(true);
        try {
            this.dCourseItemList = session.Get("getitemlist", getitemlist);
            this.totalDlap++;
        } catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(LinkSnifferNew.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get the list of course items set by dlapGetItemList()
     *
     * @return
     * @generated
     * @ordered
     */
    public Document getCourseItemList() {
        return this.dCourseItemList;
    }

    /**
     * Returns the dItemInfo
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return
     * @generated
     * @ordered
     */
    public List getCourseActivityDOM() {
        return this.dCourseActivityDOM;
    }

    public String bhLink(String id){
        Map<String, String> params = new HashMap<>();
        params.put("entityid", baseCourseid);
        params.put("itemid", id);
        try {
            Document all = session.Get("getitem", params);
            this.totalDlap++;
            if (all.getElementsByTag("href") == null || all.getElementsByTag("href").size() < 1){
                return "https://google.com";
            }
            String href = "http://gls.agilix.com/dlap.ashx?cmd=getresource&entityid=" + baseCourseid + "&path=" + all.getElementsByTag("href").get(0).text();
            Map<String, String> newParams = new HashMap<>();
            newParams.put("entityid", baseCourseid);
            newParams.put("path", all.getElementsByTag("href").get(0).text());
            if (all.getElementsByTag("href").get(0).text().contains("://")){
                return all.getElementsByTag("href").get(0).text();
            }
            Document a = session.Get("getresource", newParams);            
            this.totalDlap++;
            if (a == null){
                // Broken on purpose
                System.out.println(all.toString());
                return "https://google.com/brokenOnPurpose.html";
            }            
            if (a.getElementById("header") != null){
                if (a.getElementById("header").toString().contains("Error")){
                    // Broken on purpose
                    return "https://google.com/brokenOnPurpose.html";
                }
            }
            else{
                // The link worked, but I didn't want to figure this out, it works.
                return "https://google.com";
            }
        }   catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(LinkSnifferNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "https://google.com";
    }
    
    /**
     * Formats the URL to http or https then returns the new formatted url
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param address
     * @param https
     * @return
     * @generated
     * @ordered
     */
    public String formatUrl(String address, boolean https) {

        if (address.contains("[~]") || address.matches("(.*)%5B%7E%5D(.*)")) {
            String path = "";
            if (address.contains("[~]")){
                path = address.split("]")[1];
                if (address.split("]")[1].contains("?")){
                    path = address.split("]")[1].split("\\?")[0];
                }
            }
            else if (address.matches("(.*)%5B%7E%5D(.*)")){
                path = address.split("%5D")[1];
                if (address.split("%5D")[1].contains("?")){
                    path = address.split("]")[1].split("\\?")[0];
                }
            }
            this.totalInt++;
            
            String newForm = "http://gls.agilix.com/dlap.ashx?cmd=getresource&entityid=" + baseCourseid + "&path=assets" + path;
            return newForm;
        } else if (address.contains("javascript:")) {
            this.totalInt++;
            if (address.contains("navToItem")){
                String id = address.split("'")[1];                
                id = id.split("'")[0];
                return bhLink(id);
            }
            return "https://bing.com";
        }
        else if (address.contains(" ") || address.substring(0, 1).contains("#")){
            // Check for anchor links
            return "https://google.com";
        }
        else if (address.substring(0, 2).contains("//")){
            return "http:" + address;
        }
        else if (!address.contains("://")){
            return "http://" + address;
        }
        else{
            return address;
        }
    }

    /**
     * Returns a string of all the broken links
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return
     * @generated
     * @ordered
     */
    public String displayBrokenLinks() {
        // TODO : to implement
        String display = "\n==== Analytics:";
        display += "\nTotal Links Pinged: " + this.totalPinged;
        display += "\nTotal Dlap Calls: " + this.totalDlap;
        display += "\nTotal total external links: " + this.totalExt;
        display += "\nTotal total internal links: " + this.totalInt;
        display += "\nTotal Images: " + this.totalImg;
        if (this.brokenLinks.isEmpty()) {
            return display + "\n\n==== No broken links";
        }
        display += "\n\n==== Broken Links:";
        display += "\nTotal broken links: " + this.brokenLinks.size() + "\n";
        int size = this.brokenLinks.size();
        for (int i = 0; i < size; i++) {
            String linkInfo = this.brokenLinks.get(i).toString();
            display += linkInfo;
        }
        return display;
    }

    public double progress() {
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.parseDouble(df.format((this.progress / this.totalItems) * 100));
    }

    public boolean getDomainCourses(String domainid) {
        Map<String, String> params = new HashMap<>();
        params.put("domainid", domainid);
        params.put("limit", "0");
        try {
            Document all = session.Get("listcourses", params);
            this.totalDlap++;
            if (all.getElementsByTag("response").get(0).attr("code").equals("OK")) {
                Elements courses = all.getElementsByTag("course");
                for (Element course : courses) {
                    this.listCourses.add(course.attr("title") + "::" + course.attr("id"));
                }
                return true;
            } else {
                return false;
            }
        } catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            addError("483", "Couldn't get list of courses");
        }
        return false;
    }

    public boolean isRunning() {
        return this.isRunning;
    }
    
    public void setWait(){
        try {
            while(this.interrupt){
                Thread.sleep(10);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(LinkSnifferNew.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setInterrupt(boolean i){
        this.interrupt = i;
    }
    
    public void run() {
        Document cil = getCourseItemList();
        this.totalItems = cil.getElementsByTag("item").size();
        this.isRunning = true;
        Elements items = cil.getElementsByTag("item");
        for (Element item : items) {
            setWait();
            this.progress++;
            System.out.println("Progress: " + progress() + "%");
            Elements typeTag = item.getElementsByTag("type");
            if (!typeTag.isEmpty()) {
                String itemType = typeTag.get(0).text();
                if (itemType.contains("Resource") || itemType.contains("Assignment") || itemType.contains("Discussion") || itemType.contains("Homework")) {
                    String entityid = item.attr("resourceentityid").split(",")[0];
                    String path = item.getElementsByTag("href").text();
                    String id = item.attr("id");
                    String content = dlapGetResource(entityid, path);
                    if (content != null) {
                        processBrokenLinks(content, id);
                        getImages(content, id);
                    }
                }
                else if(itemType.contains("AssetLink")){
                    String url = item.getElementsByTag("href").get(0).text();
                    if (!pingUrl(url)) {
                        String a = "\n\nurl: " + url;
                        String activity = item.attr("id");
                        a += "\nLocation url: https://byui.brainhoney.com/Frame/Component/CoursePlayer?enrollmentid=" + this.baseCourseid + "&itemid=" + activity;
                        this.brokenLinks.add(a);
                    }
                }
            }
        }
        String display = displayBrokenLinks();
        if (display.isEmpty()) {
            System.out.println("Your Course contains no broken links");
        } else {
            System.out.println(display);
        }
        this.isRunning = false;        
    }

    public List getAllCourses() {
        return this.listCourses;
    }
}
