/*
 * Author: Chase Willden
 *
 * Error Numbers:
 * 1: Unable to connect
 * 100+ - Logging in Issues
 * 200+ - Dlap Call Issues
 * 300+ - System Issues
 */
package boxsniffer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
public final class Sniffer {

    /**
     * The first letter 'd' indicates that this is a result from a Dlap
     * call.<div><br></div><div>Holds CourseActivityXML</div>
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    private List<Document> dCourseActivityDOM = new ArrayList<>();
    private List<String> errorLog = new ArrayList<>();
    private Document enrollmentList;
    private Document dCourseItemList;
    private Session session;
    private List<String> queriedItems = new ArrayList<>();
    private String baseUrl;
    private List<String> listCourses;
    private int totalDlap = 0;
    private double totalItems = 0.0;
    private int totalQueriedElements = 0;
    private double progress = 1.0;
    private boolean isRunning = false;
    private String baseCourseid = "";
    private boolean interrupt = false;
    private int linkCount = 0;
    private BlockingQueue<Element> queue = new LinkedBlockingQueue<>(3);
    private String query = "";
    private ThreadPool pool = new ThreadPool(Runtime.getRuntime().availableProcessors());
    private boolean showElements = true;
    private boolean showEnrollments = false;
    private int totalStudents = 0;
    private int totalActiveStudents = 0;
    private int activeStudents = 0;

    /**
     * Box Sniffer Constructor
     */
    public Sniffer() {
        this.listCourses = new ArrayList<>();
        try {
            this.session = new Session("Link Sniffer", "http://gls.agilix.com/dlap.ashx");
        } catch (TransformerConfigurationException | ParserConfigurationException ex) {
            addError("1", "Sniffer couldn't connect");
        }
    }

