/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package psm;

import com.dtw.TimeWarpInfo;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import java.beans.PropertyChangeSupport;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import com.timeseries.TimeSeries;
import com.timeseries.TimeSeriesPoint;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 *
 * @author fiebrink
 */
public class SimplestDTW {

    public int receivePort = 6448;
    public int sendPort = 6453;
    OSCPortOut sender;
    public OSCPortIn receiver;
    int numFeatures = 3; //TODO: Change to 3
    //protected transient EventListenerList classificationListenerList = new EventListenerList(); //listening for new classification result notification
    //protected transient EventListenerList trainingListenerList = new EventListenerList();
    LinkedList<TimeSeries> ts1s = new LinkedList<TimeSeries>();
    LinkedList<TimeSeries> ts2s = new LinkedList<TimeSeries>();
    TimeSeries currentTs = new TimeSeries(numFeatures);
    int currentTime = 0;
    int currentTrainingGesture = 0;
    double distanceTo1 = Double.MAX_VALUE;
    double distanceTo2 = Double.MAX_VALUE;
    int minGestureSize = 5;
    int minSizeInExamples = 10;
    int maxSizeInExamples = 10;
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
    public SimplestDTW() throws SocketException, UnknownHostException, Exception {
        addOscListeners();

    }

    public void setCurrentTrainingGesture(int i) {
        currentTrainingGesture = i;
    }

    public int getCurrentTrainingGesture() {
        return currentTrainingGesture;
    }

    public void deleteExamples(int whichClass) {
        if (whichClass == 0) {
            ts1s.clear();
        } else {
            ts2s.clear();
        }
    }

    //Whether Wekinator should add incoming feature vectors to the training set
    public enum RecordingState {

        RECORDING,
        NOT_RECORDING
    };

    //Whether Wekinator should be classifying incoming feature vectors
    public enum RunningState {

        RUNNING_SINGLE,
        RUNNING_CONTINUOUS,
        NOT_RUNNING
    };

    public void startRunningSingle() {
        if (runningState != RunningState.RUNNING_SINGLE) {
            //Note: For now, this should be fine even if classifier is untrained / no data.
            setRunningState(RunningState.RUNNING_SINGLE);
            currentTime = 0;
            currentTs = new TimeSeries(numFeatures);
        }
    }

    public void startRunningContinuous() {
        if (runningState != RunningState.RUNNING_CONTINUOUS) {
            //Note: For now, this should be fine even if classifier is untrained / no data.
            setRunningState(RunningState.RUNNING_CONTINUOUS);
            currentTime = 0;
            currentTs = new TimeSeries(numFeatures);

            minSizeInExamples = minGestureSize;
            maxSizeInExamples = 0;
            for (TimeSeries ts : ts1s) {
                if (ts.size() < minSizeInExamples) {
                    minSizeInExamples = ts.size();
                }
                if (ts.size() > maxSizeInExamples) {
                    maxSizeInExamples = ts.size();
                }
            }
            for (TimeSeries ts : ts2s) {
                if (ts.size() < minSizeInExamples) {
                    minSizeInExamples = ts.size();
                }
                if (ts.size() > maxSizeInExamples) {
                    maxSizeInExamples = ts.size();
                }
            }


        }

    }

