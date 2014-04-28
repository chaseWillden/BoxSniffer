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
public class BoxSniffer {

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
    private int itemsSelected = 0;
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
    public BoxSniffer() {
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
        totalDlap = 0;
        totalItems = 0.0;
        progress = 1.0;
        isRunning = false;
        baseCourseid = "";
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
        String display = "Analytics\n";
        display += "\nTotal Dlap Calls," + this.totalDlap;
        if (this.brokenLinks.isEmpty()) {
            return display + "\nNo Queried Items";
        }
        display += "\nAudit Report";
        display += "\nTotal Queried Items," + (this.brokenLinks.size() - 1) + "\n";
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
    
    public Elements getQuery(String query, String content){
        return Jsoup.parse(content).select(query);
    }
    
    public Elements checkBoxLinks(Elements eles){
        if (eles.size() > 0){
            return eles;
        }
        return null;
    }
    
    public void run(String query) {
        Document cil = getCourseItemList();
        this.totalItems = cil.getElementsByTag("item").size();
        this.isRunning = true;
        Elements items = cil.getElementsByTag("item");
        this.brokenLinks.add("\nCourse Id, Item Id, Item Title, Link");
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
                    String title = item.getElementsByTag("title").text();
                    if (content != null) {
                        // Get Broken Box Links
                        Elements eles = checkBoxLinks(getQuery(query, content));
                        if (eles != null && !eles.toString().contains("embed")){
                            String d = "\n" + entityid;
                            d += "," + id;
                            d += "," + title;
                            d += ",https://byui.brainhoney.com/Frame/Component/CoursePlayer?enrollmentid=" + entityid + "&itemid=" + id;
                            this.brokenLinks.add(d);
                        }
                    }
                }
                else if(itemType.contains("AssetLink")){
                    String url = item.getElementsByTag("href").get(0).text();
                    // If broken
                    String content = "<html><head></head><body><div><a href='" + url +"'>Asset Link</a><a href='https://google.com'>Google</a></div></body></html>";
                    Elements eles = checkBoxLinks(getQuery(query, content));
                    if (eles != null && !eles.toString().contains("embed")){
                        String entityid = item.attr("resourceentityid").split(",")[0];
                        String id = item.attr("id");
                        String title = item.getElementsByTag("title").text();
                        String d = "\n" + entityid;
                        d += "," + id;
                        d += "," + title;
                        d += ",https://byui.brainhoney.com/Frame/Component/CoursePlayer?enrollmentid=" + entityid + "&itemid=" + id;
                        this.brokenLinks.add(d);
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