    public void run(final String query) {
        Document cil = getCourseItemList();
        this.totalItems = cil.getElementsByTag("item").size();
        this.isRunning = true;
        Elements items = cil.getElementsByTag("item");

        // Add to report CSV
        if (isShowElements()) {
            this.queriedItems.add("\nCourse Id, Item Id, Item Title, Total Elements, Link, Element");
        } else {
            this.queriedItems.add("\nCourse Id, Item Id, Item Title, Total Elements, Link");
        }

        queue = new LinkedBlockingQueue<>(3);
        this.query = query;
        for (final Element item : items) {
            // Each time it loops, it sets a wait, even if it's zero 
            Thread g = new Thread() {
                @Override
                public void run() {
                    try {
                        queue.put(item);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            getPool().addTask(g);
            getPool().addTask(new Consumer());
        }
    }

    /**
     * @return the linkCount
     */
    public int getLinkCount() {
        return linkCount;
    }

    /**
     * @param linkCount the linkCount to set
     */
    public void setLinkCount(int linkCount) {
        this.linkCount = linkCount;
    }

    /**
     * @return the pool
     */
    public ThreadPool getPool() {
        return pool;
    }

    /**
     * @param pool the pool to set
     */
    public void setPool(ThreadPool pool) {
        this.pool = pool;
    }

    /**
     * @return the showElements
     */
    public boolean isShowElements() {
        return showElements;
    }

    /**
     * @param showElements the showElements to set
     */
    public void setShowElements(boolean showElements) {
        this.showElements = showElements;
    }

    /**
     * @return the showEnrollments
     */
    public boolean isShowEnrollments() {
        return showEnrollments;
    }

    /**
     * @param showEnrollments the showEnrollments to set
     */
    public void setShowEnrollments(boolean showEnrollments) {
        this.showEnrollments = showEnrollments;
    }

    private class Consumer implements Runnable {

        @Override
        public void run() {
            try {
                String q = query;
                itemThread(queue.take(), q);
                progress++;
                // After looping through all items, set isRunning to false
                if (progress() > 99.0) {
                    isRunning = false;
                    getPool().stopThreads();
                    System.gc();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void itemThread(Element item, String query) {
        setWait();
        Elements typeTag = item.getElementsByTag("type");

        // Double check item if it has type
        if (!typeTag.isEmpty()) {
            String itemType = typeTag.get(0).text();

            // Check item type, needs to check all types
            if (!itemType.contains("AssetLink") && !itemType.contains("Folder")
                    && !itemType.contains("Lessons") && !itemType.contains("RssFeed")
                    && !itemType.contains("Shortcut") && !itemType.contains("Survey")) {

                // Get Item Resource
                String entityid = item.attr("resourceentityid").split(",")[0];
                String path = item.getElementsByTag("href").text();
                String content = dlapGetResource(entityid, path);

                if (content != null) {
                    // Get Broken Box Links
                    String id = item.attr("id");
                    String title = item.getElementsByTag("title").text();

                    // Gets query, checks item size
                    Elements eles = checkElementSize(getQuery(query, content));
                    if (eles != null) {
                        // Add to total
                        this.totalQueriedElements += eles.size();
                        // Adds csv to Report
                        String d = "\n" + entityid;
                        d += "," + id;
                        d += "," + title;
                        d += "," + eles.size();
                        d += ",https://byui.brainhoney.com/Frame/Component/CoursePlayer?enrollmentid=" + entityid + "&itemid=" + id;
                        if (isShowElements()){
                            d += "," + eles.outerHtml();
                        }
                        this.queriedItems.add(d);
                    }
                }
            } // If the activity is an AssetLink
            else if (itemType.contains("AssetLink")) {
                String url = item.getElementsByTag("href").get(0).text();

                // Create a simple webpage so parser can parse.
                String content = "<html><head></head><body><div><a href='" + url + "'>Asset Link</a><a href='https://google.com'>Google</a></div></body></html>";
                Elements eles = checkElementSize(getQuery(query, content));
                if (eles != null) {
                    // Add to total
                    this.totalQueriedElements += eles.size();

                    // Create report as CSV
                    String entityid = item.attr("resourceentityid").split(",")[0];
                    String id = item.attr("id");
                    String title = item.getElementsByTag("title").text();
                    String d = "\n" + entityid;
                    d += "," + id;
                    d += "," + title;
                    d += "," + eles.size();
                    d += ",https://byui.brainhoney.com/Frame/Component/CoursePlayer?enrollmentid=" + entityid + "&itemid=" + id;
                    this.queriedItems.add(d);
                }
            }
        }
    }
    
    public void auditEnrollments(){
        Elements enrollments = this.enrollmentList.select("enrollment[privileges=2228225]");
        this.activeStudents = 0;
        this.totalStudents = enrollments.size();
        for (Element e : enrollments){
            this.totalActiveStudents++;
            this.activeStudents++;
        }
        this.enrollmentList = null;
    }

    /**
     * Parse as webpage and queries document.
     *
     * @param query
     * @param content
     * @return
     */
    public Elements getQuery(String query, String content) {
        return Jsoup.parse(content).select(query);
    }

    /**
     * Check size of Elements
     *
     * @param eles
     * @return
     */
    public Elements checkElementSize(Elements eles) {
        if (eles.size() > 0) {
            return eles;
        }
        return null;
    }

    // Dlap Calls
    /**
     * Login - DLAP Call.
     *
     * @param username
     * @param password
     * @param prefix
     * @return
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
            addError("100", "Couldn't login");
        }
        if (!Session.IsSuccess(result)) {
            addError("101", "Unable to login: " + Session.GetMessage(result));
            return false;
        }
        return true;
    }

    /**
     * Get the course resource
     *
     * @param entityid
     * @param path
     * @return
     */
    public String dlapGetResource(String entityid, String path) {
        if (entityid.isEmpty() || path.isEmpty()) {
            return null;
        }
        Map< String, String> params = new HashMap<>();
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
            addError("200", "Resource couldn't be downloaded");
        }
        return null;
    }

    /**
     * Dlap Logout
     */
    public void logout() {
        try {
            this.session.Logout();
            this.totalDlap++;
        } catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            addError("102", "Couldn't Logout");
        }
    }
    
    /**
     * Dlap call to get all enrollments
     * @param courseId 
     */
    public void DlapListEntityEnrollments(String courseId){
        this.baseCourseid = courseId;
        Map< String, String> getEnrollments = new HashMap<>();
        getEnrollments.put("entityid", courseId);
        getEnrollments.put("allstatus", "true");
        session.setIsHtml(true);
        try {
            this.enrollmentList = session.Get("listentityenrollments", getEnrollments);
            this.totalDlap++;
        } catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            addError("201", "Couldn't execute getItemList Dlap Call");
        }
    }

    /**
     * Dlap, Returns list of Items
     *
     * @param courseId
     */
    public void dlapGetItemList(String courseId) {
        // TODO : to implement	
        this.baseCourseid = courseId;
        Map< String, String> getitemlist = new HashMap<>();
        getitemlist.put("entityid", courseId);
        session.setIsHtml(true);
        try {
            this.dCourseItemList = session.Get("getitemlist", getitemlist);
            this.totalDlap++;
        } catch (TransformerException | IOException | ParserConfigurationException | SAXException ex) {
            addError("201", "Couldn't execute getItemList Dlap Call");
        }
    }

    /**
     * Dlap, sets list of domain level courses
     *
     * @param domainid
     * @return
     */
    public boolean getDomainCourses(String domainid) {
        Map< String, String> params = new HashMap<>();
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
            addError("202", "Couldn't get list of courses");
        }
        return false;
    }

    // Object / Attribute related functions
    // Getters / Setters
    public String displayErrorMsgs() {
        String display = "";
        display += "Error Messages: \n";
        int size = this.errorLog.size();
        String info = "";
        for (int i = 0; i < size; i++) {
            display = this.errorLog.get(i).toString();
        }
        return display;
    }

    /**
     * Returns an Audit of the progress and query
     *
     * @return
     */
    public String displayCourseAudit() {
        // TODO : to implement
        String display = "Analytics\n";
        display += "\nTotal Dlap Calls," + this.totalDlap;
        if (this.isShowEnrollments()){
            display += "\n\nEnrollment Audit";
            display += "\nTotal Students," + this.totalStudents;
            display += "\nTotal Active Students for this course," + this.activeStudents;
            return display;
        }
        if (this.queriedItems.isEmpty()) {
            return display + "\nNo Queried Items";
        }
        display += "\nAudit Report";
        display += "\nTotal Items in Course," + this.totalItems;
        display += "\nTotal Queried Items," + (this.queriedItems.size() - 1);
        display += "\nTotal Queried Elements, " + (this.totalQueriedElements) + "\n";
        
        int size = this.queriedItems.size();
        for (int i = 0; i < size; i++) {
            String linkInfo = this.queriedItems.get(i).toString();
            display += linkInfo;
        }
        return display;
    }

    /**
     * Set Error Message
     *
     * @param errorNum
     * @param errorMsg
     */
    public void addError(String errorNum, String errorMsg) {
        this.errorLog.add(errorNum + ": " + errorMsg);
    }

    /**
     * Set the base URL
     *
     * @param url
     */
    public void setUrl(String url) {
        this.baseUrl = url;
    }

    /**
     * Get Course Item List
     *
     * @return
     */
    public Document getCourseItemList() {
        return this.dCourseItemList;
    }

    /**
     * List Courses
     *
     * @return
     */
    public List<String> getAllCourses() {
        return Collections.unmodifiableList(this.listCourses);
    }

    /**
     * Get the progress of the sniffer
     *
     * @return
     */
    public double progress() {
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.parseDouble(df.format((this.progress / this.totalItems) * 100));
    }

    /**
     * Checks if tool is still running or if it completed its cycle
     *
     * @return
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * Interrupt for however long. Called by setWait with an infinite loop
     *
     * @param i
     */
    public void setInterrupt(boolean i) {
        this.interrupt = i;
    }

    /**
     * Pause the thread / Tool
     */
    public void setWait() {
        try {
            while (this.interrupt) {
                Thread.sleep(10);
            }
        } catch (InterruptedException ex) {
            addError("300", "Unable to pause thread");
        }
    }

    /**
     * Reset the values
     */
    public void reset() {
        dCourseActivityDOM = new ArrayList<>();
        errorLog = new ArrayList<>();
        dCourseItemList = null;
        queriedItems = new ArrayList<>();
        baseUrl = "";
        listCourses = new ArrayList<>();
        totalDlap = 0;
        totalItems = 0.0;
        progress = 1.0;
        isRunning = false;
        baseCourseid = "";
        totalQueriedElements = 0;
    }
}
