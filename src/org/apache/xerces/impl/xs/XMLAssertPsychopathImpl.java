/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.xerces.impl.xs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.PSVIAttrNSImpl;
import org.apache.xerces.dom.PSVIDocumentImpl;
import org.apache.xerces.dom.PSVIElementNSImpl;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.xs.assertion.XMLAssertAdapter;
import org.apache.xerces.impl.xs.assertion.XSAssertImpl;
import org.apache.xerces.impl.xs.util.XSTypeHelper;
import org.apache.xerces.util.XMLChar;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.eclipse.wst.xml.xpath2.processor.DynamicContext;
import org.eclipse.wst.xml.xpath2.processor.DynamicError;
import org.eclipse.wst.xml.xpath2.processor.PsychoPathTypeHelper;
import org.eclipse.wst.xml.xpath2.processor.ResultSequence;
import org.eclipse.wst.xml.xpath2.processor.ResultSequenceFactory;
import org.eclipse.wst.xml.xpath2.processor.StaticError;
import org.eclipse.wst.xml.xpath2.processor.ast.XPath;
import org.eclipse.wst.xml.xpath2.processor.internal.types.AnyAtomicType;
import org.eclipse.wst.xml.xpath2.processor.internal.types.AnyType;
import org.eclipse.wst.xml.xpath2.processor.internal.types.SchemaTypeValueFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class implementing an XPath interface for XML Schema 1.1 "assertions" evaluation.
 * This class interfaces with the "Eclipse/PsychoPath XPath 2.0" engine for XPath 
 * expression evaluations for XML Schema assertions.
 * 
 * We construct here Xerces PSVI enabled DOM trees (on which PsychoPath XPath 2.0 
 * engine operates) from XNI event calls, for typed XDM instance support. XML Schema 
 * assertions are evaluated on these XPath tree instances in a bottom up fashion.
 * 
 * @xerces.internal
 * 
 * @author Mukul Gandhi, IBM
 * @author Ken Cai, IBM
 * 
 * @version $Id$
 */
public class XMLAssertPsychopathImpl extends XMLAssertAdapter {

    // class variable declarations
    private DynamicContext fDynamicContext;
    private XSModel fSchema = null;
    private AbstractPsychoPathImpl fAbstrPsychopathImpl = null;

    // the DOM root of assertions tree
    private Document fAssertDocument = null;

    // an element to track construction of assertion DOM tree. Value of this object changes as per
    // the XNI document events.
    private Element fCurrentAssertDomNode = null;

    // a stack holding the DOM roots for assertions evaluation
    private Stack fAssertRootStack = null;

    // a stack parallel to 'assertRootStack' storing all assertions for a single XDM tree.
    private Stack fAssertListStack = null;

    // XMLSchemaValidator reference. set from the XMLSchemaValidator object itself.
    private XMLSchemaValidator fValidator = null;
    
    // parameters to pass to PsychoPath engine (like, the XML namespace bindings).
    private Map fAssertParams = null;
    
    // a placeholder definition used for assertions error messages.
    private final String ERROR_PLACEHOLDER_REGEX = "\\{\\$value\\}";

    
    /*
     * Class constructor.
     */
    public XMLAssertPsychopathImpl(Map assertParams) {        
        // initializing the class variables
        this.fAssertDocument = new PSVIDocumentImpl();        
        this.fAssertRootStack = new Stack();
        this.fAssertListStack = new Stack();
        this.fAssertParams = assertParams;
    }
    

    /*
     * Initialize the PsychoPath XPath processor.
     */
    private void initXPathProcessor() throws Exception {        
        fValidator = (XMLSchemaValidator) getProperty("http://apache.org/xml/properties/assert/validator");        
        fAbstrPsychopathImpl = new AbstractPsychoPathImpl();
        fDynamicContext = fAbstrPsychopathImpl.initDynamicContext(fSchema, fAssertDocument, fAssertParams);        
    } // initXPathProcessor
    

