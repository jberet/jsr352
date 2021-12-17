package org.jberet.jpa.repository;

import java.util.Properties;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.jberet.util.BatchUtil;

/**
 *
 * @author a.moscatelli
 */
@Converter
public class PropertiesConverter implements AttributeConverter<Properties, String> {

    @Override
    public String convertToDatabaseColumn(Properties x) {
        return BatchUtil.propertiesToString(x);
    }

    @Override
    public Properties convertToEntityAttribute(String y) {
        return BatchUtil.stringToProperties(y);
    }
    
}
