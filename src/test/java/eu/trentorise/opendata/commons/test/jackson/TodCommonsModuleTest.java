/*
 * Copyright 2015 Trento Rise.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.trentorise.opendata.commons.test.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import eu.trentorise.opendata.commons.BuilderStyle;
import eu.trentorise.opendata.commons.Dict;
import eu.trentorise.opendata.commons.LocalizedString;
import eu.trentorise.opendata.commons.TodConfig;
import eu.trentorise.opendata.commons.PeriodOfTime;
import eu.trentorise.opendata.commons.jackson.TodCommonsModule;
import static eu.trentorise.opendata.commons.test.jackson.TodJacksonTester.changeField;
import static eu.trentorise.opendata.commons.test.jackson.TodJacksonTester.testJsonConv;
import eu.trentorise.opendata.commons.validation.Ref;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.immutables.value.Value;

/**
 *
 * @author David Leoni
 */
public class TodCommonsModuleTest {

    private static final Logger LOG = Logger.getLogger(TodCommonsModuleTest.class.getName());

    private ObjectMapper objectMapper;

    @BeforeClass
    public static void beforeClass() {
        TodConfig.init(TodCommonsModuleTest.class);
    }

    @Before
    public void before() {
        objectMapper = new ObjectMapper();
        TodCommonsModule.registerModulesInto(objectMapper);
    }

    @After
    public void after() {
        objectMapper = null;
    }

    @Test
    public void testDict() throws JsonProcessingException, IOException {

        testJsonConv(objectMapper, LOG, Dict.of("a", "b"));
        testJsonConv(objectMapper, LOG, Dict.of(Locale.FRENCH, "a", "b"));

        Dict dict = objectMapper.readValue("{}", Dict.class);
        assertEquals(Dict.of(), dict);

        try {
            Dict dict_2 = objectMapper.readValue("{\"it\":null}", Dict.class);
            Assert.fail("Should have validated the dict!");
        }
        catch (Exception ex) {

        }
    }

    @Test
    public void testLocalizedString() throws JsonProcessingException, IOException {

        testJsonConv(objectMapper, LOG, LocalizedString.of(Locale.FRENCH, "a"));

        String json = changeField(objectMapper, LOG, LocalizedString.of(Locale.ITALIAN, "a"), "string", NullNode.instance);

        try {
            objectMapper.readValue(json, LocalizedString.class);
            Assert.fail("Should not accept null values!");
        }
        catch (Exception ex) {

        }

        try {
            objectMapper.readValue("{\"string\":\"a\"}", LocalizedString.class);
            Assert.fail("Should have failed because no locale field was provided!");
        }
        catch (Exception ex) {

        }

    }

    @Test
    public void testPeriodOfTime () throws JsonProcessingException, IOException {

	testJsonConv(objectMapper, LOG, PeriodOfTime.of("bla"));
	
        testJsonConv(objectMapper, LOG, PeriodOfTime.of("1980", "1990"));
        
        testJsonConv(objectMapper, LOG, PeriodOfTime.of("1980", "1990", "bla"));

    }
    
    /**
     * todo we're not really using this custom validation stuff 
     */
    @Test
    @Ignore
    public void testValidationError() throws JsonProcessingException, IOException {

        testJsonConv(objectMapper, LOG, Ref.builder()
        	.setPhysicalRow(1)
        	.setTracePath("a"))
        	.build();
        
        /*
        testJsonConv(objectMapper, LOG, ValidationError.of(
        	Ref.builder().setTracePath("$a.b").build(),
        	ErrorLevel.INFO, "2", "a%x", "x", "b"));
	*/
        
        //String json = changeField(objectMapper, LOG, ValidationError.of("$a.b", 2, "c") , "ref", NullNode.instance);
        //assertEquals("*", objectMapper.readValue(json, ValidationError.class).getRef().getJsonPath());
    }

    /**
     * Seems it doesn't work with empty constructors
     * 
     * @see #testEmptyConstructorForBuilder() 
     */
    @Test(expected = com.fasterxml.jackson.databind.JsonMappingException.class)
    public void testEmptyConstructor() throws IOException {
        assertEquals(LocalizedString.of(), objectMapper.readValue("{}", LocalizedString.class));
    }

    @JsonSerialize(as = B.class)
    @JsonDeserialize(as = B.class)
    @Value.Immutable
    @BuilderStyle
    abstract static class AB {

        @Value.Default
        public int getProp1() {
            return 1;
        }
        
        @Value.Default
        public int getProp2() {
            return 2;
        }
        
    }

    /**
     * This works as Jackson by default initializes missing fields with null and
     * builder has always default values to substitute to nulls
     * 
     * @see #testEmptyConstructor() 
     */
    @Test
    public void testEmptyConstructorForBuilder() throws IOException {
        B b = objectMapper.readValue("{\"prop1\":3}", B.class);
        LOG.log(Level.FINE, "b.prop2 = {0}", "'" + b.getProp2() + "'");
        assertEquals(B.of().withProp1(3), b);
    }

    @Test
    public void example1() throws JsonProcessingException, IOException {

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new GuavaModule());
        om.registerModule(new TodCommonsModule());

        String json = om.writeValueAsString(LocalizedString.of(Locale.ITALIAN, "ciao"));
        LocalizedString reconstructedLocalizedString = om.readValue(json, LocalizedString.class);
    }

    @Test
    public void example2() throws JsonProcessingException, IOException {

        ObjectMapper om = new ObjectMapper();
        TodCommonsModule.registerModulesInto(om);

        String json = om.writeValueAsString(LocalizedString.of(Locale.ITALIAN, "ciao"));
        LocalizedString reconstructedLocalizedString = om.readValue(json, LocalizedString.class);
    }

    private static class RootLocale {

        public Locale locale;
    }

    /**
     * Shows that contrary to Jackson we deserialize "" into {@link Locale#ROOT}
     * instead of nasty null.
     *
     * @since 1.1.0
     */
    @Test
    public void testEmptyLocaleDeser() throws JsonProcessingException, IOException {
        RootLocale rootLocale = new RootLocale();
        rootLocale.locale = Locale.ROOT;
        String json = objectMapper.writeValueAsString(rootLocale);
        LOG.log(Level.FINE, "json = {0}", json);
        RootLocale res = objectMapper.readValue(json, RootLocale.class);
        assertNotNull(res.locale);
        assertEquals(Locale.ROOT, res.locale);
    }

    /**
     * Shows that contrary to Jackson we deserialize "" into {@link Locale#ROOT}
     * instead of nasty null.
     *
     * Expects error because super nasty Jackson doesn't even call the
     * {@link eu.trentorise.opendata.commons.jackson.TodCommonsModule.LocaleDeserializer}
     * Sic...
     *
     * @since 1.1.0
     */
    @Test(expected = AssertionError.class)
    public void testNullLocaleDeser() throws JsonProcessingException, IOException {
        RootLocale rootLocale = new RootLocale();
        rootLocale.locale = null;
        String json = objectMapper.writeValueAsString(rootLocale);
        LOG.log(Level.FINE, "json = {0}", json);
        RootLocale res = objectMapper.readValue(json, RootLocale.class);
        assertEquals(Locale.ROOT, res.locale);
    }

    /**
     * Tests weird module equality copied from Guava module
     */
    @Test
    public void testEquality() {
        TodCommonsModule sm = new TodCommonsModule();
        assertEquals(sm, sm);
        assertEquals(new TodCommonsModule().hashCode(), new TodCommonsModule().hashCode());
        assertNotEquals(new TodCommonsModule(), new TodCommonsModule());
    }

}