    /*
     * (non-Javadoc)
     * @see org.apache.xerces.xni.parser.XMLAssertAdapter#startElement
     *      (org.apache.xerces.xni.QName, org.apache.xerces.xni.XMLAttributes, 
     *       org.apache.xerces.xni.Augmentations)
     */
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) {
        
        if (fCurrentAssertDomNode == null) {
           fCurrentAssertDomNode = new PSVIElementNSImpl((CoreDocumentImpl) fAssertDocument, element.uri, element.rawname);
           fAssertDocument.appendChild(fCurrentAssertDomNode);
        } else {
            Element elem = new PSVIElementNSImpl((CoreDocumentImpl) fAssertDocument, element.uri, element.rawname);
            fCurrentAssertDomNode.appendChild(elem);
            fCurrentAssertDomNode = elem;
        }
        
        // add attribute nodes to DOM element node
        final int attributesLength = attributes.getLength();
        for (int attIndex = 0; attIndex < attributesLength; attIndex++) {
            String attrUri = attributes.getURI(attIndex);
            String attQName = attributes.getQName(attIndex);
            String attrLocalName = attributes.getLocalName(attIndex);
            String attValue = attributes.getValue(attIndex);             
            
            PSVIAttrNSImpl attrNode = new PSVIAttrNSImpl((PSVIDocumentImpl)fAssertDocument, attrUri, attQName, attrLocalName);
            attrNode.setNodeValue(attValue);
            
            // set PSVI information for the attribute
            Augmentations attrAugs = attributes.getAugmentations(attIndex);
            AttributePSVImpl attrPSVI = (AttributePSVImpl) attrAugs.getItem(Constants.ATTRIBUTE_PSVI);
            attrNode.setPSVI(attrPSVI);
            
            fCurrentAssertDomNode.setAttributeNode(attrNode);
        }

        List assertionList = (List) augs.getItem("ASSERT");
        
        // if we have assertions applicable to this element, store the element reference and the assertions on it,
        // on the runtime stacks.
        if (assertionList != null) {
            fAssertRootStack.push(fCurrentAssertDomNode);
            fAssertListStack.push(assertionList);
        }
        
    } // startElement
    

    /*
     * (non-Javadoc)
     * @see org.apache.xerces.xni.parser.XMLAssertAdapter#endElement(org.apache.xerces.xni.QName, 
     *      org.apache.xerces.xni.Augmentations)
     */
    public void endElement(QName element, Augmentations augs) throws Exception {
        
        if (fCurrentAssertDomNode != null) {            
            // set PSVI information on the element
            ElementPSVI elemPSVI = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI);
            ((PSVIElementNSImpl) fCurrentAssertDomNode).setPSVI(elemPSVI);
            
            // handling default values of elements (adding them as 'text' node in the assertion tree)
            XSElementDecl elemDecl = (XSElementDecl) elemPSVI.getElementDeclaration();
            if (elemDecl != null && elemDecl.fDefault != null && fCurrentAssertDomNode.getChildNodes().getLength() == 0) {
                String normalizedDefaultValue = elemDecl.fDefault.normalizedValue;
                fCurrentAssertDomNode.appendChild(fAssertDocument.createTextNode(normalizedDefaultValue));
            }               
            
            if (!fAssertRootStack.empty() && (fCurrentAssertDomNode == fAssertRootStack.peek())) {               
                 // get XSModel instance                
                 fSchema =  ((PSVIElementNSImpl) fCurrentAssertDomNode).getSchemaInformation();
                 
                 // pop the stack to go one level up
                 fAssertRootStack.pop();
                 // get assertions and go one level up on the stack
                 List assertions = (List) fAssertListStack.pop(); 
                 Boolean isAssertProcessingNeeded = (Boolean) augs.getItem("ASSERT_PROC_NEEDED_FOR_UNION");
                 if (isAssertProcessingNeeded.booleanValue()) {
                    // if in a pre-processing step in XMLSchemaValidator, a member type of union in XML Schema namespace successfully 
                    // validated an atomic value, we don't process assertions on such union types. For all other use-cases (non union
                    // simple types or complex types, applicable assertions would always be processed.
                    processAllAssertionsOnElement(element, assertions, elemPSVI);
                 }
            }

            if (fCurrentAssertDomNode.getParentNode() instanceof Element) {
                fCurrentAssertDomNode = (Element) fCurrentAssertDomNode.getParentNode();
            }
        }
        
    } // endElement
    

    /*
     * Method to evaluate all of XML schema 1.1 assertions for an element tree. This is the root method
     * which evaluates all XML schema assertions, in a single XML instance validation episode.
     */
    private void processAllAssertionsOnElement(QName element, List assertions, ElementPSVI elemPSVI) throws Exception {
         
         // initialize the XPath engine
         initXPathProcessor();
         
         // determine "string value" of XPath2 context variable, $value
         String value = getStringValueOf$Value(elemPSVI);

         // evaluate assertions
         if (assertions instanceof XSObjectList) {
            // assertions from a "complex type" definition             
            evaluateAssertionsFromAComplexType(element, assertions, value);            
         }
         else if (assertions instanceof Vector) {            
            // assertions from a "simple type" definition
            evaluateAssertionsFromASimpleType(element, assertions, value, elemPSVI);            
         }
         
    } // processAllAssertionsOnElement


    /*
     * Determine "string value" of XPath2 context variable $value.
     */
    private String getStringValueOf$Value(ElementPSVI pElemPSVI) throws DOMException {
        
        int textChildCount = 0;        
        // we are only interested in text & element nodes. Store count of them in this variable.
        int effectiveChildCount = 0;
        
        // there could be adjacent text nodes in a DOM tree. merge them to get the value.
        NodeList childList = fCurrentAssertDomNode.getChildNodes();
        StringBuffer textValueContents = new StringBuffer();
        final int childListLength = childList.getLength();
        for (int childNodeIndex = 0; childNodeIndex < childListLength; childNodeIndex++) {
            Node node = childList.item(childNodeIndex);
            short nodeType = node.getNodeType();
            if (nodeType == Node.TEXT_NODE) {
                textChildCount++;
                effectiveChildCount++;
                textValueContents.append(node.getNodeValue());
            }
            else if (nodeType == Node.ELEMENT_NODE) {
                effectiveChildCount++;  
            }
        }
        
        String value = "";
        
        if (textChildCount == effectiveChildCount) {
            // the DOM tree we are inspecting has simple content. therefore we can find the desired string value. 
            XSElementDeclaration elemDecl = pElemPSVI.getElementDeclaration();
            if ((elemDecl.getTypeDefinition()).derivedFrom(SchemaSymbols.URI_SCHEMAFORSCHEMA,
                                                           SchemaSymbols.ATTVAL_STRING,
                                                           XSConstants.DERIVATION_RESTRICTION)) {
                // if element's schema type is derived by restriction from xs:string, white-space normalization is
                // not needed for the string value for context variable $value.
                value = textValueContents.toString();  
            }
            else {
                // white-space normalization is needed for the string value of $value in case of derivation from
                // non xs:string atomic types.
                value = XMLChar.trim(textValueContents.toString());
            }    
        }
        else {
            // the DOM tree we are inspecting, has 'mixed/element only' content.
            value = null; 
        }
        
        return value;
        
    } // getStringValueOf$Value


    /*
     * Evaluate assertions on a "complex type".
     */
    private void evaluateAssertionsFromAComplexType(QName element, List assertions, String value) 
                                                    throws Exception {
        if (value != null) {
            // complex type with simple content
            setTypedValueFor$value(value, null, null);
        } else {
            // complex type with complex content. set xpath context variable $value to an empty sequence.
            fDynamicContext.set_variable(new org.eclipse.wst.xml.xpath2.processor.internal.types.QName(
                                             "value"), getXPath2ResultSequence(new ArrayList()));
        }
        
        XSObjectList assertList = (XSObjectList) assertions;
        XSObjectList attrMemberTypes = null;
        final int assertListSize = assertList.size();
        for (int i = 0; i < assertListSize; i++) {
            XSAssertImpl assertImpl = (XSAssertImpl) assertList.get(i);               
            boolean xpathContextExists = false;
            if (assertImpl.getType() == XSConstants.ASSERTION) {
                // not an assertion facet
                xpathContextExists = true;   
            }
            // check if this is an assertion, from an attribute
            if (assertImpl.getAttrName() != null) {
                value = assertImpl.getAttrValue();
                XSSimpleTypeDefinition attrType = (XSSimpleTypeDefinition) assertImpl.getTypeDefinition();                
                attrMemberTypes = attrType.getMemberTypes();
                final boolean isTypeDerivedFromUnion = ((XSSimpleType) attrType.getBaseType()).getVariety() == XSSimpleType.VARIETY_UNION;                
                if (assertImpl.getVariety() == XSSimpleTypeDefinition.VARIETY_LIST) {
                    // this assertion belongs to a type, that is an item type of a "simpleType -> list".
                    // tokenize the list value by the longest sequence of white-spaces.
                    StringTokenizer values = new StringTokenizer(value, " \n\t\r");
                    
                    // evaluate assertion on all of list items
                    while (values.hasMoreTokens()) {
                        String itemValue = values.nextToken();
                        setValueOf$valueForAListItem(itemValue, attrType);                        
                        AssertionError assertError = evaluateAssertion(element, assertImpl, itemValue, xpathContextExists, true);
                        if (assertError != null) {
                            reportAssertionsError(assertError);    
                        }
                    }
                }
                else if (assertImpl.getVariety() == XSSimpleTypeDefinition.VARIETY_ATOMIC) {
                    // evaluating assertions for "simpleType -> restriction" (not derived by union)
                    setTypedValueFor$value(value, null, attrType);
                    AssertionError assertError = evaluateAssertion(element, assertImpl, value, xpathContextExists, false);
                    if (assertError != null) {
                        reportAssertionsError(assertError);    
                    }
                }
                else if (attrMemberTypes != null && attrMemberTypes.getLength() > 0 && !isTypeDerivedFromUnion) {
                   // evaluate assertions for "simpleType -> union"
                   boolean isValidationFailedForUnion = isValidationFailedForUnion(attrMemberTypes, element, value, false);
                   // only 1 error message is reported for assertion failures on "simpleType -> union", since it is hard 
                   // (perhaps impossible?) to determine statically that what all assertions can cause validation failure, 
                   // when participating in an XML schema union.
                   if (isValidationFailedForUnion) {                        
                        fValidator.reportSchemaError("cvc-assertion.union.3.13.4.1", new Object[] { element.rawname, value } );   
                   } 
                }
                else if (isTypeDerivedFromUnion) {
                    // although Xerces XSModel treats this case as a simpleType with variety union, but from assertions perspective
                    // this is treated like a "simpleType -> restriction" case (which also appears to be syntactically true in every case?).
                    // REVISIT...
                    setValueOf$ValueForSTVarietyUnion(value, attrMemberTypes);
                    AssertionError assertError = evaluateAssertion(element, assertImpl, value, false, false);
                    if (assertError != null) {
                        reportAssertionsError(assertError);    
                    }
                }
            }
            else {
                AssertionError assertError = evaluateAssertion(element, assertImpl, value, xpathContextExists, false);
                if (assertError != null) {
                    reportAssertionsError(assertError);    
                }  
            }
        }
        
    } // evaluateAssertionsFromAComplexType
    
    
    /*
     * Evaluate assertions on a "simple type".
     */
    private void evaluateAssertionsFromASimpleType(QName element, List assertions, String value, ElementPSVI elemPSVI) throws Exception {
        
        // find the itemType and memberTypes of xs:list and xs:union respectively (only one of these will be applicable)        
        XSSimpleTypeDefinition itemType = null;                    
        XSObjectList memberTypes = null;                     
        
        if (elemPSVI.getTypeDefinition().getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            XSSimpleTypeDefinition simpleTypeDefn = (XSSimpleTypeDefinition) elemPSVI.getTypeDefinition();
            itemType = simpleTypeDefn.getItemType();
            if (itemType == null) {
                memberTypes = simpleTypeDefn.getMemberTypes();    
            }
        }
        
        final boolean isTypeDerivedFromUnion = ((XSSimpleType) elemPSVI.getTypeDefinition().getBaseType()).getVariety() == XSSimpleType.VARIETY_UNION;
        
        // process assertions from a simple type definition           
        Vector assertList = (Vector) assertions;
        final int assertListLength = assertList.size();
        
        for (int assertIdx = 0; assertIdx < assertListLength; assertIdx++) {
            XSAssertImpl assertImpl = (XSAssertImpl) assertList.get(assertIdx);
            if (itemType != null) {
               // evaluating assertions for "simpleType -> list". tokenize the list value by the longest sequence of white-spaces.
               StringTokenizer values = new StringTokenizer(value, " \n\t\r");
               
               // evaluate assertion on all of list items
               while (values.hasMoreTokens()) {
                   String itemValue = values.nextToken();
                   setValueOf$valueForAListItem(itemValue, itemType);
                   AssertionError assertError = evaluateAssertion(element, assertImpl, itemValue, false, true);
                   if (assertError != null) {
                       reportAssertionsError(assertError);    
                   }
               }
            }
            else if (memberTypes != null && memberTypes.getLength() == 0) {
                // evaluating assertions for "simpleType -> restriction"
                setTypedValueFor$value(value, null, null);
                AssertionError assertError = evaluateAssertion(element, assertImpl, value, false, false);
                if (assertError != null) {
                    reportAssertionsError(assertError);    
                }    
            }
            else if (memberTypes != null && memberTypes.getLength() > 0 && !isTypeDerivedFromUnion) {
                  // evaluate assertions for "simpleType -> union"
                  boolean isValidationFailedForUnion = isValidationFailedForUnion(memberTypes, element, value, false);
                 // only 1 error message is reported for assertion failures on "simpleType -> union", since it is hard 
                 // (perhaps impossible?) to determine statically that what all assertions can cause validation failure, 
                 // when participating in an XML schema union.
                 if (isValidationFailedForUnion) {
                      fValidator.reportSchemaError("cvc-assertion.union.3.13.4.1", new Object[] { element.rawname, value } );   
                 }
            }
            else if (isTypeDerivedFromUnion) {
                // although Xerces XSModel treats this case as a simpleType with variety union, but from assertions perspective,
                // this is treated like a "simpleType -> restriction" case (which also appears to be syntactically in every case?).
                // REVISIT...
                setValueOf$ValueForSTVarietyUnion(value, memberTypes);
                AssertionError assertError = evaluateAssertion(element, assertImpl, value, false, false);
                if (assertError != null) {
                    reportAssertionsError(assertError);    
                }
            }
        }
        
    } // evaluateAssertionsFromASimpleType
    
    
    /*
     * Set a typed value of XPath2 context variable $value if an atomic value on which assertion is been evaluated, is validated by a
     * simpleType with variety union. 
     */
    private void setValueOf$ValueForSTVarietyUnion(String value, XSObjectList memberTypes) {
        
        // check member types of union in order to find which member type can successfully validate the string value,
        // and set value of XPath2 context variable $value accordingly.
        for (int memTypeIdx = 0; memTypeIdx < memberTypes.getLength(); memTypeIdx++) {
            XSSimpleType simpleTypeDv = (XSSimpleType) memberTypes.item(memTypeIdx);
            if (XSTypeHelper.isValueValidForASimpleType(value, simpleTypeDv)) {
               short xsdTypecode = getXercesXSDTypeCodeFor$Value(simpleTypeDv);
               setValueOf$ValueForSTVarietyAtomic(value, xsdTypecode);
               break;
            }            
        }
        
    } // setValueOf$ValueForSTVarietyUnion
    
    
    /*
     * Set a typed value of XPath2 context variable $value if an atomic value on which assertion is been evaluated,
     * is an item of a schema component xs:list.
     */
    private void setValueOf$valueForAListItem(String value, XSSimpleTypeDefinition simpType) throws Exception {
        
        XSObjectList memberTypes = simpType.getMemberTypes();
        if (memberTypes.getLength() > 0) {
            // the list item's type has variety 'union'.
            XSSimpleTypeDefinition actualListItemType = getActualListItemTypeForVarietyUnion(memberTypes, value);
            // set a schema 'typed value' to variable $value
            setTypedValueFor$value(value, actualListItemType, null);
        } 
        else {
            // the list item's type has variety 'atomic'.
            setTypedValueFor$value(value, simpType, null); 
        }

    } // setValueOf$valueForAListItem
    
    
    /*
     * Determine if an validation must fail due to assertions evaluation for "simpleType -> union" member types.
     */
    private boolean isValidationFailedForUnion(XSObjectList memberTypes, QName element, String value, boolean isAttribute) {
        
        boolean isValidationFailedForUnion = true;
        final int memberTypesLength = memberTypes.getLength();
        
        for (int memberTypeIdx = 0; memberTypeIdx < memberTypesLength; memberTypeIdx++) {
            XSSimpleTypeDefinition memType = (XSSimpleTypeDefinition) memberTypes.item(memberTypeIdx);
            
            // check for assertions on types in an non-schema namespace
            if (!SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(memType.getNamespace()) && simpleTypeHasAsserts(memType)) {
                XSObjectList memberTypeFacets = memType.getMultiValueFacets();
                final int memberTypeFacetsLength = memberTypeFacets.getLength();
                for (int memberTypeFacetIdx = 0; memberTypeFacetIdx < memberTypeFacetsLength; memberTypeFacetIdx++) {
                    XSMultiValueFacet facet = (XSMultiValueFacet) memberTypeFacets.item(memberTypeFacetIdx);
                    if (facet.getFacetKind() == XSSimpleTypeDefinition.FACET_ASSERT) {
                        Vector assertFacets = facet.getAsserts();
                        int assertsSucceeded = 0;
                        for (Iterator iter = assertFacets.iterator(); iter.hasNext(); ) {
                            XSAssertImpl assertImpl = (XSAssertImpl) iter.next();
                            try {
                               setTypedValueFor$value(value, memType, null);
                               AssertionError assertError = evaluateAssertion(element, assertImpl, value, false, false);
                               if (assertError == null) {
                                   assertsSucceeded++;  
                               }
                            }
                            catch(Exception ex) {
                               // An exception may occur if for example, a typed value cannot be constructed by PsychoPath
                               // engine for a given "string value" (say a value '5' was attempted to be formed as a typed
                               // value xs:date).
                               // it's useful to report warning ... TO DO
                            }
                        }
                        if (assertsSucceeded == assertFacets.size()) {
                            // all assertions on a 'union' member type have evaluated to 'true', therefore validation with
                            // union has succeeded wrt assertions.
                            return false;  
                        }
                    }
                }
            }
        }
        
        return isValidationFailedForUnion;
        
    } // isValidationFailedForUnion
    
    
    /*
     * Check if a simple type has assertion facets.
     */
    private boolean simpleTypeHasAsserts(XSSimpleTypeDefinition simpleType) {
        
        boolean simpleTypehasAsserts = false;
        
        XSObjectList simpleTypeFacets = simpleType.getMultiValueFacets();
        final int simpleTypeFacetsLength = simpleTypeFacets.getLength();
        for (int facetIdx = 0; facetIdx < simpleTypeFacetsLength; facetIdx++) {
            XSMultiValueFacet facet = (XSMultiValueFacet) simpleTypeFacets.item(facetIdx);
            if (facet.getFacetKind() == XSSimpleTypeDefinition.FACET_ASSERT && facet.getAsserts().size() > 0) {
                simpleTypehasAsserts = true;
                break;
            }
        }
        
        return simpleTypehasAsserts;

    } // simpleTypeHasAsserts
    

    /*
     * (non-Javadoc)
     * @see org.apache.xerces.xni.parser.XMLAssertAdapter#characters
     *      (org.apache.xerces.xni.XMLString)
     */
    public void characters(XMLString text) {        
        // add a child text node to the assertions, DOM tree
        if (fCurrentAssertDomNode != null) {
            fCurrentAssertDomNode.appendChild(fAssertDocument.createTextNode(new String(text.ch, text.offset, text.length)));
        }        
    }
    

    /*
     * Method to evaluate an assertion object.
     */
    private AssertionError evaluateAssertion(QName element, XSAssertImpl assertImpl, String value,
                                             boolean xPathContextExists, boolean isList) {
        
        AssertionError assertionError = null;
        
        try {  
            XPath xp = assertImpl.getCompiledXPath();
            
            boolean result;            
            if ((value == null) ||
                (xPathContextExists == true)) {
                result = fAbstrPsychopathImpl.evaluateXPathExpr(xp, assertImpl.getXPathDefaultNamespace(),
                                                                     fCurrentAssertDomNode);  
            } 
            else {
                // XPath context is "undefined"
                result = fAbstrPsychopathImpl.evaluateXPathExpr(xp, assertImpl.getXPathDefaultNamespace(),
                                                                     null); 
            }
            
            if (!result) {
               // assertion evaluation is false
               assertionError = new AssertionError("cvc-assertion.3.13.4.1", element, assertImpl, value, isList); 
            }
        }
        catch (DynamicError ex) {
            if (ex.code().equals("XPDY0002")) {
               // ref: http://www.w3.org/TR/xpath20/#eval_context
               assertionError = new AssertionError("cvc-assertion.4.3.15.3", element, assertImpl, value, isList);
            }
            else {
               assertionError = new AssertionError("cvc-assertion.3.13.4.1", element, assertImpl, value, isList);
            }
        }
        catch (StaticError ex) {
            assertionError = new AssertionError("cvc-assertion.3.13.4.1", element, assertImpl, value, isList);
        }
        catch(Exception ex) {
            assertionError = new AssertionError("cvc-assertion.3.13.4.1", element, assertImpl, value, isList);   
        }
        
        return assertionError;
        
    } // evaluateAssertion
    
    
    /*
     * Find a "schema typed value" (of kind xs:anyAtomicType*) to assign to XPath2 context variable $value.
     */
    private void setTypedValueFor$value(String value, XSSimpleTypeDefinition listOrUnionType, XSTypeDefinition attrType) throws Exception {
        
        // dummy short code initializer
        short xsdTypecode = -100;
        
        if (listOrUnionType != null) {
            xsdTypecode = getXercesXSDTypeCodeFor$Value(listOrUnionType);
            setValueOf$ValueForSTVarietyAtomic(value, xsdTypecode);
        }
        else {
           if (attrType != null) {
              // is value of an attribute
              xsdTypecode = getXercesXSDTypeCodeFor$Value(attrType);
              setValueOf$ValueForSTVarietyAtomic(value, xsdTypecode);
           }
           else {
              // is "simple type" value of an element
              PSVIElementNSImpl currentAssertPSVINode = (PSVIElementNSImpl) fCurrentAssertDomNode;
              XSTypeDefinition typeDef = currentAssertPSVINode.getTypeDefinition();
              if (typeDef instanceof XSComplexTypeDefinition && ((XSComplexTypeDefinition) typeDef).getSimpleType() != null) {
                  setValueOf$ValueForCTWithSimpleContent(value, (XSComplexTypeDefinition) typeDef);
              }
              else if (typeDef instanceof XSComplexTypeDefinition && 
                      ((XSComplexTypeDefinition) typeDef).getSimpleType() == null) {
                  // assign an empty XPath2 sequence to xpath context variable
                  // $value.
                  fDynamicContext.set_variable(new org.eclipse.wst.xml.xpath2.processor.internal.types.QName(
                                                   "value"), getXPath2ResultSequence(new ArrayList())); 
              }
              else {
                  xsdTypecode = getXercesXSDTypeCodeFor$Value(typeDef);
                  setValueOf$ValueForSTVarietyAtomic(value, xsdTypecode);
              }
           }
        }
        
    } // setTypedValueFor$value
    
    
    /*
     * Set value of XPath2 context variable $value, when variety of it's schema type is "simpleType -> atomic".  
     */
    private void setValueOf$ValueForSTVarietyAtomic(String value, short xsdTypecode) {
        AnyType psychoPathType = SchemaTypeValueFactory.newSchemaTypeValue(xsdTypecode, value);
        fDynamicContext.set_variable(new org.eclipse.wst.xml.xpath2.processor.internal.types.QName(
                                         "value"), (AnyAtomicType) psychoPathType);
    } // setValueOf$ValueForSTVarietyAtomic


    /*
     * Set value of XPath2 context variable $value, if element has a complex type with simple content.
     */
    private void setValueOf$ValueForCTWithSimpleContent(String value, XSComplexTypeDefinition typeDef) {
        
        XSComplexTypeDefinition cmplxTypeDef = (XSComplexTypeDefinition)typeDef;
        XSSimpleTypeDefinition complexTypeSimplContentType = cmplxTypeDef.getSimpleType();
        if (complexTypeSimplContentType.getVariety() == XSSimpleTypeDefinition.VARIETY_LIST) {
            // simple content type has variety xs:list
            XSSimpleTypeDefinition listItemType = complexTypeSimplContentType.getItemType();
            // split the "string value" of list contents, into non white-space tokens.
            StringTokenizer values = new StringTokenizer(value, " \n\t\r");
            
            // construct a list of atomic XDM items, to assign to XPath2 context variable $value.
            List xdmItemList = new ArrayList();
            final XSObjectList memberTypes = listItemType.getMemberTypes();
            if (memberTypes.getLength() > 0) {
               // itemType of xs:list has variety 'union'. here list items may have different types, which are determined below.
               while (values.hasMoreTokens()) {
                   String itemValue = values.nextToken();
                   XSSimpleTypeDefinition listItemTypeForUnion = getActualListItemTypeForVarietyUnion(memberTypes, itemValue);
                   xdmItemList.add(SchemaTypeValueFactory.newSchemaTypeValue(listItemTypeForUnion.getBuiltInKind(), itemValue));
               }                                  
            }
            else {
               // every list item has a same type (the itemType of xs:list).
               while (values.hasMoreTokens()) {
                   String itemValue = values.nextToken();
                   xdmItemList.add(SchemaTypeValueFactory.newSchemaTypeValue(listItemType.getBuiltInKind(), itemValue)); 
               }                                  
            }

            // assign an XPath2 sequence to xpath context variable $value
            fDynamicContext.set_variable(new org.eclipse.wst.xml.xpath2.processor.internal.types.QName(
                                           "value"), getXPath2ResultSequence(xdmItemList));
        }
        else if (complexTypeSimplContentType.getVariety() == XSSimpleTypeDefinition.VARIETY_UNION) {
            // simple content type has variety xs:union
            XSSimpleTypeDefinition simpleContentTypeForUnion = getActualListItemTypeForVarietyUnion(complexTypeSimplContentType.getMemberTypes(), value);
            fDynamicContext.set_variable(new org.eclipse.wst.xml.xpath2.processor.internal.types.QName("value"), 
                                         SchemaTypeValueFactory.newSchemaTypeValue(simpleContentTypeForUnion.getBuiltInKind(), value));
        }
        else {
            // simple content type has variety atomic
            setValueOf$ValueForSTVarietyAtomic(value, getXercesXSDTypeCodeFor$Value(cmplxTypeDef.getSimpleType()));
        }
          
    } // setValueOf$ValueForCTWithSimpleContent
    
    
    /*
       Find the built-in Xerces schema 'type code' for XPath2 variable, $value. This function recursively
       searches the XML schema type hierarchy navigating up the base types, to find the needed built-in type.
    */
    private short getXercesXSDTypeCodeFor$Value(XSTypeDefinition elementType) {
            
      if (Constants.NS_XMLSCHEMA.equals(elementType.getNamespace())) {
         short typeCode = -100; // dummy initializer
         
         boolean isxsd11Type = false;
         
         // the below 'if else' clauses are written to process few special cases handling few of schema types,
         // within PsychoPath XPath engine.
         final String elementTypeName = elementType.getName();
         if ("dayTimeDuration".equals(elementTypeName)) {
             typeCode = PsychoPathTypeHelper.DAYTIMEDURATION_DT;
             isxsd11Type = true;
         }
         else if ("yearMonthDuration".equals(elementTypeName)) {
             typeCode = PsychoPathTypeHelper.YEARMONTHDURATION_DT;
             isxsd11Type = true;
         }
         
         return (isxsd11Type) ? typeCode : ((XSSimpleTypeDefinition) elementType).getBuiltInKind();    
      }
      else {
         return getXercesXSDTypeCodeFor$Value(elementType.getBaseType()); 
      }
      
    } // getXercesXSDTypeCodeFor$Value
    
    
    /*
     * Construct an PsychoPath XPath2 "result sequence", given a list of XDM items as input.
     */
    private ResultSequence getXPath2ResultSequence(List xdmItems) {
        
        ResultSequence xpath2Seq = ResultSequenceFactory.create_new();
        
        for (Iterator iter = xdmItems.iterator(); iter.hasNext(); ) {
            xpath2Seq.add((AnyType) iter.next()); 
        }
        
        return xpath2Seq;
        
    } // getXPath2ResultSequence
    
    
    /*
     * Method to report assertions error messages.
     */
    private void reportAssertionsError(AssertionError assertError) {
        
        String key = assertError.getErrorCode();
        QName element = assertError.getElement();
        XSAssertImpl assertImpl = assertError.getAssertion();
        boolean isList = assertError.isList();
        String value = assertError.getValue();
        
        XSTypeDefinition typeDef = assertImpl.getTypeDefinition();        
        String typeString = "";
        
        if (typeDef != null) {
            typeString = (typeDef.getName() != null) ? typeDef.getName() : "#anonymous";   
        }
        else {
            typeString = "#anonymous"; 
        }
        
        String elemErrorAnnotation = element.rawname;
        if (assertImpl.getAttrName() != null) {
            elemErrorAnnotation = element.rawname + " (attribute => " + assertImpl.getAttrName()+ ")";    
        }                
        
        String listAssertErrMessage = "";        
        if (isList) {
           listAssertErrMessage =  "Assertion failed for an xs:list member value '" + assertError.getValue() + "'.";
        }
            
        String message = assertImpl.getMessage();
        if (message != null) {
           // substitute all placeholder macro instances of "{$value}" with atomic value stored in variable "value".
           if (value != null && !"".equals(value)) {
              message = message.replaceAll(ERROR_PLACEHOLDER_REGEX, value);
           }
           
           if (!message.endsWith(".")) {
              message = message + ".";    
           }
           if (key.equals("cvc-assertion.4.3.15.3")) {
              message = "Assertion failed (undefined context) for schema type '" + typeString + "'. " + message;   
           }
           else {
              message = "Assertion failed for schema type '" + typeString + "'. " + message; 
           }           
           fValidator.reportSchemaError("cvc-assertion.failure", new Object[] { message, listAssertErrMessage } );    
        }
        else {
           fValidator.reportSchemaError(key, new Object[] { elemErrorAnnotation, assertImpl.getTest().getXPath().toString(),
                                                            typeString, listAssertErrMessage} );
        }
        
    } // reportAssertionsError
    
    
    /*
     * Find the actual schema type of "list item" instance, if the "item type" of list has variety union. 
     */
    private XSSimpleTypeDefinition getActualListItemTypeForVarietyUnion(XSObjectList memberTypes, String value) {

        XSSimpleTypeDefinition simpleTypeDefn = null;
        
        // iterate the member types of union in order, to find that which schema type can successfully validate an
        // atomic value first.
        final int memberTypesLength = memberTypes.getLength();
        for (int memTypeIdx = 0; memTypeIdx < memberTypesLength; memTypeIdx++) {
           XSSimpleType memSimpleType = (XSSimpleType) memberTypes.item(memTypeIdx);
           if (XSTypeHelper.isValueValidForASimpleType(value, memSimpleType)) {
              // no more memberTypes need to be checked
              simpleTypeDefn = memSimpleType; 
              break; 
           }
        }
        
        return simpleTypeDefn;
        
    } // getActualListItemTypeForVarietyUnion
    
    
    /*
     * Class to store "assertion evaluation" error details.
     */
    final class AssertionError {
        
        // instance variables        
        private final String errorCode;
        private final QName element;
        private final XSAssertImpl assertImpl;
        private final String value;
        // does this error concerns "simpleType -> list"
        private final boolean isList;
        
        // class constructor
        public AssertionError(String errorCode, QName element, XSAssertImpl assertImpl,
                              String value, boolean isList) {
           this.errorCode = errorCode;
           this.element = element;
           this.assertImpl = assertImpl;
           this.value = value;
           this.isList = isList;
        }
        
        public String getErrorCode() {
           return errorCode;    
        }
        
        public QName getElement() {
           return element;       
        }
        
        public XSAssertImpl getAssertion() {
           return assertImpl;    
        }
        
        public String getValue() {
           return value; 
        }
        
        public boolean isList() {
           return isList;    
        }
        
    } // class AssertionError
    
} // class XMLAssertPsychopathImpl
