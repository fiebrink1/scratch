/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package psm;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import java.beans.PropertyChangeSupport;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.beans.PropertyChangeListener;

/**
 *
 * @author fiebrink
 */
public class SimplestTrajectory {

    public int receivePort = 6448;
    public int sendPort = 6453;
    OSCPortOut sender;
    public OSCPortIn receiver;
    int numFeatures = 3; //TODO: Change to 3
    //protected transient EventListenerList classificationListenerList = new EventListenerList(); //listening for new classification result notification
    //protected transient EventListenerList trainingListenerList = new EventListenerList();
    LinkedList<Position> p1s = new LinkedList<Position>();
    int currentTrainingGesture = 0;
    double currentDistance = Double.MAX_VALUE;
    double matchThreshold = 3.0;
    protected RecordingState recordingState = RecordingState.NOT_RECORDING;
    protected RunningState runningState = RunningState.NOT_RUNNING;
    public static String PROP_RECORDINGSTATE = "recordingState";
    public static String PROP_RUNNINGSTATE = "runningState";
    public static final String PROP_MAXTHRESHOLD = "maxThreshold";
    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    protected double maxDistance;
    public static final String PROP_MAXDISTANCE = "maxDistance";
    protected int continuousMatch;
    public static final String PROP_CONTINUOUSMATCH = "continuousMatch";
    public static final String PROP_CURRENTDISTANCE = "currentDistance";
    protected boolean[] enabledFeatures = {true, true, true};


    /**
     * Get the value of enabledFeatures
     *
     * @return the value of enabledFeatures
     */
    public boolean[] getEnabledFeatures() {
        return enabledFeatures;
    }

    /**
     * Set the value of enabledFeatures
     *
     * @param enabledFeatures new value of enabledFeatures
     */
    public void setEnabledFeatures(boolean[] enabledFeatures) {
        this.enabledFeatures = enabledFeatures;
        updateMaxDistance();
    }

    /**
     * Get the value of enabledFeatures at specified index
     *
     * @param index
     * @return the value of enabledFeatures at specified index
     */
    public boolean getEnabledFeatures(int index) {
        return this.enabledFeatures[index];
    }

    /**
     * Set the value of enabledFeatures at specified index.
     *
     * @param index
     * @param newEnabledFeatures new value of enabledFeatures at specified index
     */
    public void setEnabledFeatures(int index, boolean newEnabledFeatures) {
        this.enabledFeatures[index] = newEnabledFeatures;
updateMaxDistance();
    }

    /**
     * Get the value of continuousMatch
     *
     * @return the value of continuousMatch
     */
    public int getContinuousMatch() {
        return continuousMatch;
    }

    /**
     * Set the value of continuousMatch
     *
     * @param continuousMatch new value of continuousMatch
     */
    public void setContinuousMatch(int continuousMatch) {
        int oldContinuousMatch = this.continuousMatch;
        this.continuousMatch = continuousMatch;
        propertyChangeSupport.firePropertyChange(PROP_CONTINUOUSMATCH, oldContinuousMatch, continuousMatch);
    }



    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Get the value of maxDistance
     *
     * @return the value of maxDistance
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Set the value of maxDistance
     *
     * @param maxDistance new value of maxDistance
     */
    public void setMaxDistance(double maxDistance) {
        System.out.println("Max distance set to " + maxDistance);
        double oldMaxDistance = this.maxDistance;
        this.maxDistance = maxDistance;
        propertyChangeSupport.firePropertyChange(PROP_MAXDISTANCE, oldMaxDistance, maxDistance);
    }


        /**
     * Get the value of maxDistance
     *
     * @return the value of maxDistance
     */
    public double getMatchThreshold() {
        return matchThreshold;
    }

    /**
     * Set the value of maxDistance
     *
     * @param maxDistance new value of maxDistance
     */
    public void setMatchThreshold(double matchThreshold) {
        double oldMatchThreshold = this.matchThreshold;
        this.matchThreshold = matchThreshold;
        propertyChangeSupport.firePropertyChange(PROP_MAXTHRESHOLD, oldMatchThreshold, matchThreshold);
    }
    public SimplestTrajectory() throws SocketException, UnknownHostException, Exception {
        addOscListeners();

    }

    public void setCurrentTrainingGesture(int i) {
        currentTrainingGesture = i;
    }

    public int getCurrentTrainingGesture() {
        return currentTrainingGesture;
    }

    public void deleteExamples() {
            p1s.clear();

    }

    //Whether Wekinator should add incoming feature vectors to the training set
    public enum RecordingState {

        RECORDING,
        NOT_RECORDING
    };

    //Whether Wekinator should be classifying incoming feature vectors
    public enum RunningState {

        RUNNING,
        NOT_RUNNING
    };


    public void startRunning() {
        if (runningState != RunningState.RUNNING) {
            //Note: For now, this should be fine even if classifier is untrained / no data.
            setRunningState(RunningState.RUNNING);
        }

    }

