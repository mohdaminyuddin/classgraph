/*
 * This file is part of FastClasspathScanner.
 * 
 * Author: Luke Hutchison <luke .dot. hutch .at. gmail .dot. com>
 * 
 * Hosted at: https://github.com/lukehutch/fast-classpath-scanner
 * 
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Luke Hutchison
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.lukehutch.fastclasspathscanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.github.lukehutch.fastclasspathscanner.matchprocessor.FileMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.StaticFinalFieldMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.SubclassMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.SubinterfaceMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Cls;
import io.github.lukehutch.fastclasspathscanner.whitelisted.ClsSub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.ClsSubSub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Iface;
import io.github.lukehutch.fastclasspathscanner.whitelisted.IfaceSub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.IfaceSubSub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Impl1;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Impl1Sub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Impl1SubSub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Impl2;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Impl2Sub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.Impl2SubSub;
import io.github.lukehutch.fastclasspathscanner.whitelisted.StaticField;
import io.github.lukehutch.fastclasspathscanner.whitelisted.blacklisted.Blacklisted;
import static org.junit.Assert.assertTrue;

public class FastClasspathScannerTest {
    private static final String WHITELIST_PACKAGE = Cls.class.getPackage().getName();
    private static final String BLACKLIST_PACKAGE = Blacklisted.class.getPackage().getName();

    @Test
    public void scan() throws Exception {
        final Set<String> allClasses = new FastClasspathScanner().scan().getNamesOfAllClasses();
        assertTrue(allClasses.contains(Cls.class.getName()));
        assertTrue(allClasses.contains(FastClasspathScanner.class.getName()));
        assertTrue(allClasses.contains(FastClasspathScannerTest.class.getName()));
        assertTrue(!allClasses.contains(String.class.getName()));
        assertTrue(allClasses.contains(Object.class.getName()));
        assertTrue(allClasses.contains(Blacklisted.class.getName()));
    }

    @Test
    public void scanWithWhitelist() throws Exception {
        final Set<String> allClasses = new FastClasspathScanner(WHITELIST_PACKAGE).scan().getNamesOfAllClasses();
        assertTrue(allClasses.contains(Cls.class.getName()));
        assertTrue(!allClasses.contains(FastClasspathScanner.class.getName()));
        assertTrue(!allClasses.contains(FastClasspathScannerTest.class.getName()));
        assertTrue(!allClasses.contains(String.class.getName()));
        assertTrue(allClasses.contains(Object.class.getName()));
        assertTrue(allClasses.contains(Blacklisted.class.getName()));
    }

    @Test
    public void scanWithWhitelistAndBlacklist() throws Exception {
        final Set<String> allClasses = new FastClasspathScanner(WHITELIST_PACKAGE, "-" + BLACKLIST_PACKAGE).scan()
                .getNamesOfAllClasses();
        assertTrue(allClasses.contains(Cls.class.getName()));
        assertTrue(!allClasses.contains(FastClasspathScanner.class.getName()));
        assertTrue(!allClasses.contains(FastClasspathScannerTest.class.getName()));
        assertTrue(!allClasses.contains(String.class.getName()));
        assertTrue(allClasses.contains(Object.class.getName()));
        assertTrue(!allClasses.contains(Blacklisted.class.getName()));
    }

    @Test
    public void scanSubAndSuperclass() throws Exception {
        final List<Class<? extends Cls>> collector = new ArrayList<>();
        final FastClasspathScanner scanner = new FastClasspathScanner(WHITELIST_PACKAGE).matchSubclassesOf(Cls.class,
                new SubclassMatchProcessor<Cls>() {
                    @Override public void processMatch(final Class<? extends Cls> matchingClass) {
                        collector.add(matchingClass);
                    }
                }).scan();
        assertTrue(!collector.contains(Cls.class));
        assertTrue(collector.contains(ClsSub.class));
        assertTrue(collector.contains(ClsSubSub.class));
        assertTrue(!scanner.getNamesOfSubclassesOf(Cls.class).contains(Cls.class.getName()));
        assertTrue(scanner.getNamesOfSubclassesOf(Cls.class).contains(ClsSub.class.getName()));
        assertTrue(scanner.getNamesOfSubclassesOf(Cls.class).contains(ClsSubSub.class.getName()));
        assertTrue(!scanner.getNamesOfSuperclassesOf(ClsSubSub.class).contains(ClsSubSub.class.getName()));
        assertTrue(scanner.getNamesOfSuperclassesOf(ClsSubSub.class).contains(ClsSub.class.getName()));
        assertTrue(scanner.getNamesOfSuperclassesOf(ClsSubSub.class).contains(Cls.class.getName()));
    }

    @Test
    public void scanSubAndSuperinterface() throws Exception {
        final List<Class<? extends Iface>> collector = new ArrayList<>();
        final FastClasspathScanner scanner = new FastClasspathScanner(WHITELIST_PACKAGE).matchSubinterfacesOf(
                Iface.class, new SubinterfaceMatchProcessor<Iface>() {
                    @Override public void processMatch(final Class<? extends Iface> matchingInterface) {
                        collector.add(matchingInterface);
                    }
                }).scan();
        assertTrue(!collector.contains(Iface.class));
        assertTrue(collector.contains(IfaceSub.class));
        assertTrue(collector.contains(IfaceSubSub.class));
        assertTrue(!scanner.getNamesOfSubinterfacesOf(Iface.class).contains(Iface.class.getName()));
        assertTrue(scanner.getNamesOfSubinterfacesOf(Iface.class).contains(IfaceSub.class.getName()));
        assertTrue(scanner.getNamesOfSubinterfacesOf(Iface.class).contains(IfaceSubSub.class.getName()));
        assertTrue(!scanner.getNamesOfSuperinterfacesOf(IfaceSubSub.class).contains(IfaceSubSub.class.getName()));
        assertTrue(scanner.getNamesOfSuperinterfacesOf(IfaceSubSub.class).contains(IfaceSub.class.getName()));
        assertTrue(scanner.getNamesOfSuperinterfacesOf(IfaceSubSub.class).contains(Iface.class.getName()));
    }

    @Test
    public void scanTransitiveImplements() throws Exception {
        final FastClasspathScanner scanner = new FastClasspathScanner(WHITELIST_PACKAGE).scan();
        assertTrue(!scanner.getNamesOfClassesImplementing(Iface.class).contains(Iface.class.getName()));
        assertTrue(!scanner.getNamesOfClassesImplementing(IfaceSubSub.class).contains(Cls.class.getName()));

        assertTrue(scanner.getNamesOfClassesImplementing(Iface.class).contains(Impl1.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSub.class).contains(Impl1.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSubSub.class).contains(Impl1.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(Iface.class).contains(Impl1Sub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSub.class).contains(Impl1Sub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSubSub.class).contains(Impl1Sub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(Iface.class).contains(Impl1SubSub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSub.class).contains(Impl1SubSub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSubSub.class).contains(Impl1SubSub.class.getName()));

        assertTrue(scanner.getNamesOfClassesImplementing(Iface.class).contains(Impl2.class.getName()));
        assertTrue(!scanner.getNamesOfClassesImplementing(IfaceSub.class).contains(Impl2.class.getName()));
        assertTrue(!scanner.getNamesOfClassesImplementing(IfaceSubSub.class).contains(Impl2.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(Iface.class).contains(Impl2Sub.class.getName()));
        assertTrue(!scanner.getNamesOfClassesImplementing(IfaceSub.class).contains(Impl2Sub.class.getName()));
        assertTrue(!scanner.getNamesOfClassesImplementing(IfaceSubSub.class).contains(Impl2Sub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(Iface.class).contains(Impl2SubSub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSub.class).contains(Impl2SubSub.class.getName()));
        assertTrue(scanner.getNamesOfClassesImplementing(IfaceSubSub.class).contains(Impl2SubSub.class.getName()));
    }

    private boolean readFileContents = false;

    @Test
    public void scanFilePattern() throws Exception {
        new FastClasspathScanner().matchFilenamePattern(
                "[[^/]*/]*file-content-test\\.txt",
                new FileMatchProcessor() {
                    @Override public void processMatch(final String absolutePath, final String relativePath, final InputStream inputStream) throws IOException {
                        readFileContents = "File contents".equals(new BufferedReader(new InputStreamReader(inputStream))
                                .readLine());
                    }
                }).scan();
        assertTrue(readFileContents);
    }

    private int readStaticFieldCount = 0;

    @Test
    public void scanStaticFinalFieldName() throws Exception {
        HashSet<String> fieldNames = new HashSet<String>();
        for (String fieldName : new String[] { "stringField", "intField", "boolField", "charField", "integerField",
                "booleanField" }) {
            fieldNames.add(StaticField.class.getName() + "." + fieldName);
        }
        new FastClasspathScanner(WHITELIST_PACKAGE).matchStaticFinalFieldNames(fieldNames,
                new StaticFinalFieldMatchProcessor() {
                    @Override public void processMatch(final String className, final String fieldName, final Object fieldConstantValue) {
                        switch (fieldName) {
                        case "stringField":
                            assertTrue("Static field contents".equals(fieldConstantValue));
                            break;
                        case "intField":
                            assertTrue(new Integer(3).equals(fieldConstantValue));
                            break;
                        case "boolField":
                            assertTrue(new Boolean(true).equals(fieldConstantValue));
                            break;
                        case "charField":
                            assertTrue(new Character('y').equals(fieldConstantValue));
                            break;
                        case "integerField":
                        case "booleanField":
                            throw new RuntimeException("Non-constant field should not be matched");
                        default:
                            throw new RuntimeException("Unknown field");
                        }
                        readStaticFieldCount++;
                    }
                }).scan();
        assertTrue(readStaticFieldCount == 4);
    }

    @Test
    public void hashContents() throws Exception {
        HashMap<String, String> classNameToClassfileHash = new FastClasspathScanner(WHITELIST_PACKAGE)
                .enableHashingClassfileContents().scan().getClassNameToClassfileHash();
        String hash = classNameToClassfileHash.get(Cls.class.getName());
        assertTrue(hash != null && hash.length() == 32);
        //        for (Entry<String, String> ent : classNameToClassfileHash.entrySet()) {
        //            String k = ent.getKey();
        //            String v = ent.getValue();
        //            System.out.println(k + " -> " + v);
        //        }
    }
}
