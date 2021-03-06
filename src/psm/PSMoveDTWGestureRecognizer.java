/*
 * Implements Dynamic Time Warping for PSMove Gesture Recognizer -- does the heavy lifting. Shouldn't need to edit this.
 * 
 */
package psm;

import com.dtw.TimeWarpInfo;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import com.timeseries.TimeSeries;
import com.timeseries.TimeSeriesPoint;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 *
 * @author fiebrink
 */
public class PSMoveDTWGestureRecognizer implements GestureRecognizer {

    protected int receivePort = 6448; //Send PSMove motion data via osc to this port. Set this in constructor.
    protected String oscReceiveMessage = "/oscCustomFeatures"; //Send PSMove motion data via osc using this message name
    protected int sendPort = 6449;
    protected OSCPortOut sender;
    protected InetAddress sendToHost;
    protected String oscSendMessage = "/gesturePerformed";
    
    

    protected int matchWidth = 5; //This could be changed to tweak DTW behavior
    protected int minAllowedGestureLength = 5; //This could be changed to tweak DTW behavior
     
    
    protected int numFeatures = 9; //This is set in constructor
    protected int numClasses = 2; //This is set in constructor

    protected ArrayList<LinkedList<TimeSeries>> allseries;
    protected TimeSeries currentTs = new TimeSeries(numFeatures);
    protected int currentTime = 0;
    protected int currentTrainingLabel = 0;
    protected double[] distanceToClasses;
    protected double closestDist = Double.MAX_VALUE;
    protected boolean isClassActive[];
    protected int minSizeInExamples = 10; 
    protected int maxSizeInExamples = 10;
    protected double matchThreshold = 3.0;
   
    protected double maxDistance;
    protected int continuousMatch; //identity of closest matching class (-1 if no class is above match threshold)
    protected OSCPortIn receiver;
            

    protected RecordingState recordingState = RecordingState.NOT_RECORDING;
    protected RunningState runningState = RunningState.NOT_RUNNING;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    protected EventListenerList continuousClassificationListenerList = new EventListenerList();
    private ChangeEvent changeEvent = null;

    // Create new recognizer for # features (e.g., 9 for PS Move), numClasses (e.g., 4 gesture types)
    public PSMoveDTWGestureRecognizer(int numFeatures, int numClasses, int receivePort, int sendPort, InetAddress sendToHost) throws SocketException, UnknownHostException, Exception {
        this.numClasses = numClasses;
        this.numFeatures = numFeatures;
        this.receivePort = receivePort;
        this.sendToHost = sendToHost;
        this.sendPort = sendPort;
        isClassActive = new boolean[numClasses];
        for (int i = 0; i < isClassActive.length; i++) {
            isClassActive[i] = true;
        }
        initialize();
    }
   
    
    /* Call this constructor if you want to load from a file */
    public PSMoveDTWGestureRecognizer(File f, int receivePort, int sendPort, InetAddress sendToHost) throws SocketException, UnknownHostException, Exception {
        this.receivePort = receivePort;
        this.sendPort = sendPort;
        this.sendToHost = sendToHost;
        initialize();
        loadFromFile(f);
    }
   

    
    //Phoenix: See this as an example of using ChangeListener to implement observers
    public void addClassificationListener(ChangeListener l) {
        continuousClassificationListenerList.add(ChangeListener.class, l);
    }

