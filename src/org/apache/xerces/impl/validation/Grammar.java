/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999,2000 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.xerces.impl.validation;

import org.apache.xerces.xni.QName;
import org.apache.xerces.impl.validation.XMLAttributeDecl;
import org.apache.xerces.impl.validation.XMLNotationDecl;
import org.apache.xerces.impl.validation.XMLEntityDecl;
import org.apache.xerces.impl.validation.XMLSimpleType;


/**
 * @author Stubs generated by DesignDoc on Wed Jun 07 11:58:44 PDT 2000
 * @author Stubs generated by DesignDoc on Mon Sep 11 11:10:57 PDT 2000
 *
 * @version $Id$
 */
public class Grammar {

    //
    // Constants
    //

    private static final int CHUNK_SHIFT = 8; // 2^8 = 256
    private static final int CHUNK_SIZE = (1 << CHUNK_SHIFT);
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;
    private static final int INITIAL_CHUNK_COUNT = (1 << (10 - CHUNK_SHIFT)); // 2^10 = 1k

    private static final short LIST_FLAG = 0x80;
    private static final short LIST_MASK = ~LIST_FLAG;
    
    //
    // Data
    //

    /** fTargetNamespace */
    protected String fTargetNamespace;

    // element decl tables

    private int fElementDeclCount = 0;
    private String fElementDeclName[][] = new String[INITIAL_CHUNK_COUNT][];
    private short fElementDeclType[][] = new short[INITIAL_CHUNK_COUNT][];
    private DatatypeValidator fElementDeclDatatypeValidator[][] = new DatatypeValidator[INITIAL_CHUNK_COUNT][];
    private ContentModelValidator fElementDeclContentModelValidator[][] = new ContentModelValidator[INITIAL_CHUNK_COUNT][];
    private int fElementDeclFirstAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];
    private int fElementDeclLastAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];
    private String fElementDeclDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private short   fElementDeclDefaultType[][] = new short[INITIAL_CHUNK_COUNT][];

    // attribute decl tables

    private int fAttributeDeclCount = 0 ;
    private QName fAttributeDeclName[][] = new QName[INITIAL_CHUNK_COUNT][];
    private short   fAttributeDeclType[][] = new short[INITIAL_CHUNK_COUNT][];
    private String[]   fAttributeDeclEnumeration[][] = new String[INITIAL_CHUNK_COUNT][][];
    private short   fAttributeDeclDefaultType[][] = new short[INITIAL_CHUNK_COUNT][];
    private DatatypeValidator fAttributeDeclDatatypeValidator[][] = new DatatypeValidator[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private int fAttributeDeclNextAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // scope mapping tables

    //private Hash2intTable fElementNameAndScopeToElementDeclIndexMapping = new Hash2intTable();

    //
    // Constructors
    //

    /**
     * 
     */
    public Grammar() {
    }

    //
    // Methods
    //

    /**
     * getTargetNamespace
     * 
     * @return 
     */
    public String getTargetNamespace() {
        return fTargetNamespace;
    } // getTargetNamespace

    /**
     * getFirstElementDeclIndex
     * 
     * @return 
     */
    public int getFirstElementDeclIndex() {
        return fElementDeclCount > 0 ? fElementDeclCount : -1;
    } // getFirstElementDeclIndex

    /**
     * getNextElementDeclIndex
     * 
     * @param elementDeclIndex 
     * 
     * @return 
     */
    public int getNextElementDeclIndex(int elementDeclIndex) {
        return elementDeclIndex < fElementDeclCount - 1 
             ? elementDeclIndex + 1 : -1;
    } // getNextElementDeclIndex

    /**
     * getElementDeclIndex
     * 
     * @param elementDeclName 
     * @param scope 
     * 
     * @return 
     */
    public int getElementDeclIndex(String elementDeclName, int scope) {
        throw new RuntimeException("implement Grammar#getElementDeclIndex(String,int):int");
    } // getElementDeclIndex

    /**
     * getElementDecl
     * 
     * @param elementDeclIndex 
     * @param elementDecl The values of this structure are set by this call.
     * 
     * @return 
     */
    public boolean getElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return false;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        //elementDecl.name.clear();
        elementDecl.name.prefix = null;
        elementDecl.name.rawname = null;

        elementDecl.name.localpart          = fElementDeclName[chunk][index];               
        elementDecl.name.uri                = fTargetNamespace; 

        if (fElementDeclType[chunk][index] == -1) {
            elementDecl.type                    = -1;
            elementDecl.simpleType.list = false;
        }
        else {
            elementDecl.type                    = (short) (fElementDeclType[chunk][index] & LIST_MASK);
            elementDecl.simpleType.list = (fElementDeclType[chunk][index] & LIST_FLAG) != 0;
        }

        elementDecl.contentModelValidator = fElementDeclContentModelValidator[chunk][index];

        elementDecl.simpleType.datatypeValidator = fElementDeclDatatypeValidator[chunk][index];       
        elementDecl.simpleType.defaultType = fElementDeclDefaultType[chunk][index];
        elementDecl.simpleType.defaultValue = fElementDeclDefaultValue[chunk][index];

        return true;
    } // getElementDecl

    /**
     * getFirstAttributeDeclIndex
     * 
     * @param elementDeclIndex 
     * 
     * @return 
     */
    public int getFirstAttributeDeclIndex(int elementDeclIndex) {
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        return  fElementDeclFirstAttributeDeclIndex[chunk][index];
    } // getFirstAttributeDeclIndex

    /**
     * getNextAttributeDeclIndex
     * 
     * @param attributeDeclIndex 
     * 
     * @return 
     */
    public int getNextAttributeDeclIndex(int attributeDeclIndex) {
        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex &  CHUNK_MASK;

        return fAttributeDeclNextAttributeDeclIndex[chunk][index];
    } // getNextAttributeDeclIndex

    /**
     * getAttributeDeclIndex
     * 
     * @param elementDeclIndex 
     * @param attributeDeclName 
     * 
     * @return 
     */
    public int getAttributeDeclIndex(int elementDeclIndex, String attributeDeclName) {
        throw new RuntimeException("implement Grammar#getAttributeDeclIndex(int,String):int");
    } // getAttributeDeclIndex

    /**
     * getAttributeDecl
     * 
     * @param attributeDeclIndex 
     * @param attributeDecl The values of this structure are set by this call.
     * 
     * @return 
     */
    public boolean getAttributeDecl(int attributeDeclIndex, XMLAttributeDecl attributeDecl) {
        if (attributeDeclIndex < 0 || attributeDeclIndex >= fAttributeDeclCount) {
            return false;
        }
        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex & CHUNK_MASK;

        //attributeDecl.name.setValues(fAttributeDeclName[chunk][index]);
        QName temp = fAttributeDeclName[chunk][index];
        attributeDecl.name.prefix = temp.prefix;
        attributeDecl.name.localpart =  temp.localpart;
        attributeDecl.name.rawname = temp.rawname;
        attributeDecl.name.uri = temp.uri;


        short attributeType;
        boolean isList;

        if (fAttributeDeclType[chunk][index] == -1) {
            
            attributeType = -1;
            isList = false;
        }
        else {
            attributeType = (short) (fAttributeDeclType[chunk][index] & LIST_MASK);
            isList = (fAttributeDeclType[chunk][index] & LIST_FLAG) != 0;
        }
        attributeDecl.simpleType.setValues(attributeType,"",fAttributeDeclEnumeration[chunk][index],
                                           isList, fAttributeDeclDefaultType[chunk][index],
                                           fAttributeDeclDefaultValue[chunk][index], 
                                           fAttributeDeclDatatypeValidator[chunk][index]);
        return true;
    } // getAttributeDecl

    /**
     * getFirstEntityDeclIndex
     * 
     * @return 
     */
    public int getFirstEntityDeclIndex() {
        throw new RuntimeException("implement Grammar#getFirstEntityDeclIndex():int");
    } // getFirstEntityDeclIndex

    /**
     * getNextEntityDeclIndex
     * 
     * @param elementDeclIndex 
     * 
     * @return 
     */
    public int getNextEntityDeclIndex(int elementDeclIndex) {
        throw new RuntimeException("implement Grammar#getNextEntityDeclIndex(int):int");
    } // getNextEntityDeclIndex

    /**
     * getEntityDeclIndex
     * 
     * @param entityDeclName 
     * 
     * @return 
     */
    public int getEntityDeclIndex(String entityDeclName) {
        throw new RuntimeException("implement Grammar#getEntityDeclIndex(String):int");
    } // getEntityDeclIndex

    /**
     * getEntityDecl
     * 
     * @param entityDeclIndex 
     * @param entityDecl 
     * 
     * @return 
     */
    public boolean getEntityDecl(int entityDeclIndex, XMLEntityDecl entityDecl) {
        throw new RuntimeException("implement Grammar#getEntityDecl(int,XMLEntityDecl):boolean");
    } // getEntityDecl

    /**
     * getFirstNotationDeclIndex
     * 
     * @return 
     */
    public int getFirstNotationDeclIndex() {
        throw new RuntimeException("implement Grammar#getFirstNotationDeclIndex():int");
    } // getFirstNotationDeclIndex

    /**
     * getNextNotationDeclIndex
     * 
     * @param elementDeclIndex 
     * 
     * @return 
     */
    public int getNextNotationDeclIndex(int elementDeclIndex) {
        throw new RuntimeException("implement Grammar#getNextNotationDeclIndex(int):int");
    } // getNextNotationDeclIndex

    /**
     * getNotationDeclIndex
     * 
     * @param notationDeclName 
     * 
     * @return 
     */
    public int getNotationDeclIndex(String notationDeclName) {
        throw new RuntimeException("implement Grammar#getNotationDeclIndex(String):int");
    } // getNotationDeclIndex

    /**
     * getNotationDecl
     * 
     * @param notationDeclIndex 
     * @param notationDecl 
     * 
     * @return 
     */
    public boolean getNotationDecl(int notationDeclIndex, XMLNotationDecl notationDecl) {
        throw new RuntimeException("implement Grammar#getNotationDecl(int,XMLNotationDecl):boolean");
    } // getNotationDecl


     //
    // Protected methods
    //
    

    protected int createElementDecl() {
       int chunk = fElementDeclCount >> CHUNK_SHIFT;
       int index = fElementDeclCount & CHUNK_MASK;
       ensureElementDeclCapacity(chunk);
       fElementDeclName[chunk][index]                    = null; 
       fElementDeclType[chunk][index]                    = -1;  
       fElementDeclDatatypeValidator[chunk][index]       = null;
       fElementDeclContentModelValidator[chunk][index]   = null;
       fElementDeclFirstAttributeDeclIndex[chunk][index] = -1;
       fElementDeclLastAttributeDeclIndex[chunk][index]  = -1;
       fElementDeclDefaultValue[chunk][index]            = null;
       fElementDeclDefaultType[chunk][index]             = -1;
       return fElementDeclCount++;
    }

    protected void setElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return;
        }
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        fElementDeclName[chunk][index]              = elementDecl.name.rawname;
        
        fElementDeclType[chunk][index]              = elementDecl.type; 
        fElementDeclDatatypeValidator[chunk][index] =
                                        elementDecl.simpleType.datatypeValidator;
        fElementDeclContentModelValidator[chunk][index] =
                                        elementDecl.contentModelValidator;
        if (elementDecl.simpleType.list  == true ) {
            fElementDeclType[chunk][index] |= LIST_FLAG;
        }
        
    }




    protected void putElementNameMapping(QName name, int scope,
                                         int elementDeclIndex) {
    }

    protected void setFirstAttributeDeclIndex(int elementDeclIndex, int newFirstAttrIndex){
        
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return;
        }
    
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        fElementDeclFirstAttributeDeclIndex[chunk][index] = newFirstAttrIndex;
    }


    protected int createAttributeDecl() {
        int chunk = fAttributeDeclCount >> CHUNK_SHIFT;
        int index = fAttributeDeclCount & CHUNK_MASK;

        ensureAttributeDeclCapacity(chunk);
        fAttributeDeclName[chunk][index]                    = new QName();
        fAttributeDeclType[chunk][index]                    = -1;
        fAttributeDeclDatatypeValidator[chunk][index]       = null;
        fAttributeDeclEnumeration[chunk][index] = null;
        fAttributeDeclDefaultType[chunk][index] = XMLSimpleType.DEFAULT_TYPE_IMPLIED;
        fAttributeDeclDefaultValue[chunk][index]            = null;
        fAttributeDeclNextAttributeDeclIndex[chunk][index]  = -1;
        return fAttributeDeclCount++;
    }


    protected void setAttributeDecl(int elementDeclIndex, int attributeDeclIndex, XMLAttributeDecl attributeDecl) {


    }

    protected void setTargetNameSpace( String targetNamespace ){
        fTargetNamespace = targetNamespace;
    }

    // debugging

    public void printElements(  ) {
        int elementDeclIndex = 0;
        XMLElementDecl elementDecl = new XMLElementDecl();
        while (getElementDecl(elementDeclIndex++, elementDecl)) {
            System.out.println("element decl: "+elementDecl.name+
                               ", "+ elementDecl.name.rawname+
                               ", "+ elementDecl.contentModelValidator.toString());
            
        }
    }

    public void printAttributes(int elementDeclIndex) {
        int attributeDeclIndex = getFirstAttributeDeclIndex(elementDeclIndex);
        System.out.print(elementDeclIndex);
        System.out.print(" [");
        while (attributeDeclIndex != -1) {
            System.out.print(' ');
            System.out.print(attributeDeclIndex);
            printAttribute(attributeDeclIndex);
            attributeDeclIndex = getNextAttributeDeclIndex(attributeDeclIndex);
            if (attributeDeclIndex != -1) {
                System.out.print(",");
            }
        }
        System.out.println(" ]");
    }


     //
    // Private methods
    //

    // debugging

    private void printAttribute(int attributeDeclIndex) {
        XMLAttributeDecl attributeDecl = new XMLAttributeDecl();
        if (getAttributeDecl(attributeDeclIndex, attributeDecl)) {
            System.out.print(" { ");
            System.out.print(attributeDecl.name.localpart);
            System.out.print(" }");
        }
    }

    // content models
    //

    //private void printSyntaxTree(CMNode cmn){
    //}

    
   // private int countLeaves(int contentSpecIndex) {
   //     return countLeaves(contentSpecIndex, new XMLContentSpec());
    //}
    
    //private int countLeaves(int contentSpecIndex, XMLContentSpec contentSpec) {
    //}
    // ensure capacity


    private boolean ensureElementDeclCapacity(int chunk) {
        try {
            return fElementDeclName[chunk][0] == null;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fElementDeclName = resize(fElementDeclName, fElementDeclName.length * 2);
            fElementDeclType = resize(fElementDeclType, fElementDeclType.length * 2);
            fElementDeclDatatypeValidator = resize(fElementDeclDatatypeValidator, fElementDeclDatatypeValidator.length * 2);
            fElementDeclContentModelValidator = resize(fElementDeclContentModelValidator, fElementDeclContentModelValidator.length * 2);
            fElementDeclFirstAttributeDeclIndex = resize(fElementDeclFirstAttributeDeclIndex, fElementDeclFirstAttributeDeclIndex.length * 2);
            fElementDeclLastAttributeDeclIndex = resize(fElementDeclLastAttributeDeclIndex, fElementDeclLastAttributeDeclIndex.length * 2);
            fElementDeclDefaultValue = resize( fElementDeclDefaultValue, fElementDeclDefaultValue.length * 2 ); 
            fElementDeclDefaultType  = resize( fElementDeclDefaultType, fElementDeclDefaultType.length *2 );
        } catch (NullPointerException ex) {
            // ignore
        }
        fElementDeclName[chunk] = new String[CHUNK_SIZE];
        fElementDeclType[chunk] = new short[CHUNK_SIZE];
        fElementDeclDatatypeValidator[chunk] = new DatatypeValidator[CHUNK_SIZE];
        fElementDeclContentModelValidator[chunk] = new ContentModelValidator[CHUNK_SIZE];
        fElementDeclFirstAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclLastAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclDefaultValue[chunk] = new String[CHUNK_SIZE]; 
        fElementDeclDefaultType[chunk]  = new short[CHUNK_SIZE]; 
        return true;
    }



    private boolean ensureAttributeDeclCapacity(int chunk) {
        try {
            return fAttributeDeclName[chunk][0] == null;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fAttributeDeclName = resize(fAttributeDeclName, fAttributeDeclName.length * 2);
            fAttributeDeclType = resize(fAttributeDeclType, fAttributeDeclType.length * 2);
            fAttributeDeclEnumeration = resize(fAttributeDeclEnumeration, fAttributeDeclEnumeration.length * 2);
            fAttributeDeclDefaultType = resize(fAttributeDeclDefaultType, fAttributeDeclDefaultType.length * 2);
            fAttributeDeclDatatypeValidator = resize(fAttributeDeclDatatypeValidator, fAttributeDeclDatatypeValidator.length * 2);
            fAttributeDeclDefaultValue = resize(fAttributeDeclDefaultValue, fAttributeDeclDefaultValue.length * 2);
            fAttributeDeclNextAttributeDeclIndex = resize(fAttributeDeclNextAttributeDeclIndex, fAttributeDeclNextAttributeDeclIndex.length * 2);
        } catch (NullPointerException ex) {
            // ignore
        }
        fAttributeDeclName[chunk] = new QName[CHUNK_SIZE];
        fAttributeDeclType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclEnumeration[chunk] = new String[CHUNK_SIZE][];
        fAttributeDeclDefaultType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclDatatypeValidator[chunk] = new DatatypeValidator[CHUNK_SIZE];
        fAttributeDeclDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNextAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        return true;
    }

    // resize initial chunk

    private short[][] resize(short array[][], int newsize) {
       short newarray[][] = new short[newsize][];
       System.arraycopy(array, 0, newarray, 0, array.length);
       return newarray;
   }


    private int[][] resize(int array[][], int newsize) {
        int newarray[][] = new int[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private DatatypeValidator[][] resize(DatatypeValidator array[][], int newsize) {
        DatatypeValidator newarray[][] = new DatatypeValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private ContentModelValidator[][] resize(ContentModelValidator array[][], int newsize) {
       ContentModelValidator newarray[][] = new ContentModelValidator[newsize][];
       System.arraycopy(array, 0, newarray, 0, array.length);
       return newarray;
    }


    private QName[][] resize(QName array[][], int newsize) {
        QName newarray[][] = new QName[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private String[][] resize(String array[][], int newsize) {
        String newarray[][] = new String[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }
      
    private String[][][] resize(String array[][][], int newsize) {
      String newarray[][][] = new String[newsize] [][];
      System.arraycopy(array, 0, newarray, 0, array.length);
      return newarray;
  }


} // class Grammar
