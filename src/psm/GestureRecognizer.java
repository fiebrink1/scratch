/*
 * Interface for classes that recognize gestures (i.e., DTW, Targeter)
 */
package psm;

import java.beans.PropertyChangeListener;
import javax.swing.event.ChangeListener;

/**
 *
 * @author fiebrink
 */
public interface GestureRecognizer {

    String PROP_CONTINUOUSMATCH = "continuousMatch";
    String PROP_MAXDISTANCE = "maxDistance";
    String PROP_MAXTHRESHOLD = "maxThreshold";
    public static String PROP_RECORDINGSTATE = "recordingState";
    public static String PROP_RUNNINGSTATE = "runningState";

    void addClassificationListener(ChangeListener l);

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

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    void deleteExamples(int whichClass);

    void deleteLast(int classNum);

    /**
     * Get the value of continuousMatch
     *
     * @return the value of continuousMatch
     */
    int getContinuousMatch();

    int getCurrentTrainingLabel();

    boolean getIsClassActive(int c);

    double getLastDistance(int gestureClass);

    double[] getLastDistances();

    /**
     * Get the value of maxDistance
     *
     * @return the value of maxDistance
     */
    double getMatchThreshold();

    /**
     * Get the value of maxDistance
     *
     * @return the value of maxDistance
     */
    double getMaxDistance();

    int getNumExamples(int classVal);

    RecordingState getRecordingState();

    RunningState getRunningState();

    void printState();

    void removeClassificationListener(ChangeListener l);

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Set the value of continuousMatch
     *
     * @param continuousMatch new value of continuousMatch
     */
    void setContinuousMatch(int continuousMatch);

    void setCurrentTrainingLabel(int i);

    void setIsClassActive(boolean isActive, int c);

    /**
     * Set the value of maxDistance
     *
     * @param maxDistance new value of maxDistance
     */
    void setMatchThreshold(double matchThreshold);

    void startRecording();

    void startRunningContinuous();

    void stopRecording();

    void stopRunning();
}
