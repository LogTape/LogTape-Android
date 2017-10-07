package se.tightloop.logtapeandroid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dnils on 2017-10-07.
 */
/**
 *  A property supplier is used to provide LogTape with additional
 *  properties that will be shown in the uploaded log report.
 */

public abstract class LogTapePropertySupplier {
    class LogProperty {
        String label = "";
        String value = "";
    };

    List<LogProperty> properties = new ArrayList<LogProperty>();

    /**
     * Adds a new property to the list of properties.
     *
     * @param label The label of the property you wish to add
     * @param value The value of the property you wish to add
     */
    protected void addProperty(String label, String value)  {
        if (label != null && value != null) {
            LogProperty property = new LogProperty();
            property.label = label;
            property.value = value;
            properties.add(property);
        }
    }

    /**
     * Add the properties that should be part of the log report.
     * Make repeated calls to {@link LogTapePropertySupplier#addProperty(String, String)}
     * to add properties.
     */
    abstract public void populate();


    List<LogProperty> getProperties() {
        properties.clear();
        populate();
        return properties;
    }
}
