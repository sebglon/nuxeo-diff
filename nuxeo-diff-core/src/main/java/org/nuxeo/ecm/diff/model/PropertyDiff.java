/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;

/**
 * Representation of a property (field) diff.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class PropertyDiff implements Serializable {

    private static final long serialVersionUID = -8458912212588012911L;

    protected String propertyType;

    protected DifferenceType differenceType = DifferenceType.different;

    /**
     * Checks if is simple type.
     *
     * @return true, if is simple type
     */
    public boolean isSimpleType() {
        return PropertyType.isSimpleType(propertyType);
    }

    /**
     * Checks if is list type.
     *
     * @return true, if is list type
     */
    public boolean isListType() {
        return PropertyType.isListType(propertyType);
    }

    /**
     * Checks if is scalar list type.
     *
     * @return true, if is scalar list type
     */
    public boolean isScalarListType() {
        return PropertyType.isScalarListType(propertyType);
    }

    /**
     * Checks if is complex list type.
     *
     * @return true, if is complex list type
     */
    public boolean isComplexListType() {
        return PropertyType.isComplexListType(propertyType);
    }

    /**
     * Checks if is content list type.
     *
     * @return true, if is content list type
     */
    public boolean isContentListType(String propertyType) {
        return PropertyType.isContentListType(propertyType);
    }

    /**
     * Checks if is complex type.
     *
     * @return true, if is complex type
     */
    public boolean isComplexType() {
        return PropertyType.isComplexType(propertyType);
    }

    /**
     * Checks if is content type.
     *
     * @return true, if is content type
     */
    public boolean isContentType() {
        return PropertyType.isContentType(propertyType);
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public void setDifferenceType(DifferenceType differenceType) {
        this.differenceType = differenceType;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof PropertyDiff)) {
            return false;
        }
        String otherPropertyType = ((PropertyDiff) other).getPropertyType();
        DifferenceType otherDifferenceType = ((PropertyDiff) other).getDifferenceType();
        return differenceType.equals(otherDifferenceType)
                && ((propertyType == null && otherPropertyType == null) || (propertyType != null
                        && otherPropertyType != null && propertyType.equals(otherPropertyType)));
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(" (");
        sb.append(propertyType);
        sb.append(", ");
        sb.append(differenceType.name());
        sb.append(")");
        return sb.toString();
    }
}
