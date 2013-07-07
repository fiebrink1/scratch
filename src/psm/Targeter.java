/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package psm;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import java.beans.PropertyChangeSupport;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author fiebrink
 */
public class Targeter implements GestureRecognizer {

    int numClasses = 2;
    public int receivePort = 6448;
    public OSCPortIn receiver;
    int numFeatures = 3; //TODO: Change to 3
    ArrayList<LinkedList<Position>> allPositions;
    int currentTrainingLabel = 0;
    double[] distanceToClasses;
    double closestDist = Double.MAX_VALUE;
    double matchThreshold = 3.0;
    protected RecordingState recordingState = RecordingState.NOT_RECORDING;
    protected RunningState runningState = RunningState.NOT_RUNNING;
    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    protected double maxDistance;
    protected int continuousMatch;
    protected boolean[] enabledFeatures = {true, true, true};
    boolean isClassActive[];
    protected EventListenerList continuousClassificationListenerList = new EventListenerList();
    protected EventListenerList exampleAddedListenerList = new EventListenerList();

    private ChangeEvent changeEvent = null;

    private ChangeEvent trainingAddedChangeEvent = null;
    

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
    protected void setMaxDistance(double maxDistance) {
        //  System.out.println("Max distance set to " + maxDistance);
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

    public Targeter(int numClasses) throws SocketException, UnknownHostException, Exception {
        this.numClasses = numClasses;
        isClassActive = new boolean[numClasses];
        for (int i = 0; i < isClassActive.length; i++) {
            isClassActive[i] = true;
        }
        addOscListeners();
        allPositions = new ArrayList<LinkedList<Position>>(numClasses);

        distanceToClasses = new double[numClasses];
        for (int i = 0; i < numClasses; i++) {
            LinkedList<Position> l = new LinkedList<Position>();
            allPositions.add(l);
            distanceToClasses[i] = Double.MAX_VALUE;
        }
    }

    public void setCurrentTrainingLabel(int i) {
        currentTrainingLabel = i;
    }

    public int getCurrentTrainingLabel() {
        return currentTrainingLabel;
    }

    public void deleteExamples(int whichClass) {
        LinkedList<Position> listToClear = allPositions.get(whichClass);
        listToClear.clear();
    }

    public void addClassificationListener(ChangeListener l) {
        continuousClassificationListenerList.add(ChangeListener.class, l);
    }

    public void removeClassificationListener(ChangeListener l) {
        continuousClassificationListenerList.remove(ChangeListener.class, l);
    }

    public void addTrainingAddedListener(ChangeListener l) {
        exampleAddedListenerList.add(ChangeListener.class, l);
    }

    public void removeTrainingAddedListener(ChangeListener l) {
        exampleAddedListenerList.remove(ChangeListener.class, l);
    }

    public void deleteLast(int classNum) {
        LinkedList<Position> ts = allPositions.get(classNum);
        if (ts.size() > 0) {
            ts.removeLast();
        }
    }

    public boolean getIsClassActive(int c) {
        return isClassActive[c];
    }

    public void setIsClassActive(boolean isActive, int c) {
        this.isClassActive[c] = isActive;
    }

    public double[] getLastDistances() {
        return distanceToClasses;
    }

    public RecordingState getRecordingState() {
        return recordingState;
    }

    public RunningState getRunningState() {
        return runningState;

    }

    public void startRunningContinuous() {
        if (runningState != RunningState.RUNNING_CONTINUOUS) {
            //Note: For now, this should be fine even if classifier is untrained / no data.
            setRunningState(RunningState.RUNNING_CONTINUOUS);
        }

    }

    protected void updateMaxDistance() {
        //Update threshold info
        double maxDist = 0.0;
        int numClassesWithExamples = 0;
        int whichClass = -1;

        for (int i = 0; i < allPositions.size(); i++) {
            //For now, include inactive classes here.
            LinkedList<Position> list1 = allPositions.get(i);
            if (list1.size() > 0) {
                numClassesWithExamples++;
                whichClass = i;
            }

            for (int j = i + 1; j < allPositions.size(); j++) {
                LinkedList<Position> list2 = allPositions.get(j);

                //Find max distance between list1 and list2
                for (Position p1 : list1) {
                    for (Position p2 : list2) {
                        double ss = p1.sqrDistance(p2);
                        if (ss > maxDist) {
                            maxDist = ss;
                        }
                    }
                }
            }

            //Buffer we want in case classes are close
                maxDist = maxDist * 10;
        }

        if (numClassesWithExamples == 1) {
            LinkedList<Position> list = allPositions.get(whichClass);
            for (int i = 0; i < list.size(); i++) {
                Position p1 = list.get(i);
                for (int j = i+1; j < list.size(); j++) {
                    Position p2 = list.get(j);
                    double ss = p1.sqrDistance(p2);
                    if (ss > maxDist) {
                        maxDist = ss; //now maxDist is max intra-class Dist
                    }
                }
            }
            maxDist = maxDist * 20; // just in case.
        }

        setMaxDistance(Math.sqrt(maxDist));
    }

    protected void fireNewClassificationResult() {
        Object[] listeners = continuousClassificationListenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    protected void fireNewExampleAdded() {
        Object[] listeners = exampleAddedListenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (trainingAddedChangeEvent == null) {
                    trainingAddedChangeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(trainingAddedChangeEvent);
            }
        }
    }

    public void stopRunning() {
        if (runningState == RunningState.RUNNING_CONTINUOUS) {
            setRunningState(RunningState.NOT_RUNNING);
        }
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
                } else if (getRunningState() == RunningState.RUNNING_CONTINUOUS) {

                    analyze(d);
                }

            }
        };
        receiver.addListener("/oscCustomFeatures", listener);
    }

    protected void addTrainingVector(double[] d) {
        System.out.print("Added vector : ");
        for (int i = 0; i < d.length; i++) {
            System.out.print(d[i] + " ");
        }
        System.out.println("");
        allPositions.get(currentTrainingLabel).addLast(new Position(d));
        updateMaxDistance();
        fireNewExampleAdded();

    }

    public int getNumExamples(int classVal) {
        return allPositions.get(classVal).size();

    }

    protected void analyze(double[] d) {
        distanceToClasses = new double[numClasses];
        closestDist = Double.MAX_VALUE;
        int closestClass = -1;

        for (int i = 0; i < distanceToClasses.length; i++) {
            distanceToClasses[i] = Double.MAX_VALUE;
        }

        Position newp = new Position(d);

        for (int whichClass = 0; whichClass < numClasses; whichClass++) {
            if (isClassActive[whichClass]) {
                for (Position p : allPositions.get(whichClass)) {
                    double dist = p.sqrDistance(newp);
                    if (distanceToClasses[whichClass] > dist) {
                        distanceToClasses[whichClass] = dist;
                    }
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestClass = whichClass;
                    }
                }
            }
        }

        fireNewClassificationResult();
        System.out.println("Closest is " + closestDist + ", match th is " + matchThreshold);
        if (closestDist < matchThreshold) {
            System.out.println("MATCHES " + closestClass);
            setContinuousMatch(closestClass);
        } else {
            System.out.println("NO Match");
            setContinuousMatch(-1);
        }
    }

    public double getLastDistance(int gestureClass) {
        return distanceToClasses[gestureClass];
    }

    public void printState() {
        System.out.println("Target: " + numClasses + " classes");
        System.out.println("Closest dist: " + closestDist);
        for (int i = 0; i < numClasses; i++) {
            System.out.println("CLASS " + i + ": + "
                    + allPositions.get(i).size()
                    + "points, closest dist =" + distanceToClasses[i] + ")");
            for (Position ts : allPositions.get(i)) {
                System.out.println(ts);
            }
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
            String s = "{";
            for (int i = 0; i < coords.length; i++) {
                s += " " + coords[i] + " ";
            }
            s += "}";
            return s;
        }
    }
}
