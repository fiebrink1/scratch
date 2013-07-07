/*
 * This is the main GUI for the finger therapy. Runs for both DTW
 * (gesture recognition) and target acquisition; can drive either game using
 * simulated key presses or music (by sending OSC messages to ChucK)
 */

/*
 * PsMoveGestureRecognizerTrainingGUI.java
 *
 * Created on Aug 19, 2011, 4:14:49 PM
 */
package psm;

import psm.util.FileChooserWithExtension;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author fiebrink
 */
public class PsMoveGestureRecognizerTrainingGUI extends javax.swing.JFrame {

    Robot robot;
    PSMoveDTWGestureRecognizer analyzer;
    int numClasses = 4;
    int numFeatures = 9;
    String[] actionOptions;
    String[] gestureNames;
    GestureRow[] rows;
    int numCurrentRows = 0;
    int sendPort = 6449;
    OSCPortOut sender;
    protected boolean constantTrigger = false;


    private void updateGUIForLoadedGestureRecognizer() {
        numClasses = analyzer.numClasses;
        numFeatures = analyzer.numFeatures;
        double loadedThreshold = analyzer.getMatchThreshold();
        System.out.println("In update, thresh is " + loadedThreshold);
        double newDist = analyzer.getMaxDistance();
        if (newDist > analyzer.getMatchThreshold()) {
              sliderThreshold.setMaximum((int) (100 * newDist) + 1);
        } else {
              sliderThreshold.setMaximum((int) (100 * analyzer.getMatchThreshold()) + 1);
        }
        System.out.println("Slider max set to " + sliderThreshold.getMaximum() * .01);

        int match = analyzer.getContinuousMatch();
                    updateStatusBars(match);
        if (!constantTrigger) {
                   useClassificationResult(match);
        }
        
        updateDistances(analyzer.getLastDistances());
         if (constantTrigger) {
                   useClassificationResult(analyzer.getContinuousMatch());
               }
        // sliderThreshold.setValue((int)(analyzer.getMatchThreshold() * 100));
         analyzer.setMatchThreshold(loadedThreshold);
         sliderThreshold.setValue((int)(analyzer.getMatchThreshold() * 100));
    
    }

    //What do you want to do with it?
    public enum ControlMode {

        DIRECTION_PAD,
        MUSIC_TRIGGER
    };

    //Use DTW or just look for targets?
    public enum AnalysisMode {
        DTW,
        TARGET
    };
    protected ControlMode controlMode = ControlMode.DIRECTION_PAD;
    protected AnalysisMode analysisMode = AnalysisMode.DTW;

    /** Creates new form PsMoveGestureRecognizerTrainingGUI */
    public PsMoveGestureRecognizerTrainingGUI(String[] gestureNames, final boolean constantTrigger) throws SocketException, UnknownHostException, AWTException, Exception {

        initComponents();
        this.constantTrigger = constantTrigger;
        labelSaveLoadStatus.setText("");
        panelDebug.setVisible(false);
        this.gestureNames = new String[gestureNames.length];
        System.arraycopy(gestureNames, 0, this.gestureNames, 0, gestureNames.length);

        this.controlMode = ControlMode.MUSIC_TRIGGER;
        actionOptions = new String[1];
        actionOptions[0] = "MusicTrigger";
        sender = new OSCPortOut(InetAddress.getLocalHost(), sendPort);


        this.numClasses = gestureNames.length;
        this.analysisMode = AnalysisMode.DTW;

        addRows();

        robot = new Robot();
        
        analyzer = new PSMoveDTWGestureRecognizer(numFeatures, numClasses);
        panelSelectJoints.setVisible(false);
        repaint();
        
        attachAnalyzer();
    
    }
    
