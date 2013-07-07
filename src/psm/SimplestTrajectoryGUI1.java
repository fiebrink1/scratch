/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SimplestDTWGUI.java
 *
 * Created on Aug 11, 2011, 1:58:45 PM
 */
package psm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fiebrink
 */
public class SimplestTrajectoryGUI1 extends javax.swing.JFrame {

    SimplestTrajectory trajector;
    double myThreshold = 25.0;

    /** Creates new form SimplestDTWGUI */
    public SimplestTrajectoryGUI1() {
        initComponents();
        try {
            trajector = new SimplestTrajectory();
            trajector.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent pce) {
                    if (pce.getPropertyName().equals(SimplestTrajectory.PROP_MAXDISTANCE)) {
                        double newDist = trajector.getMaxDistance();
                        if (newDist > myThreshold) {
                            sliderThreshold.setMaximum((int) (100 * newDist) + 1);
                        } else {
                            sliderThreshold.setMaximum((int) (100 * myThreshold) + 1);
                        }
                        System.out.println("Slider max set to " + sliderThreshold.getMaximum());

                    } else if (pce.getPropertyName().equals(SimplestTrajectory.PROP_CONTINUOUSMATCH)) {
                        int match = trajector.getContinuousMatch();
                        if (match == -1) {
                            labelContinuousOutput.setText("NONE");

                        } else {
                            labelContinuousOutput.setText("MATCH");
                            labelDistance.setText("" + trajector.getCurrentDistance());
                        }
                    } else if (pce.getPropertyName().equals(SimplestTrajectory.PROP_CURRENTDISTANCE)) {
                        labelDistance.setText("Distance: " + trajector.getCurrentDistance());
                    }

                }
            });



        } catch (Exception ex) {
            System.out.println("Exception encountered: " + ex);
            Logger.getLogger(SimplestTrajectoryGUI1.class.getName()).log(Level.SEVERE, null, ex);
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

        jPanel1 = new javax.swing.JPanel();
        buttonAdd1 = new javax.swing.JButton();
        buttonDelete1 = new javax.swing.JButton();
        labelNum1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        toggleRunContinuously = new javax.swing.JToggleButton();
        labelContinuousOutput = new javax.swing.JLabel();
        labelDistance = new javax.swing.JLabel();
        labelMatchThreshold = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        sliderThreshold = new javax.swing.JSlider();
        jButton1 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        labelT1 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Add Example Targets"));

        buttonAdd1.setText("Add Target 1");
        buttonAdd1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                buttonAdd1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonAdd1MouseReleased(evt);
            }
        });
        buttonAdd1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                buttonAdd1StateChanged(evt);
            }
        });
        buttonAdd1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAdd1ActionPerformed(evt);
            }
        });

        buttonDelete1.setText("Delete Target 1");
        buttonDelete1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDelete1ActionPerformed(evt);
            }
        });

        labelNum1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelNum1.setText("0");

        jLabel2.setText("# recorded");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(buttonAdd1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(buttonDelete1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labelNum1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                        .add(313, 313, 313)
                        .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(buttonDelete1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 47, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(buttonAdd1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 47, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labelNum1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Run Gesture Recognizer"));

        toggleRunContinuously.setText("Run Continuously");
        toggleRunContinuously.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleRunContinuouslyActionPerformed(evt);
            }
        });

        labelContinuousOutput.setFont(new java.awt.Font("Lucida Grande", 0, 36)); // NOI18N
        labelContinuousOutput.setText("none");

        labelDistance.setText("Distance to 1: 0.3523549");

        labelMatchThreshold.setText("Match threshold: 10");

        sliderThreshold.setMaximum(5000);
        sliderThreshold.setValue(1000);
        sliderThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderThresholdStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(sliderThreshold, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(sliderThreshold, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(8, 8, 8)
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(labelMatchThreshold, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 181, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(toggleRunContinuously, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 131, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labelContinuousOutput)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labelDistance, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 257, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(labelMatchThreshold)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(toggleRunContinuously, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(26, 26, 26)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(labelContinuousOutput)
                            .add(labelDistance, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
        );

        jButton1.setText("print debug info");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Joint1");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox2.setSelected(true);
        jCheckBox2.setText("Joint3");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jCheckBox3.setSelected(true);
        jCheckBox3.setText("Joint2");
        jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox3ActionPerformed(evt);
            }
        });

        labelT1.setText(".001");

        jButton3.setText("OK");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(28, 28, 28)
                        .add(jButton1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jCheckBox1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jCheckBox3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jCheckBox2)
                        .add(18, 18, 18)
                        .add(labelT1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 54, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton3))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 70, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton1)
                    .add(jCheckBox1)
                    .add(jCheckBox3)
                    .add(jCheckBox2)
                    .add(labelT1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton3))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonAdd1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAdd1ActionPerformed
    }//GEN-LAST:event_buttonAdd1ActionPerformed

    private void buttonDelete1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDelete1ActionPerformed
        trajector.deleteExamples();
        labelNum1.setText("0");
    }//GEN-LAST:event_buttonDelete1ActionPerformed

    private void buttonAdd1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonAdd1MousePressed
        trajector.setCurrentTrainingGesture(0);
        trajector.startRecording();
    }//GEN-LAST:event_buttonAdd1MousePressed

    private void buttonAdd1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_buttonAdd1StateChanged
    }//GEN-LAST:event_buttonAdd1StateChanged

    private void buttonAdd1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonAdd1MouseReleased
        trajector.stopRecording();
        int n = trajector.getNumExamples();
        labelNum1.setText(Integer.toString(n));
    }//GEN-LAST:event_buttonAdd1MouseReleased

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        trajector.printState();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void toggleRunContinuouslyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleRunContinuouslyActionPerformed
        if (toggleRunContinuously.isSelected()) {
            trajector.startRunning();
        } else {
            trajector.stopRunning();
        }

        System.out.println(toggleRunContinuously.isSelected());
    }//GEN-LAST:event_toggleRunContinuouslyActionPerformed

    private void sliderThresholdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderThresholdStateChanged
        double t = sliderThreshold.getValue() * .01;
        trajector.setMatchThreshold(t);

        labelMatchThreshold.setText("Match threshold: " + t);
        myThreshold = sliderThreshold.getValue() * .01;
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

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        double t = Double.parseDouble(labelT1.getText());
        trajector.setMatchThreshold(t);

        labelMatchThreshold.setText("Match threshold: " + t);
}//GEN-LAST:event_jButton3ActionPerformed

    private void updateFeatures() {
        boolean enabled[] = new boolean[3];
        enabled[0] = jCheckBox1.isSelected();
        enabled[1] = jCheckBox2.isSelected();
        enabled[2] = jCheckBox3.isSelected();
        trajector.setEnabledFeatures(enabled);

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new SimplestTrajectoryGUI1().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAdd1;
    private javax.swing.JButton buttonDelete1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JLabel labelContinuousOutput;
    private javax.swing.JLabel labelDistance;
    private javax.swing.JLabel labelMatchThreshold;
    private javax.swing.JLabel labelNum1;
    private javax.swing.JTextField labelT1;
    private javax.swing.JSlider sliderThreshold;
    private javax.swing.JToggleButton toggleRunContinuously;
    // End of variables declaration//GEN-END:variables
}