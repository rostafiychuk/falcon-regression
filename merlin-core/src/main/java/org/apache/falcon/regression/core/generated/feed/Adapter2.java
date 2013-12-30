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
// Generated on: 2013.11.21 at 10:47:07 AM GMT+05:30 
//


package org.apache.falcon.regression.core.generated.feed;

import org.apache.falcon.regression.core.generated.dependencies.Frequency;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class Adapter2
        extends XmlAdapter<String, Frequency> {


    public Frequency unmarshal(String value) {
        return (Frequency.fromString(value));
    }

    public String marshal(Frequency value) {
        return (Frequency.toString(value));
    }

}