    public void removeClassificationListener(ChangeListener l) {
        continuousClassificationListenerList.remove(ChangeListener.class, l);
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

    public boolean getIsClassActive(int c) {
        return isClassActive[c];
    }

    public void setIsClassActive(boolean isActive, int c) {
        this.isClassActive[c] = isActive;
    }

    public void deleteLast(int classNum) {
        LinkedList<TimeSeries> ts = allseries.get(classNum);
        if (ts.size() > 0) {
            ts.removeLast();
        }
    }
    
        
    private void initialize() throws SocketException, UnknownHostException, Exception {
        addOscListeners();
        sender = new OSCPortOut(sendToHost, sendPort);
        
        allseries = new ArrayList<LinkedList<TimeSeries>>(numClasses);
        distanceToClasses = new double[numClasses];
        for (int i = 0; i < numClasses; i++) {
            LinkedList<TimeSeries> l = new LinkedList<TimeSeries>();
            allseries.add(l);
            distanceToClasses[i] = Double.MAX_VALUE;
        }
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
        // System.out.println("Continuous match set to " + continuousMatch);
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
        //   System.out.println("MAX DISTANCE set to " + maxDistance);
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
     * Set the value of match threshold, tolerance we use to identify a match with any category
     *
     * @param matchThreshold new value of match threshold
     */
    public void setMatchThreshold(double matchThreshold) {
        double oldMatchThreshold = this.matchThreshold;
        this.matchThreshold = matchThreshold;
                System.out.println("Match threshold set to " + matchThreshold);

        propertyChangeSupport.firePropertyChange(PROP_MAXTHRESHOLD, oldMatchThreshold, matchThreshold);
    }


    /* If we're adding a new gesture training example, which class does it belong to? */
    public void setCurrentTrainingLabel(int i) {
        currentTrainingLabel = i;
    }

    public int getCurrentTrainingLabel() {
        return currentTrainingLabel;
    }

    /* Delete all examples for a class */
    public void deleteExamples(int whichClass) {
        LinkedList<TimeSeries> listToClear = allseries.get(whichClass);
        listToClear.clear();
    }

    protected void updateExampleSizeStats() {
        minSizeInExamples = minAllowedGestureLength;
        maxSizeInExamples = 0;
        for (LinkedList<TimeSeries> list : allseries) {
            for (TimeSeries ts : list) {
                if (ts.size() < minSizeInExamples) {
                    minSizeInExamples = ts.size();
                }
                if (ts.size() > maxSizeInExamples) {
                    maxSizeInExamples = ts.size();
                    System.out.println("Max size is now " + maxSizeInExamples);
                }
            }
        }
    }

    /* Start running -- looking for gestures & doing analysis */
    public void startRunning() {
        if (runningState != RunningState.RUNNING) {
            //Note: For now, this should be fine even if classifier is untrained / no data.
            setRunningState(RunningState.RUNNING);
            currentTime = 0;
            currentTs = new TimeSeries(numFeatures);
            updateExampleSizeStats();
        }
    }

    // Update our knowledge of the max distance between gestures in our training set
    protected void updateMaxDistance() {
        //Update threshold info
        double maxDist = 0.0;

        int numClassesWithExamples = 0;
        int whichClass = -1;

        for (int i = 0; i < allseries.size(); i++) {
            //For now, include inactive classes here.
            LinkedList<TimeSeries> list1 = allseries.get(i);
            if (list1.size() > 0) {
                numClassesWithExamples++;
                whichClass = i;

            }

            for (int j = i + 1; j < allseries.size(); j++) {
                LinkedList<TimeSeries> list2 = allseries.get(j);

                //Find max distance between list1 and list2
                for (TimeSeries ts1 : list1) {
                    for (TimeSeries ts2 : list2) {
                        TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts1, ts2, matchWidth);
                        if (info.getDistance() > maxDist) {
                            maxDist = info.getDistance();
                        }
                    }
                }
            }

        }

        if (numClassesWithExamples == 1) {

            LinkedList<TimeSeries> list = allseries.get(whichClass);
            for (int i = 0; i < list.size(); i++) {
                TimeSeries ts1 = list.get(i);
                for (int j = i + 1; j < list.size(); j++) {
                    TimeSeries ts2 = list.get(j);
                    TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts1, ts2, matchWidth);
                    if (info.getDistance() > maxDist) {
                        maxDist = info.getDistance();
                    }
                }
            }
            maxDist = maxDist * 10; // just in case.

        }

        setMaxDistance(maxDist);
    }

    /* Stop running */
    public void stopRunning() {
        if (runningState == RunningState.RUNNING) {
            setRunningState(RunningState.NOT_RUNNING);
        }
    }

    public RecordingState getRecordingState() {
        return recordingState;
    }

    /* Start recording a new gesture */
    public void startRecording() {
        if (recordingState == RecordingState.NOT_RECORDING) {
            setRecordingState(RecordingState.RECORDING);
            currentTime = 0;
            // currentTs.clear();
            currentTs = new TimeSeries(numFeatures);
        }
    }

