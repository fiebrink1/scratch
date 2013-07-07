/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PropertyExamplesForPhoenix;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 * @author fiebrink
 */
public class ExampleObjectListeningToProps {
    public static void main(String[] args) {
        
        ExampleObjectWithProps obj = new ExampleObjectWithProps();

        obj.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                if (pce.getPropertyName().equals(ExampleObjectWithProps.PROP_MYINTPROPERTY)) {
                    System.out.println("Hey! The int property changed to " + pce.getNewValue());              
                } else if (pce.getPropertyName().equals(ExampleObjectWithProps.PROP_MYAWESOMESTRING)) {
                    System.out.println("The string changed.");
                    
                }
            }
        });   
        obj.setMyIntProperty(10);        
        obj.setMyAwesomeString("new value");

    }
}
