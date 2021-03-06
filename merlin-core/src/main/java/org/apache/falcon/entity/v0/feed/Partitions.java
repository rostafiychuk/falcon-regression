//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.05.28 at 10:55:57 AM PDT 
//


package org.apache.falcon.entity.v0.feed;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 A list of partition, which is the logical partition of a feed and this
 *                 is maintained in Hcatalog registry.
 *             
 * 
 * <p>Java class for partitions complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="partitions">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="partition" type="{uri:falcon:feed:0.1}partition" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "partitions", propOrder = {
    "partitions"
})
public class Partitions {

    @XmlElement(name = "partition")
    protected List<Partition> partitions;

    /**
     * Gets the value of the partitions property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the partitions property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPartitions().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Partition }
     * 
     * 
     */
    public List<Partition> getPartitions() {
        if (partitions == null) {
            partitions = new ArrayList<Partition>();
        }
        return this.partitions;
    }

}
