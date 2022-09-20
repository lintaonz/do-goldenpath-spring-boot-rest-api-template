package nz.co.twg.service.{{cookiecutter.java_package_name}}.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
* A supplier for {@link ObjectMapper}. Use this class to ensure all {@link ObjectMapper} configs
* are equivalent when used throughout the application.
*/
public class ObjectMapperSupplier implements Supplier<ObjectMapper> {

    @Override
    public ObjectMapper get() {
        return decorate(new ObjectMapper());
    }

    /** Configure the object mapper to include some TWG specific configuration. */
    public ObjectMapper decorate(ObjectMapper objectMapper) {
        // The encryption logic walks the POJO's object tree and constructs unique identifying keys
        // based on its member's path. However, if the member to traverse happens to be a Set, the order
        // of these keys may become unpredictable (it still iterate through the collection and assign an
        // index to each key). This usually works fine since the encryption logic knows which field the
        // key is associated to, but in the event where the data is mocked, the user creating the mock
        // data may not know which Set index is associated with which field.
        //
        // To add predictability for the benefit of component tests, the Set and Map has been configured
        // to use the "Linked" variant to maintain the ordering of the collection in the insertion
        // sequence.
        // NOTE: This only works when the mocked data is marshalled through the object mapper. For
        // mocked data that is handcrafted with love, those will need to make use of Linked Set
        // collection manually.
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addAbstractTypeMapping(Set.class, LinkedHashSet.class);
        // by default, Map already make use of LinkedHashMap. Only adding this here for sanity-sake.
        simpleModule.addAbstractTypeMapping(Map.class, LinkedHashMap.class);

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // required for writing date objects as String representation
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(simpleModule);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