    private void attachAnalyzer() {
            analyzer.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pce) {
                if (pce.getPropertyName().equals(PSMoveDTWGestureRecognizer.PROP_MAXDISTANCE)) {
                    double newDist = analyzer.getMaxDistance();
                    if (newDist > analyzer.getMatchThreshold()) {
                        sliderThreshold.setMaximum((int) (100 * newDist) + 1);
                    } else {
                        sliderThreshold.setMaximum((int) (100 * analyzer.getMatchThreshold()) + 1);
                    }
                    System.out.println("Slider max set to " + sliderThreshold.getMaximum() * .01);

                } else if (pce.getPropertyName().equals(PSMoveDTWGestureRecognizer.PROP_CONTINUOUSMATCH)) {
                    int match = analyzer.getContinuousMatch();
                    updateStatusBars(match);
                    if (!constantTrigger) {
                        useClassificationResult(match);
                    }
                }
            }
        });

        analyzer.addClassificationListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                updateDistances(analyzer.getLastDistances());
                if (constantTrigger) {
                    useClassificationResult(analyzer.getContinuousMatch());
                }
            }
        });
    }

    private void addRows() {
        rows = new GestureRow[numClasses];
        GridLayout gl = (GridLayout) panelExamples.getLayout();
        gl.setRows(numClasses);

        //Remove demo panels
        panelExamples.remove(0);
        panelExamples.remove(0);
        panelExamples.remove(0);


        //Add other panels
        for (int i = 0; i < numClasses; i++) {
            String thisName;
            if (i < gestureNames.length) {
                thisName = gestureNames[i];
            } else {
                thisName = gestureNames[0];
            }

            int thisAction = 0;
            if (i < actionOptions.length) {
                thisAction = i;
            }

            rows[i] = addRow(thisName, i, thisAction);

        }

        setVisible(true);
    }

    private void updateStatusBars(int matchingClass) {
        for (int i = 0; i < numClasses; i++) {
            if (i == matchingClass) {
                rows[i].setActive(true);
            } else {
                rows[i].setActive(false);
            }
        }
    }

    //Called only initially on change (not repeatedly)
    private void useClassificationResult(int match) {

       // else if (controlMode == ControlMode.MUSIC_TRIGGER) {
            if (match != -1) {

                Object[] o = new Object[1];
                o[0] = match;

                OSCMessage msg = new OSCMessage("/MoveGesture", o);
                try {
                    sender.send(msg);
                } catch (IOException ex) {
                    Logger.getLogger(PsMoveGestureRecognizerTrainingGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        
    }

    private void updateDistances(double[] lastDistances) {

        double max = sliderThreshold.getMaximum() * .01;
        int min = 0;
        for (int i = 0; i < lastDistances.length; i++) {

            //distance is scale from 0 --> maxSlider*.01
            //mapped to 100 -> 0 (i.e., left is furthest)
            double thisDist = (1 - (lastDistances[i] / max)) * 100;
            if (thisDist < 0) {
                thisDist = 0;
            }

            rows[i].setStatus((int) thisDist);
        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel10 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        panelExamples = new javax.swing.JPanel();
        gestureRow1 = new psm.GestureRow();
        gestureRow2 = new psm.GestureRow();
        gestureRow3 = new psm.GestureRow();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        buttonStartStop = new javax.swing.JToggleButton();
        jPanel5 = new javax.swing.JPanel();
        sliderThreshold = new javax.swing.JSlider();
        panelSelectJoints = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        panelDebug = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        panelFileManagement = new javax.swing.JPanel();
        buttonSave = new javax.swing.JButton();
        buttonLoad = new javax.swing.JButton();
        labelSaveLoadStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Gesture Recognizer");
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));
        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Edit Example Movements", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Verdana", 0, 14))); // NOI18N
        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBounds(new java.awt.Rectangle(0, 0, 100, 20));
        jPanel1.setMaximumSize(new java.awt.Dimension(32767, 100));
        jPanel1.setMinimumSize(new java.awt.Dimension(100, 20));
        jPanel1.setPreferredSize(new java.awt.Dimension(485, 20));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel1.setText("On?");

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jLabel1)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel3.setText("Name");

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 44, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jLabel3)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setBackground(new java.awt.Color(255, 255, 255));
        jLabel4.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel4.setText("Add/Remove");

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel5.setText("Action");

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 76, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(jLabel5))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));

        jLabel6.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel6.setText("Degree of Match");

        org.jdesktop.layout.GroupLayout jPanel8Layout = new org.jdesktop.layout.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 109, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .add(jLabel6)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 254, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jPanel10.add(jPanel1);

        panelExamples.setBackground(new java.awt.Color(255, 255, 255));
        panelExamples.setLayout(new java.awt.GridLayout(3, 0));
        panelExamples.add(gestureRow1);
        panelExamples.add(gestureRow2);
        panelExamples.add(gestureRow3);

        jPanel10.add(panelExamples);

        getContentPane().add(jPanel10);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Run", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Verdana", 0, 14))); // NOI18N
        jPanel2.setMaximumSize(new java.awt.Dimension(33064, 85));
        jPanel2.setMinimumSize(new java.awt.Dimension(297, 85));
        jPanel2.setPreferredSize(new java.awt.Dimension(674, 85));
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setMaximumSize(new java.awt.Dimension(400, 32767));
        jPanel4.setMinimumSize(new java.awt.Dimension(325, 40));
        jPanel4.setPreferredSize(new java.awt.Dimension(325, 40));
        jPanel4.setRequestFocusEnabled(false);

        jLabel2.setFont(new java.awt.Font("Verdana", 0, 14)); // NOI18N
        jLabel2.setText("Match Threshold:");

        buttonStartStop.setText("Start");
        buttonStartStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartStopActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .add(buttonStartStop, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 55, Short.MAX_VALUE)
                .add(jLabel2)
                .add(8, 8, 8))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(jLabel2)
                .add(buttonStartStop, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 53, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        jPanel2.add(jPanel4);

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));

        sliderThreshold.setBackground(new java.awt.Color(255, 255, 255));
        sliderThreshold.setInverted(true);
        sliderThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderThresholdStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(sliderThreshold, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(sliderThreshold, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel5);

        getContentPane().add(jPanel2);

        panelSelectJoints.setBackground(new java.awt.Color(255, 255, 255));
        panelSelectJoints.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Select Active Joints", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Verdana", 0, 14))); // NOI18N

        jCheckBox1.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Joint1");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox3.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox3.setSelected(true);
        jCheckBox3.setText("Joint2");
        jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox3ActionPerformed(evt);
            }
        });

        jCheckBox2.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox2.setSelected(true);
        jCheckBox2.setText("Joint3");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout panelSelectJointsLayout = new org.jdesktop.layout.GroupLayout(panelSelectJoints);
        panelSelectJoints.setLayout(panelSelectJointsLayout);
        panelSelectJointsLayout.setHorizontalGroup(
            panelSelectJointsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelSelectJointsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jCheckBox1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jCheckBox3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jCheckBox2)
                .addContainerGap(473, Short.MAX_VALUE))
        );
        panelSelectJointsLayout.setVerticalGroup(
            panelSelectJointsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelSelectJointsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(jCheckBox1)
                .add(jCheckBox3)
                .add(jCheckBox2))
        );

        getContentPane().add(panelSelectJoints);

        panelDebug.setBackground(new java.awt.Color(255, 255, 255));
        panelDebug.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Debug", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Verdana", 0, 14))); // NOI18N

        jButton1.setText("printDebug");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jTextField1.setText("10.000");

        jButton2.setText("setNewThreshold");

        org.jdesktop.layout.GroupLayout panelDebugLayout = new org.jdesktop.layout.GroupLayout(panelDebug);
        panelDebug.setLayout(panelDebugLayout);
        panelDebugLayout.setHorizontalGroup(
            panelDebugLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelDebugLayout.createSequentialGroup()
                .add(jButton1)
                .add(38, 38, 38)
                .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton2)
                .addContainerGap(296, Short.MAX_VALUE))
        );
        panelDebugLayout.setVerticalGroup(
            panelDebugLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelDebugLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelDebugLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton1)
                    .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton2))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        getContentPane().add(panelDebug);

        panelFileManagement.setBackground(new java.awt.Color(255, 255, 255));
        panelFileManagement.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Save/Load Gesture Recognizer", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Verdana", 0, 14))); // NOI18N

        buttonSave.setText("Save to file...");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        buttonLoad.setText("Load from file...");
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        labelSaveLoadStatus.setText("jLabel7");

        org.jdesktop.layout.GroupLayout panelFileManagementLayout = new org.jdesktop.layout.GroupLayout(panelFileManagement);
        panelFileManagement.setLayout(panelFileManagementLayout);
        panelFileManagementLayout.setHorizontalGroup(
            panelFileManagementLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelFileManagementLayout.createSequentialGroup()
                .add(buttonSave)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(buttonLoad)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(labelSaveLoadStatus, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 335, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(82, Short.MAX_VALUE))
        );
        panelFileManagementLayout.setVerticalGroup(
            panelFileManagementLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelFileManagementLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelFileManagementLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(buttonSave)
                    .add(buttonLoad)
                    .add(labelSaveLoadStatus))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        getContentPane().add(panelFileManagement);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void activateStopButton(boolean a) {
        if (a) {
            buttonStartStop.setText("Stop");
            analyzer.startRunningContinuous();
            if (!buttonStartStop.isSelected()) {
                buttonStartStop.setSelected(true);
            }
        } else {
            buttonStartStop.setText("Start");
            analyzer.stopRunning();
            if (buttonStartStop.isSelected()) {
                buttonStartStop.setSelected(false);
            }
        }
    }

    private void buttonStartStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartStopActionPerformed
        activateStopButton(buttonStartStop.isSelected());
    }//GEN-LAST:event_buttonStartStopActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        analyzer.printState();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void sliderThresholdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderThresholdStateChanged
        if (!sliderThreshold.getValueIsAdjusting()) {
            double t = sliderThreshold.getValue() * .01;
            System.out.println("Setting threshold from slider");
            analyzer.setMatchThreshold(t);
        }
    }//GEN-LAST:event_sliderThresholdStateChanged

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        updateFeatures();
}//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
        updateFeatures();
}//GEN-LAST:event_jCheckBox3ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        updateFeatures();
}//GEN-LAST:event_jCheckBox2ActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        // TODO add your handling code here:
        JFrame frame = new JFrame();
        //frame.setPreferredSize(new Dimension(478, 532));
        frame.setSize(new Dimension(478, 532));
        FileChooserWithExtension fc = new FileChooserWithExtension("txt", "Gesture file", new File("gestures.txt"), new File("./"), true);
        int returnVal = fc.showSaveDialog(this);
        File file = null;
        if (returnVal == FileChooserWithExtension.APPROVE_OPTION) {
            file = fc.getSelectedFile();
           // fc.getCu
        }
        if (file != null)  {
            try {
                analyzer.writeToFile(file);
                labelSaveLoadStatus.setText("Wrote to file " + file.getName());
            } catch (IOException ex) {
                System.out.println("Problem encountered writing to file");
                Logger.getLogger(PsMoveGestureRecognizerTrainingGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        
                
        }
        
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed

        JFrame frame = new JFrame();
        //frame.setPreferredSize(new Dimension(478, 532));
        frame.setSize(new Dimension(478, 532));
        FileChooserWithExtension fc = new FileChooserWithExtension("txt", "Gesture Set", null, new File("./"), false);
        int returnVal = fc.showOpenDialog(frame);

        File file = null;
        if (returnVal == FileChooserWithExtension.APPROVE_OPTION) {
            file = fc.getSelectedFile();
          //  PSMoveDTWGestureRecognizer gr = null;

            try {
                analyzer.loadFromFile(file);
                for (int i = 0; i < analyzer.numClasses; i++) {
                    rows[i].setNumExamples(analyzer.getNumExamples(i));
                }
                labelSaveLoadStatus.setText("Read successfully from file");
                
            } catch (Exception ex) {
                System.out.println("Error reading from file:");
                ex.printStackTrace();
                labelSaveLoadStatus.setText("Error reading from file");
            }
          //  if (gr != null) {
                updateGUIForLoadedGestureRecognizer();
          //  }
                
        }
    }//GEN-LAST:event_buttonLoadActionPerformed

    private void updateFeatures() {
     /*   if (analysisMode == analysisMode.TARGET) {
            boolean enabled[] = new boolean[3];
            enabled[0] = jCheckBox1.isSelected();
            enabled[1] = jCheckBox2.isSelected();
            enabled[2] = jCheckBox3.isSelected();
            ((Targeter) analyzer).setEnabledFeatures(enabled);
        } */
    }

    /**
     * To run from command line: Default is target acquisition + game control;
     * add "dtw" flag to run with DTW + ChucK control
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        final boolean doDtw;
        //if (args.length > 0 && args[0].equals("dtw")) {
            doDtw = true;
       /* } else {
            doDtw = false;
        } */

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                    Logger.getLogger(PsMoveGestureRecognizerTrainingGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
                PsMoveGestureRecognizerTrainingGUI gui;
                String[] names = {"Gesture1", "Gesture2", "Gesture3", "Gesture4"};
                try {
                    //if (doDtw) {
                        gui = new PsMoveGestureRecognizerTrainingGUI(names, true);
                    //} else {

                        //gui = new PsMoveGestureRecognizerTrainingGUI(names, ControlMode.DIRECTION_PAD, AnalysisMode.TARGET, true);

                    //}
                } catch (Exception ex) {
                    System.out.println("Error encountered in running GUI: ");
                    ex.printStackTrace();
                    System.exit(0);
                }

            }
        });
    }

    private GestureRow addRow(String name, final int rowNum, int selectedAction) {
        String[] s1 = actionOptions;
        final GestureRow r = new GestureRow(name, 0, s1);
        r.setActive(false);
        r.selectAction(selectedAction);
        panelExamples.add(r, rowNum);

        r.addAddButtonMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                if (analyzer.getRunningState() != PSMoveDTWGestureRecognizer.RunningState.NOT_RUNNING) {
                    activateStopButton(false);

                    analyzer.stopRunning();
                }
                analyzer.setCurrentTrainingLabel(rowNum);
                analyzer.startRecording();
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                analyzer.stopRecording();
                int n = analyzer.getNumExamples(rowNum);
                rows[rowNum].setNumExamples(n);
            }
        });
        r.addRemoveButtonMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent me) {
                if (analyzer.getRunningState() != PSMoveDTWGestureRecognizer.RunningState.NOT_RUNNING) {
                    activateStopButton(false);
                    analyzer.stopRunning();
                }
                analyzer.deleteLast(rowNum);
                int n = analyzer.getNumExamples(rowNum);
                rows[rowNum].setNumExamples(n);
            }
        });

        r.addActionChoiceListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {

            }
        });

        r.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent pce) {
                if (pce.getPropertyName().equals(GestureRow.PROP_ISGESTUREENABLED)) {
                    activeChange(rowNum, r.isGestureEnabled());
                }
            }
        });
        return r;

    }

    private void activeChange(int rowNum, boolean active) {
        analyzer.setIsClassActive(active, rowNum);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonSave;
    private javax.swing.JToggleButton buttonStartStop;
    private psm.GestureRow gestureRow1;
    private psm.GestureRow gestureRow2;
    private psm.GestureRow gestureRow3;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel labelSaveLoadStatus;
    private javax.swing.JPanel panelDebug;
    private javax.swing.JPanel panelExamples;
    private javax.swing.JPanel panelFileManagement;
    private javax.swing.JPanel panelSelectJoints;
    private javax.swing.JSlider sliderThreshold;
    // End of variables declaration//GEN-END:variables
}