    /* Stop recording the new gesture */
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
        addOscFeatureListener();
        receiver.startListening();
        System.out.println("Listening...");
    }
    private static int tmp = 0;

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
                if (getRecordingState() == RecordingState.RECORDING) {
                    addTrainingVector(d); //calls newTrainingExampleRecorded
                    //newTrainingExampleRecorded(id);
                } else if (getRunningState() == RunningState.RUNNING) {
                    addContinuousRunVector(d);
                }
                tmp++;
            }
        };
        receiver.addListener(oscReceiveMessage, listener);
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
        LinkedList<TimeSeries> list = allseries.get(currentTrainingLabel);
        list.add(currentTs);

        //Update threshold info
        updateMaxDistance();
    }

    public int getNumExamples(int classVal) {
        return allseries.get(classVal).size();
    }

    protected void detectContinuous() {
        //Chop to sizes between minSizeInExamples, min(current ts size, maxSizeInExamples)
        // and look for best match.

        int min = minAllowedGestureLength;
        if (min > minSizeInExamples) {
            min = minSizeInExamples;
        }

        List<TimeSeries> l = getCandidateSeries(currentTs, min, maxSizeInExamples);

        distanceToClasses = new double[numClasses];
        closestDist = Double.MAX_VALUE;
        int closestClass = -1;

        for (int i = 0; i < distanceToClasses.length; i++) {
            distanceToClasses[i] = Double.MAX_VALUE;
        }

        for (int whichClass = 0; whichClass < numClasses; whichClass++) {
            if (isClassActive[whichClass]) {
                for (TimeSeries candidate : l) {
                    for (TimeSeries ts : allseries.get(whichClass)) {
                        TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts, candidate, 5);
                        if (distanceToClasses[whichClass] > info.getDistance()) {
                            distanceToClasses[whichClass] = info.getDistance();
                        }
                        if (info.getDistance() < closestDist) {
                            closestDist = info.getDistance();
                            closestClass = whichClass;
                        }
                    }
                }
            }
        }

        fireNewClassificationResult();
        // System.out.println("Closest is " + closestDist + ", match th is " + matchThreshold + " length =" + l.size());
        if (closestDist < matchThreshold) {
            // System.out.println("MATCHES " + closestClass);
            setContinuousMatch(closestClass);
            sendOsc(closestClass);
        } else {
            // System.out.println("NO Match");
            setContinuousMatch(-1);
        }
    }

    
    protected void sendOsc(int match) {
        Object[] o = new Object[1];
        o[0] = match;
        OSCMessage msg = new OSCMessage(oscSendMessage, o);
        try {
              sender.send(msg);
              //System.out.println("Osc message sent");
        } catch (IOException ex) {
              Logger.getLogger(PsMoveGestureRecognizerTrainingGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
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

        /*
         * System.out.println("Testing windowing: "); for (int i= 0; i <
         * l.size(); i++) { System.out.println("Windowed # " + i + " is: " +
         * l.get(i).toString());
         *
         * }
         */

        return l;
    }

    //Used for debugging & single classification, not for real-time
    public int classifyLast() {
        closestDist = Double.MAX_VALUE;
        int closestClass = -1;
        distanceToClasses = new double[numClasses];
        for (int i = 0; i < distanceToClasses.length; i++) {
            distanceToClasses[i] = Double.MAX_VALUE;
        }

        for (int whichClass = 0; whichClass < numClasses; whichClass++) {
            if (isClassActive[whichClass]) {
                for (TimeSeries ts : allseries.get(whichClass)) {
                    TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(ts, currentTs, 5);
                    if (distanceToClasses[whichClass] > info.getDistance()) {
                        distanceToClasses[whichClass] = info.getDistance();
                    }
                    if (info.getDistance() < closestDist) {
                        closestDist = info.getDistance();
                        closestClass = whichClass;
                    }
                }
            }
        }

        return closestClass; //doesn't use threshold   
    }

    public double getLastDistance(int gestureClass) {
        return distanceToClasses[gestureClass];
    }

    public double[] getLastDistances() {
        return distanceToClasses;
    }

    //For debugging only
    public void printState() {
        System.out.println("DTW: " + numClasses + " classes");
        System.out.println("Closest dist: " + closestDist);
        for (int i = 0; i < numClasses; i++) {
            System.out.println("CLASS " + i + ": + "
                    + allseries.get(i).size()
                    + "points, closest dist =" + distanceToClasses[i] + ")");
            for (TimeSeries ts : allseries.get(i)) {
                System.out.println(ts);
            }
        }

        System.out.println("CURRENT Timeseries:");
        System.out.println(currentTs);
    }

    //Loads state of this recognizer from a file (e.g. gesture.txt)
    public void loadFromFile(File f) throws Exception {
        stopRecording();
        stopRunning();
        
        BufferedReader in = new BufferedReader(new FileReader(f));
        numFeatures = Integer.parseInt(in.readLine());
        numClasses = Integer.parseInt(in.readLine());
       // PSMoveDTWGestureRecognizer gr = new PSMoveDTWGestureRecognizer(numFeatures, numClasses);
        currentTime = Integer.parseInt(in.readLine());
        currentTrainingLabel = Integer.parseInt(in.readLine());
        closestDist = Double.parseDouble(in.readLine());
        distanceToClasses = new double[Integer.parseInt(in.readLine())];
        for (int i= 0; i < distanceToClasses.length; i++) {
            distanceToClasses[i] = Double.parseDouble(in.readLine());      
        }
        isClassActive = new boolean[Integer.parseInt(in.readLine())];
        for (int i = 0; i < isClassActive.length; i++) {
            int ii = Integer.parseInt(in.readLine());
            isClassActive[i] = (ii == 1);
        }
        minAllowedGestureLength = Integer.parseInt(in.readLine());
        minSizeInExamples = Integer.parseInt(in.readLine());
        maxSizeInExamples = Integer.parseInt(in.readLine());
        matchWidth = Integer.parseInt(in.readLine());
        double matchThreshold = Double.parseDouble(in.readLine());
        System.out.println("Match threshold read as " + matchThreshold + " in file read");
        setMatchThreshold(matchThreshold);
        
        maxDistance = Double.parseDouble(in.readLine());
        continuousMatch = Integer.parseInt(in.readLine());
        System.out.println("Is this TIMESERRIES?" + in.readLine());
        getTimeSeriesFromInput(in);
        
        currentTs = new TimeSeries(numFeatures);        
        in.close();
    }

    //Write entire state of this object to a file, so you can load it later
    public void writeToFile(File f) throws IOException {
        /*
         * FileOutputStream fout = new FileOutputStream(f); ObjectOutputStream o
         * = new ObjectOutputStream(fout); writeToOutputStream(o); o.close();
         * fout.close();
         */
        stopRecording();
        stopRunning();
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.write(Integer.toString(numFeatures) + "\n");
        writer.write(Integer.toString(numClasses) + "\n");
        writer.write(Integer.toString(currentTime) + "\n");
        writer.write(Integer.toString(currentTrainingLabel) + "\n"); //?
        writer.write(Double.toString(closestDist) + "\n");
        writer.write(Integer.toString(distanceToClasses.length) + "\n");
        for (int i = 0; i < distanceToClasses.length; i++) {
            writer.write(Double.toString(distanceToClasses[i]) + "\n");
        }
        writer.write(Integer.toString(isClassActive.length) + "\n");
        for (int i = 0; i < isClassActive.length; i++) {
            writer.write(Integer.toString(isClassActive[i] ? 1 : 0) + "\n");
        }
        writer.write(Integer.toString(minAllowedGestureLength) + "\n");
        writer.write(Integer.toString(minSizeInExamples) + "\n");
        writer.write(Integer.toString(maxSizeInExamples) + "\n");
        writer.write(Integer.toString(matchWidth) + "\n");
        writer.write(Double.toString(matchThreshold) + "\n");
        writer.write(Double.toString(maxDistance) + "\n");
        writer.write(Integer.toString(continuousMatch) + "\n");

        writer.write("TIMESERIES:\n");
        writer.write(convertTimeSeriesToOutput(allseries));
        //    writer.write(convertTimeSeriesToOutput(currentTs)); //TODO
        writer.close();


    }

    protected void getTimeSeriesFromInput(BufferedReader in) throws Exception {        
        allseries.clear();
        int numAl = Integer.parseInt(in.readLine());
        for (int i = 0; i < numAl; i++)
        {
            LinkedList<TimeSeries> l = new LinkedList<TimeSeries>();
            allseries.add(l);
            
            int numLl = Integer.parseInt(in.readLine());
           
            for (int ii = 0; ii < numLl; ii++) {
                int numDimensions = Integer.parseInt(in.readLine());
                TimeSeries ts = new TimeSeries(numDimensions);
                int numPoints = Integer.parseInt(in.readLine());
                
                for (int point = 0; point < numPoints; point++) {
                    double timeAtPoint = Double.parseDouble(in.readLine());
                    double[] m = new double[numDimensions];
                    for (int dim = 0; dim < numDimensions; dim++ )
                    {
                        m[dim] = Double.parseDouble(in.readLine());
                        
                    }
                    
                    ts.addLast(timeAtPoint, new TimeSeriesPoint(m));
                }
                //Time series is now constructed
                l.add(ts);
            }          
        }
    }
    
    
    protected String convertTimeSeriesToOutput(ArrayList<LinkedList<TimeSeries>> al_ll_ts) {
        StringBuilder s = new StringBuilder("");
        int numAl = al_ll_ts.size();
        s.append(numAl);
        s.append("\n");
        for (LinkedList<TimeSeries> ll_ts : al_ll_ts) {
            int numLl = ll_ts.size();
            s.append(numLl);
            s.append("\n");
            for (TimeSeries ts : ll_ts) {
                s.append(ts.numOfDimensions());
                s.append("\n");

                s.append(ts.numOfPts());
                s.append("\n");

                for (int i = 0; i < ts.numOfPts(); i++) {
                    s.append(Double.toString(ts.getTimeAtNthPoint(i)));
                    s.append("\n");
                    double[] m = ts.getMeasurementVector(i);
                    for (int j = 0; j < m.length; j++) {
                        s.append(Double.toString(m[j]));
                        s.append("\n");
                    }
                }

            }
        }

        return s.toString();
    }

}
