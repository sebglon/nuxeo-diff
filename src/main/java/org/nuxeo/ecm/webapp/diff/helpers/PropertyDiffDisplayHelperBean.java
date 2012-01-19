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
 *     ataillefer
 */
package org.nuxeo.ecm.webapp.diff.helpers;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.diff.diff_match_patch;
import org.nuxeo.ecm.platform.diff.helpers.ComplexPropertyHelper;
import org.nuxeo.ecm.platform.diff.model.PropertyType;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Helps handling property diff display.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@Name("propertyDiffDisplayHelper")
@Scope(APPLICATION)
public class PropertyDiffDisplayHelperBean implements Serializable {

    private static final long serialVersionUID = -7995476720750309928L;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected diff_match_patch dmp;

    @Create
    public void init() {
        dmp = new diff_match_patch();
    }

    public static Serializable getSimplePropertyValue(DocumentModel doc,
            String schemaName, String fieldName) throws ClientException {

        return ComplexPropertyHelper.getSimplePropertyValue(doc, schemaName,
                fieldName);
    }

    public List<String> getComplexItemNames(String schemaName, String fieldName)
            throws Exception {

        return ComplexPropertyHelper.getComplexItemNames(schemaName, fieldName);
    }

    public Serializable getComplexItemValue(DocumentModel doc,
            String schemaName, String fieldName, String complexItemName)
            throws ClientException {

        return ComplexPropertyHelper.getComplexItemValue(doc, schemaName,
                fieldName, complexItemName);
    }

    public List<Integer> getListItemIndexes(ListPropertyDiff listPropertyDiff)
            throws ClientException {

        return ComplexPropertyHelper.getListItemIndexes(listPropertyDiff);
    }

    public Serializable getListItemValue(DocumentModel doc, String schemaName,
            String fieldName, int itemIndex) throws ClientException {

        return ComplexPropertyHelper.getListItemValue(doc, schemaName,
                fieldName, itemIndex);
    }

    public List<String> getComplexListItemNames(String schemaName,
            String fieldName) throws Exception {

        return ComplexPropertyHelper.getComplexListItemNames(schemaName,
                fieldName);
    }

    public Serializable getComplexListItemValue(DocumentModel doc,
            String schemaName, String fieldName, int itemIndex,
            String complexItemName) throws ClientException {

        return ComplexPropertyHelper.getComplexListItemValue(doc, schemaName,
                fieldName, itemIndex, complexItemName);
    }

    public boolean isSimpleProperty(Serializable prop) {

        return ComplexPropertyHelper.isSimpleProperty(prop);
    }

    public boolean isComplexProperty(Serializable prop) {

        return ComplexPropertyHelper.isComplexProperty(prop);
    }

    public boolean isListProperty(Serializable prop) {

        return ComplexPropertyHelper.isListProperty(prop);
    }

    /**
     * Gets the property display.
     * 
     * @param propertyValue the property value
     * @param propertyDiff the property diff
     * @return the property display
     */
    public String getPropertyDisplay(Serializable propertyValue,
            SimplePropertyDiff propertyDiff) {

        String propertyDisplay;

        // TODO: propertyDiff should never be null, see the 'files' schema case.
        String propertyType;
        if (propertyDiff == null) {
            propertyType = PropertyType.STRING;
        } else {
            propertyType = propertyDiff.getPropertyType();
        }

        // Boolean
        if (PropertyType.BOOLEAN.equals(propertyType)) {
            propertyDisplay = resourcesAccessor.getMessages().get(
                    "property.boolean." + propertyValue);
        }
        // Date
        else if (PropertyType.DATE.equals(propertyType)) {
            DateFormat sdf = new SimpleDateFormat("dd MMMM yyyy - hh:mm");
            if (propertyValue instanceof Calendar) {
                propertyDisplay = sdf.format(((Calendar) propertyValue).getTime());
            } else { // Date
                propertyDisplay = sdf.format(propertyValue);
            }
        }
        // Default: we consider property value is a String.
        // Works fine for PropertyType.STRING, PropertyType.INTEGER,
        // PropertyType.LONG, PropertyType.DOUBLE.
        else {
            propertyDisplay = propertyValue.toString();
            // String leftValue = propertyDiff.getLeftValue();
            // String rightValue = propertyDiff.getRightValue();
            // if (leftValue == null) {
            // leftValue = "";
            // }
            // if (rightValue == null) {
            // rightValue = "";
            // }
            // LinkedList<Diff> diffs = dmp.diff_main(leftValue, rightValue);
            // propertyDisplay = dmp.diff_prettyHtml(diffs);

        }
        // TODO: Directory!

        return propertyDisplay;
    }
}
