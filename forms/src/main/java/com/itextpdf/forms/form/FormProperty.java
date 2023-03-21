/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2023 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.forms.form;

/**
 * Set of constants that will be used as keys to get and set properties.
 */
public final class FormProperty {

    /** The Constant PROPERTY_START. */
    private static final int PROPERTY_START = (1 << 21);

    /** The Constant FORM_FIELD_FLATTEN for form related properties. */
    public static final int FORM_FIELD_FLATTEN = PROPERTY_START + 1;

    /** The Constant FORM_FIELD_SIZE. */
    public static final int FORM_FIELD_SIZE = PROPERTY_START + 2;

    /** The Constant FORM_FIELD_VALUE. */
    public static final int FORM_FIELD_VALUE = PROPERTY_START + 3;

    /** The Constant FORM_FIELD_PASSWORD_FLAG. */
    public static final int FORM_FIELD_PASSWORD_FLAG = PROPERTY_START + 4;

    /** The Constant FORM_FIELD_COLS. */
    public static final int FORM_FIELD_COLS = PROPERTY_START + 5;

    /** The Constant FORM_FIELD_ROWS. */
    public static final int FORM_FIELD_ROWS = PROPERTY_START + 6;

    /** The Constant FORM_FIELD_CHECKED. */
    public static final int FORM_FIELD_CHECKED = PROPERTY_START + 7;

    /** The Constant FORM_FIELD_MULTIPLE. */
    public static final int FORM_FIELD_MULTIPLE = PROPERTY_START + 8;

    /** The Constant FORM_FIELD_SELECTED. */
    public static final int FORM_FIELD_SELECTED = PROPERTY_START + 9;

    /** The Constant FORM_FIELD_LABEL. */
    public static final int FORM_FIELD_LABEL = PROPERTY_START + 10;

    /** The Constant FORM_ACCESSIBILITY_LANGUAGE. */
    public static final int FORM_ACCESSIBILITY_LANGUAGE = PROPERTY_START + 11;

    /** The Constant FORM_FIELD_RADIO_GROUP_NAME. */
    public static final int FORM_FIELD_RADIO_GROUP_NAME = PROPERTY_START + 12;

    /** The Constant FORM_FIELD_RADIO_BORDER_CIRCLE. */
    public static final int FORM_FIELD_RADIO_BORDER_CIRCLE = PROPERTY_START + 13;

    /**
     * The Constant FORM_CHECKBOX_TYPE.
     */
    public static final int FORM_CHECKBOX_TYPE = PROPERTY_START + 14;

    /** The Constant FORM_CONFORMANCE_LEVEL. */
    public static final int FORM_CONFORMANCE_LEVEL = PROPERTY_START + 15;
    
    private FormProperty() {
        // Empty constructor.
    }
}