    protected void updateMaxDistance() {
                    //Update threshold info
            double maxDist = 0.0;
            for (TimeSeries ts1 : ts1s) {
                for (TimeSeries ts2 : ts2s) {
                     TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts1, ts2, 5);
                     if (info.getDistance() > maxDist) {
                         maxDist = info.getDistance();
                     }
                }
            }
            setMaxDistance(maxDist);

    }

    public void stopRunning() {
        if (runningState == RunningState.RUNNING_SINGLE || runningState == RunningState.RUNNING_CONTINUOUS) {
            setRunningState(RunningState.NOT_RUNNING);
        }
    }

    public RecordingState getRecordingState() {
        return recordingState;
    }

    public void startRecording() {
        if (recordingState == RecordingState.NOT_RECORDING) {
            setRecordingState(RecordingState.RECORDING);
            currentTime = 0;
            // currentTs.clear();
            currentTs = new TimeSeries(numFeatures);
        }
    }

    public void stopRecording() {
        if (recordingState == RecordingState.RECORDING) {
            setRecordingState(RecordingState.NOT_RECORDING);
            addTrainingExample();
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
                } else if (getRunningState() == RunningState.RUNNING_SINGLE) {
                    addClassificationVector(d);
                } else if (getRunningState() == RunningState.RUNNING_CONTINUOUS) {
                    addContinuousRunVector(d);
                }

            }
        };
        receiver.addListener("/oscCustomFeatures", listener);
    }

    protected void addTrainingVector(double[] d) {
        TimeSeriesPoint p = new TimeSeriesPoint(d);
        currentTs.addLast(currentTime, p);
        currentTime++;
    }

    protected void addClassificationVector(double[] d) {
        TimeSeriesPoint p = new TimeSeriesPoint(d);
        currentTs.addLast(currentTime, p);
        currentTime++;
    }

    protected void addContinuousRunVector(double[] d) {
        TimeSeriesPoint p = new TimeSeriesPoint(d);
        currentTs.addLast(currentTime, p);
        currentTime++;
        detectContinuous();
    }

    protected void addTrainingExample() {
        if (currentTs.size() == 0) {
            System.out.println("Error: Current training example is blank!");
            return;
        }
        if (currentTrainingGesture == 0) {
            ts1s.add(currentTs);
        } else {
            ts2s.add(currentTs);
        }
        //     trainingExampleAdded(currentTrainingGesture); //notify listeners
        //Update threshold info
            double maxDist = 0.0;
            for (TimeSeries ts1 : ts1s) {
                for (TimeSeries ts2 : ts2s) {
                     TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts1, ts2, 5);
                     if (info.getDistance() > maxDist) {
                         maxDist = info.getDistance();
                     }
                }
            }
            setMaxDistance(maxDist);
    }

    public int getNumExamples(int classVal) {
        if (classVal == 0) {
            return ts1s.size();
        } else {
            return ts2s.size();
        }
    }

    protected void detectContinuous() {
        //Chop to sizes between minSizeInExamples, min(current ts size, maxSizeInExamples)
        // and look for best match.
        int min = minGestureSize;
        if (min > minSizeInExamples) { // ?
            min = minSizeInExamples;
        }

        List<TimeSeries> l = getCandidateSeries(currentTs, min, maxSizeInExamples);
        //System.out.println("Got candidate for min " + min + " max " + maxSizeInExamples + " size" + l.size());

        distanceTo1 = Double.MAX_VALUE;
        distanceTo2 = Double.MAX_VALUE;

        for (TimeSeries candidate : l) {
            for (TimeSeries ts : ts1s) {
                TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts, candidate, 5);
                if (distanceTo1 > info.getDistance()) {
                    distanceTo1 = info.getDistance();

                }
            }

            for (TimeSeries ts : ts2s) {
                TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts, candidate, 5);
               // System.out.println(info.getDistance());
                if (distanceTo2 > info.getDistance()) {
                    distanceTo2 = info.getDistance();
                }
            }

        }
        double closestDist = distanceTo1;
        int closestClass = 1;
        if (distanceTo2 < closestDist) {
            closestDist = distanceTo2;
            closestClass = 2;
           // System.out.print("2 is closest: " + closestDist);
        } else {
            //System.out.print("1 is closest: " + closestDist);
        }

        System.out.println("Closest is " + closestDist + ", match th is " + matchThreshold + " length =" + l.size());
        if (closestDist < matchThreshold) {
            System.out.println("MATCHES " + closestClass);
            setContinuousMatch(closestClass);
        } else {
            System.out.println("NO Match");
            setContinuousMatch(-1);
        }
        //TODO: Callbacks -- for state update in GUI
    }

    protected List<TimeSeries> getCandidateSeries(TimeSeries t, int minSize, int maxSize) {
        List<TimeSeries> l = new LinkedList<TimeSeries>();
        //hop size = 1

        if (t.size() < minSize) {
            // l.add(new TimeSeries(t)); //don't do anything
          //  System.out.println("T too small: " + t.size());
            return l;
        }

        int shortestStartPos = t.size() - minSize;
        int longestStartPos;
        if (t.size() > maxSize) {
            longestStartPos = t.size() - maxSize;
        } else {
            longestStartPos = 0;
        }
        
      //  System.out.println("Shortest start = " + shortestStartPos + ", longest = " + longestStartPos);

        int hopSize = 1;

        // int windowNum = 0;
        for (int startPos = longestStartPos; startPos <= shortestStartPos; startPos = startPos + hopSize) {
            TimeSeries tt = new TimeSeries(t.numOfDimensions());
            for (int i = 0; i < t.size() - startPos; i++) {
                double[] next = t.getMeasurementVector(startPos + i);
                tt.addLast(new Double(i), new TimeSeriesPoint(next));
            }
            l.add(tt);
        }

        /*        System.out.println("Testing windowing: ");
        for (int i= 0; i < l.size(); i++) {
        System.out.println("Windowed # " + i + " is: " + l.get(i).toString());

        } */

        return l;
    }

    public int classifyLast() {
        double closestDist = Double.MAX_VALUE;
        distanceTo1 = Double.MAX_VALUE;
        distanceTo2 = Double.MAX_VALUE;

        int closestClass = 1;

        System.out.println("Distance 1s:");
        for (TimeSeries ts : ts1s) {
            TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts, currentTs, 5);
            System.out.println(info.getDistance());
            if (distanceTo1 > info.getDistance()) {
                distanceTo1 = info.getDistance();
            }
        }

        System.out.println("Distance 2s:");
        for (TimeSeries ts : ts2s) {
            TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts, currentTs, 5);
            System.out.println(info.getDistance());
            if (distanceTo2 > info.getDistance()) {
                distanceTo2 = info.getDistance();
            }
        }
        if (distanceTo1 < distanceTo2) {
            return 1;
        } else {
            return 2;
        }
    }

    public double getLastDistance(int gestureClass) {
        if (gestureClass == 0) {
            return distanceTo1;
        } else {
            return distanceTo2;
        }

    }

    public void printState() {
        System.out.println("Gesture 1 TimeSeries:");
        for (TimeSeries ts : ts1s) {
            System.out.println("Contains: ");
            System.out.println(ts);
            System.out.println(ts.getTimeAtNthPoint(0));
        }
        System.out.println("Gesture 2 Timeseries:");
        for (TimeSeries ts : ts2s) {
            System.out.println("Contains: ");
            System.out.println(ts);
            System.out.println(ts.getTimeAtNthPoint(0));

        }
        System.out.println("Current Timeseries:");
        System.out.println(currentTs);
        System.out.println(currentTs.getTimeAtNthPoint(0));


    }
//     protected void trainingExampleAdded(int classValue) {
//        // Guaranteed to return a non-null array
//        Object[] listeners = trainingListenerList.getListenerList();
//        // Process the listeners last to first, notifying
//        // those that are interested in this event
//        for (int i = listeners.length - 2; i >= 0; i -= 2) {
//            if (listeners[i] == TrainingAddedListener.class) {
//                ((TrainingAddedListener) listeners[i + 1]).fireTrainingExampleRecorded(classValue);
//            }
//        }
//    }
//
//        protected void newClassificationResult(int id, int classValue) {
//        // Guaranteed to return a non-null array
//        Object[] listeners = classificationListenerList.getListenerList();
//        // Process the listeners last to first, notifying
//        // those that are interested in this event
//        for (int i = listeners.length - 2; i >= 0; i -= 2) {
//            if (listeners[i] == ClassificationListener.class) {
//                ((ClassificationListener) listeners[i + 1]).fireClassificationResult(id, classValue);
//            }
//        }
//    }
}
