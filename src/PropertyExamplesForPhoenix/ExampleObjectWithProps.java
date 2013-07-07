/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PropertyExamplesForPhoenix;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author fiebrink
 */
public class ExampleObjectWithProps {
    
    private int myIntProperty = 0;
    public static final String PROP_MYINTPROPERTY = "myIntProperty";


    private String myAwesomeString;
    public static final String PROP_MYAWESOMESTRING = "myAwesomeString";

    /**
     * Get the value of myAwesomeString
     *
     * @return the value of myAwesomeString
     */
    public String getMyAwesomeString() {
        return myAwesomeString;
    }

    /**
     * Set the value of myAwesomeString
     *
     * @param myAwesomeString new value of myAwesomeString
     */
    public void setMyAwesomeString(String myAwesomeString) {
        String oldMyAwesomeString = this.myAwesomeString;
        this.myAwesomeString = myAwesomeString;
        propertyChangeSupport.firePropertyChange(PROP_MYAWESOMESTRING, oldMyAwesomeString, myAwesomeString);
    }

    
    
    public ExampleObjectWithProps() {
  
    }


    /**
     * Get the value of myIntProperty
     *
     * @return the value of myIntProperty2
     */
    public int getMyIntProperty() {
        return myIntProperty;
    }

    /**
     * Set the value of myIntProperty
     *
     * @param myIntProperty new value of myIntProperty
     */
    public void setMyIntProperty(int myIntProperty) {
        int oldMyIntProperty = this.myIntProperty;
        this.myIntProperty = myIntProperty;
        propertyChangeSupport.firePropertyChange(PROP_MYINTPROPERTY, oldMyIntProperty, myIntProperty);
    }
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

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

}
