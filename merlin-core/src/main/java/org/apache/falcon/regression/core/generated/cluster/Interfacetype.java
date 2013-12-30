/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation,
// vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.05.29 at 03:46:54 PM GMT+05:30 
//


package org.apache.falcon.regression.core.generated.cluster;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for interfacetype.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;simpleType name="interfacetype">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="readonly"/>
 *     &lt;enumeration value="write"/>
 *     &lt;enumeration value="execute"/>
 *     &lt;enumeration value="workflow"/>
 *     &lt;enumeration value="messaging"/>
 *     &lt;enumeration value="registry"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "interfacetype")
@XmlEnum
public enum Interfacetype {

    @XmlEnumValue("readonly")
    READONLY("readonly"),
    @XmlEnumValue("write")
    WRITE("write"),
    @XmlEnumValue("execute")
    EXECUTE("execute"),
    @XmlEnumValue("workflow")
    WORKFLOW("workflow"),
    @XmlEnumValue("messaging")
    MESSAGING("messaging"),
    @XmlEnumValue("registry")
    REGISTRY("registry");
    private final String value;

    Interfacetype(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Interfacetype fromValue(String v) {
        for (Interfacetype c : Interfacetype.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}