    protected void updateMaxDistance() {
                    //Update threshold info
            double maxSS = 0.0;
            for (int i = 0; i < p1s.size(); i++) {
                for (int j = i; j < p1s.size(); j++) {
                    Position p1 = p1s.get(i);
                    Position p2 = p1s.get(j);
                    double ss = p1.sqrDistance(p2);
                    if (ss > maxSS) {
                         maxSS = ss;
                     }
                }
            }

            setMaxDistance(Math.sqrt(maxSS));
    }

    public void stopRunning() {
        if (runningState == RunningState.RUNNING) {
            setRunningState(RunningState.NOT_RUNNING);
        }
    }

    public RecordingState getRecordingState() {
        return recordingState;
    }

    public void startRecording() {
        if (recordingState == RecordingState.NOT_RECORDING) {
            setRecordingState(RecordingState.RECORDING);
        }
    }

    public void stopRecording() {
        if (recordingState == RecordingState.RECORDING) {
            setRecordingState(RecordingState.NOT_RECORDING);
        }
    }

    protected void setRecordingState(RecordingState recordingState) {
        RecordingState oldState = this.recordingState;
        this.recordingState = recordingState;
        propertyChangeSupport.firePropertyChange(PROP_RECORDINGSTATE, oldState, recordingState);
    }

    public RunningState getRunningState() {
        return runningState;
    }

    protected void setRunningState(RunningState runningState) {
        RunningState oldState = this.runningState;
        this.runningState = runningState;
        propertyChangeSupport.firePropertyChange(PROP_RUNNINGSTATE, oldState, runningState);
    }

    private void addOscListeners() throws SocketException, UnknownHostException, Exception {
        try {
            receiver = new OSCPortIn(receivePort);
        } catch (Exception ex) {
            System.out.println("Could not bind to port " + receivePort + ". Please quit all other instances of Wekinator or change the receive port.");
            throw ex;
        }
        sender = new OSCPortOut(InetAddress.getLocalHost(), sendPort);
        addOscFeatureListener();
        receiver.startListening();
        System.out.println("Listening...");
    }

    private void addOscFeatureListener() {
        OSCListener listener = new OSCListener() {

            public void acceptMessage(java.util.Date time, OSCMessage message) {
                // System.out.println("received OSC");
                Object[] o = message.getArguments();
                double d[] = new double[o.length];
                for (int i = 0; i < o.length; i++) {
                    if (o[i] instanceof Float) {
                        d[i] = ((Float) o[i]).floatValue();
                    } else {
                        System.out.println("Warning: Received feature value is not a float");
                    }
                }
                // Use this feature vector!
                if (getRecordingState() == RecordingState.RECORDING) {
                    addTrainingVector(d); //calls newTrainingExampleRecorded
                    //newTrainingExampleRecorded(id);
                } else if (getRunningState() == RunningState.RUNNING) {
                
                    analyze(d);
                }

            }
        };
        receiver.addListener("/oscCustomFeatures", listener);
    }

    protected void addTrainingVector(double[] d) {
        System.out.print("Added vector : ");
        for (int i = 0 ; i < d.length; i++) {
            System.out.print(d[i] + " ");
        }
        System.out.println("");
            p1s.add(new Position(d));

       updateMaxDistance();
    
    }

    

    public int getNumExamples() {
            return p1s.size();
       
    }

    protected void analyze(double[] d) {
        double sdistanceTo1 = Double.MAX_VALUE;

        Position newp = new Position(d);
            for (Position p : p1s) {
                double dist = p.sqrDistance(newp);
                if (sdistanceTo1 > dist) {
                    sdistanceTo1 = dist;
                }
            }

            setCurrentDistance(Math.sqrt(sdistanceTo1));

        double closestDist = getCurrentDistance();
        
        

        System.out.println("Closest is " + closestDist + ", match th is " + matchThreshold);
        if (closestDist < matchThreshold) {
            System.out.println("MATCHES");
            setContinuousMatch(1);
        } else {
            System.out.println("NO Match");
            setContinuousMatch(-1);
        }
        //TODO: Callbacks -- for state update in GUI

        //TODO: sqrt distance

    }


    public double getCurrentDistance() {
            return currentDistance;
    }

        /**
     *
     */
    public void setCurrentDistance(double currentDistance) {
        double oldDistance = this.currentDistance;
        this.currentDistance = currentDistance;
        propertyChangeSupport.firePropertyChange(PROP_CURRENTDISTANCE, oldDistance, currentDistance);
    }


    public void printState() {
        System.out.println("Positions:");
        for (Position p : p1s) {
            System.out.println(p);
        }
    }

    class Position {
        public int dimension;
        public double coords[];

        public Position(int dimension) {
            this.dimension = dimension;
            coords = new double[dimension];
        }

        public Position(double[] c) {
            this.dimension = c.length;
            coords = new double[c.length];
            System.arraycopy(c, 0, coords, 0, c.length);
        }

        public double sqrDistance(Position p2) {
            double ss = 0.0;
            for (int i = 0; i < coords.length; i++) {
                if (enabledFeatures[i]) {
                    ss += Math.pow((coords[i] - p2.coords[i]), 2.0);
                }
            }
            return ss;
        }

        @Override
        public String toString() {
            String s= "{";
            for (int i = 0; i < coords.length; i++) {
                s += " " + coords[i] + " ";
            }
            s += "}";
            return s;
        }

    }

}
