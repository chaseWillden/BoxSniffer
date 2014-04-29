/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package boxsniffer;

import java.awt.GridLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 *
 * @author willdech
 */
public class Admin extends javax.swing.JFrame {
    
    private BoxSniffer bs = new BoxSniffer();
    private String domainid;
    /**
     * Creates new form Admin
     */
    public Admin() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        displayCourseList = new javax.swing.JList();
        filterCourses = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        changeDomainIdBtn = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Brainhoney Admin");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        displayCourseList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Select Course" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        displayCourseList.setToolTipText("");
        jScrollPane1.setViewportView(displayCourseList);

        filterCourses.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                filterCoursesKeyTyped(evt);
            }
        });

        jLabel1.setText("Filter:");

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        changeDomainIdBtn.setText("Change Domain Id");
        changeDomainIdBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeDomainIdBtnActionPerformed(evt);
            }
        });
        jMenu2.add(changeDomainIdBtn);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(766, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(filterCourses)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(25, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterCourses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:
        //setExtendedState(this.MAXIMIZED_BOTH);
        checkCredentials();
    }//GEN-LAST:event_formWindowOpened

    private void changeDomainIdBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeDomainIdBtnActionPerformed
        // TODO add your handling code here:
        changeDomainId();
    }//GEN-LAST:event_changeDomainIdBtnActionPerformed

    private void filterCoursesKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filterCoursesKeyTyped
        // TODO add your handling code here:        
        Thread get = new Thread(){
            @Override
            public void run() {
                populateCourses(filterCourses.getText());
            }  
        };
        get.start();
    }//GEN-LAST:event_filterCoursesKeyTyped
    
    public void checkCredentials(){
        boolean login = loginPopup();
        if (!login){           
            int reply = JOptionPane.showConfirmDialog(null, "Credentials were incorrect, would you like to try again?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (reply != 0){
                System.exit(-1);
            }
            else{
                checkCredentials();                
            }
        }
        else{
            populateCourses("");
        }
    }
    
    public void populateCourses(String filter){
        bs.getDomainCourses(domainid);
        List allCourses = bs.getAllCourses();
        final String[] strings = new String[allCourses.size()];
        int next = 0;
        for (Object allCourse : allCourses) {
            if (filter.isEmpty()) {
                strings[next++] = allCourse.toString();
            } else {
                if (allCourse.toString().toLowerCase().contains(filter.toLowerCase())) {
                    strings[next++] = allCourse.toString();
                }                
            }
        }
        displayCourseList.setModel(new javax.swing.AbstractListModel() {
            @Override
            public int getSize() { return strings.length; }
            @Override
            public Object getElementAt(int i) { return strings[i]; }
        });
    }
    
    public boolean changeDomainId(){
        final JTextField ndomainid = new JTextField();
        JPanel gridLayout = new JPanel(new GridLayout(2, 1));
        gridLayout.add(new JLabel("Domain id:"));
        gridLayout.add(ndomainid);

        ndomainid.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorRemoved(AncestorEvent pEvent) {
            }

            @Override
            public void ancestorMoved(AncestorEvent pEvent) {
            }

            @Override
            public void ancestorAdded(AncestorEvent pEvent) {
                ndomainid.requestFocusInWindow();
            }
        });

        int option = JOptionPane.showConfirmDialog(null, gridLayout, "Change Domain Id",
            JOptionPane.OK_CANCEL_OPTION);
        this.domainid = ndomainid.getText();
        return true;
    }
    
    public boolean loginPopup(){
        final JTextField username = new JTextField();
        JTextField password = new JPasswordField();
        JTextField prefix = new JTextField();
        JTextField domainid = new JTextField();
        JPanel gridLayout = new JPanel(new GridLayout(6, 1));
        gridLayout.add(new JLabel("Username:"));
        gridLayout.add(username);
        gridLayout.add(new JLabel("Password:"));
        gridLayout.add(password);
        gridLayout.add(new JLabel("Prefix:"));
        gridLayout.add(prefix);
        gridLayout.add(new JLabel("Domain id:"));
        gridLayout.add(domainid);

        username.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorRemoved(AncestorEvent pEvent) {
            }

            @Override
            public void ancestorMoved(AncestorEvent pEvent) {
            }

            @Override
            public void ancestorAdded(AncestorEvent pEvent) {
                username.requestFocusInWindow();
            }
        });

        int option = JOptionPane.showConfirmDialog(null, gridLayout, "Login",
            JOptionPane.OK_CANCEL_OPTION);
        this.domainid = domainid.getText();
        if (option == JOptionPane.OK_OPTION) {
            return bs.login(username.getText(), password.getText(), prefix.getText());
        } else {
            return false;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem changeDomainIdBtn;
    private javax.swing.JList displayCourseList;
    private javax.swing.JTextField filterCourses;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
