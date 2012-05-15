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
package org.nuxeo.ecm.diff.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffDisplayBlockImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldItemDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.PropertyDiffDisplayImpl;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.diff.service.DiffDisplayService;
import org.nuxeo.ecm.diff.web.ComplexPropertyHelper;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetReferenceImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of the {@link DiffDisplayService}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DiffDisplayServiceImpl extends DefaultComponent implements
        DiffDisplayService {

    private static final long serialVersionUID = 6608445970773402827L;

    private static final Log LOGGER = LogFactory.getLog(DiffDisplayServiceImpl.class);

    protected static final String DIFF_DISPLAY_EXTENSION_POINT = "diffDisplay";

    protected static final String DIFF_BLOCK_EXTENSION_POINT = "diffBlock";

    protected static final String DIFF_WIDGET_CATEGORY = "diff";

    protected static final String DIFF_WIDGET_LABEL_PREFIX = "label.";

    protected static final String CONTENT_DIFF_LINKS_WIDGET_NAME = "contentDiffLinks";

    protected static final String CONTENT_DIFF_LINKS_WIDGET_NAME_SUFFIX = "_contentDiffLinks";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_VALUE_SUFFIX = "value";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_STYLE_CLASS_SUFFIX = "styleClass";

    protected static final String DIFF_WIDGET_PROPERTY_DISPLAY_ALL_ITEMS = "displayAllItems";

    protected static final String DIFF_WIDGET_PROPERTY_DISPLAY_ITEM_INDEXES = "displayItemIndexes";

    // TODO: refactor name (not related to widget)
    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD = "index";

    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_TYPE = "int";

    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_LABEL = "label.list.index";

    protected static final String DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD = "value";

    /** Diff display contributions. */
    protected Map<String, List<String>> diffDisplayContribs = new HashMap<String, List<String>>();

    /** Diff block contributions. */
    protected Map<String, DiffBlockDefinition> diffBlockContribs = new HashMap<String, DiffBlockDefinition>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (DIFF_DISPLAY_EXTENSION_POINT.equals(extensionPoint)) {
            if (contribution instanceof DiffDisplayDescriptor) {
                registerDiffDisplay((DiffDisplayDescriptor) contribution);
            }
        } else if (DIFF_BLOCK_EXTENSION_POINT.equals(extensionPoint)) {
            if (contribution instanceof DiffBlockDescriptor) {
                registerDiffBlock((DiffBlockDescriptor) contribution);
            }
        }
        super.registerContribution(contribution, extensionPoint, contributor);
    }

    public Map<String, List<String>> getDiffDisplays() {
        return diffDisplayContribs;
    }

    public List<String> getDiffDisplay(String type) {
        return diffDisplayContribs.get(type);

    }

    public Map<String, DiffBlockDefinition> getDiffBlockDefinitions() {
        return diffBlockContribs;
    }

    public DiffBlockDefinition getDiffBlockDefinition(String name) {
        return diffBlockContribs.get(name);
    }

    public List<DiffDisplayBlock> getDiffDisplayBlocks(DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        String leftDocType = leftDoc.getType();
        String rightDocType = rightDoc.getType();
        if (leftDocType.equals(rightDocType)) {
            LOGGER.info(String.format(
                    "The 2 documents have the same type '%s' => looking for a diffDisplay contribution defined for this type.",
                    leftDocType));
            List<String> diffBlockRefs = getDiffDisplay(leftDocType);
            if (diffBlockRefs != null) {
                LOGGER.info(String.format(
                        "Found a diffDisplay contribution defined for the type '%s' => using it to display the diff.",
                        leftDocType));
                return getDiffDisplayBlocks(
                        getDiffBlockDefinitions(diffBlockRefs), docDiff,
                        leftDoc, rightDoc);
            } else {
                LOGGER.info(String.format(
                        "No diffDisplay contribution was defined for the type '%s' => using default diff display.",
                        leftDocType));
            }
        } else {
            LOGGER.info(String.format(
                    "The 2 documents don't have the same type ('%s'/'%s') => using default diff display.",
                    leftDocType, rightDocType));
        }
        return getDefaultDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
    }

    public List<DiffDisplayBlock> getDefaultDiffDisplayBlocks(
            DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        return getDiffDisplayBlocks(getDefaultDiffBlockDefinitions(docDiff),
                docDiff, leftDoc, rightDoc);
    }

    /**
     * Registers a diff display contrib.
     *
     * @param contribution the contribution
     */
    protected final void registerDiffDisplay(DiffDisplayDescriptor descriptor) {

        String type = descriptor.getType();
        if (!StringUtils.isEmpty(type)) {
            boolean enabled = descriptor.isEnabled();
            // Check existing diffDisplay contrib for this type
            List<String> diffDisplay = diffDisplayContribs.get(type);
            if (diffDisplay != null) {
                // If !enabled remove contrib
                if (!enabled) {
                    diffDisplayContribs.remove(type);
                }
                // Else override contrib (no merge)
                // TODO: implement merge
                else {
                    diffDisplayContribs.put(type,
                            getDiffBlockRefs(descriptor.getDiffBlocks()));
                }
            }
            // No existing diffDisplay contrib for this
            // type and enabled => add contrib
            else if (enabled) {
                diffDisplayContribs.put(type,
                        getDiffBlockRefs(descriptor.getDiffBlocks()));
            }
        }
    }

    protected final List<String> getDiffBlockRefs(
            List<DiffBlockReferenceDescriptor> diffBlocks) {

        List<String> diffBlockRefs = new ArrayList<String>();
        for (DiffBlockReferenceDescriptor diffBlockRef : diffBlocks) {
            diffBlockRefs.add(diffBlockRef.getName());
        }
        return diffBlockRefs;
    }

    protected final void registerDiffBlock(DiffBlockDescriptor descriptor) {

        String diffBlockName = descriptor.getName();
        if (!StringUtils.isEmpty(diffBlockName)) {
            List<DiffFieldDescriptor> fieldDescriptors = descriptor.getFields();
            // No field descriptors => don't take diff block into account.
            if (fieldDescriptors == null || fieldDescriptors.isEmpty()) {
                LOGGER.warn(String.format(
                        "The diffBlock contribution named '%s' has no fields, it won't be taken into account.",
                        diffBlockName));
            } else {
                List<DiffFieldDefinition> fields = new ArrayList<DiffFieldDefinition>();
                // Some field descriptors were found => use them to add the
                // described fields, taking their order into account.
                for (DiffFieldDescriptor fieldDescriptor : fieldDescriptors) {
                    String schema = fieldDescriptor.getSchema();
                    String name = fieldDescriptor.getName();
                    boolean displayContentDiffLinks = fieldDescriptor.isDisplayContentDiffLinks();
                    List<DiffFieldItemDescriptor> fieldItemDescriptors = fieldDescriptor.getItems();
                    if (!StringUtils.isEmpty(schema)
                            && !StringUtils.isEmpty(name)) {
                        List<DiffFieldItemDefinition> items = new ArrayList<DiffFieldItemDefinition>();
                        for (DiffFieldItemDescriptor fieldItemDescriptor : fieldItemDescriptors) {
                            items.add(new DiffFieldItemDefinitionImpl(
                                    fieldItemDescriptor.getName(),
                                    fieldItemDescriptor.isDisplayContentDiffLinks()));
                        }
                        fields.add(new DiffFieldDefinitionImpl(schema, name,
                                displayContentDiffLinks, items));
                    }
                }
                // TODO: implement merge
                diffBlockContribs.put(
                        diffBlockName,
                        new DiffBlockDefinitionImpl(diffBlockName,
                                descriptor.getLabel(), fields));
            }
        }
    }

    protected final List<DiffBlockDefinition> getDefaultDiffBlockDefinitions(
            DocumentDiff docDiff) {

        List<DiffBlockDefinition> diffBlockDefs = new ArrayList<DiffBlockDefinition>();

        for (String schemaName : docDiff.getSchemaNames()) {
            // TODO: enable to configure the system elements display
            if (!FieldDiffHelper.SYSTEM_ELEMENT.equals(schemaName)) {
                SchemaDiff schemaDiff = docDiff.getSchemaDiff(schemaName);
                List<DiffFieldDefinition> fieldDefs = new ArrayList<DiffFieldDefinition>();
                for (String fieldName : schemaDiff.getFieldNames()) {
                    fieldDefs.add(new DiffFieldDefinitionImpl(schemaName,
                            fieldName));
                }
                diffBlockDefs.add(new DiffBlockDefinitionImpl(schemaName, null,
                        fieldDefs));
            }
        }

        return diffBlockDefs;
    }

    protected final List<DiffBlockDefinition> getDiffBlockDefinitions(
            List<String> diffBlockRefs) {

        List<DiffBlockDefinition> diffBlockDefinitions = new ArrayList<DiffBlockDefinition>();
        for (String diffBlockRef : diffBlockRefs) {
            diffBlockDefinitions.add(getDiffBlockDefinition(diffBlockRef));
        }
        return diffBlockDefinitions;
    }

    protected final List<DiffDisplayBlock> getDiffDisplayBlocks(
            List<DiffBlockDefinition> diffBlockDefinitions,
            DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        List<DiffDisplayBlock> diffDisplayBlocks = new ArrayList<DiffDisplayBlock>();

        for (DiffBlockDefinition diffBlockDef : diffBlockDefinitions) {
            if (diffBlockDef != null) {
                DiffDisplayBlock diffDisplayBlock = getDiffDisplayBlock(
                        diffBlockDef, docDiff, leftDoc, rightDoc);
                if (!diffDisplayBlock.isEmpty()) {
                    diffDisplayBlocks.add(diffDisplayBlock);
                }
            }
        }

        return diffDisplayBlocks;
    }

    protected final DiffDisplayBlock getDiffDisplayBlock(
            DiffBlockDefinition diffBlockDefinition, DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        Map<String, Map<String, PropertyDiffDisplay>> leftValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        Map<String, Map<String, PropertyDiffDisplay>> rightValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        Map<String, Map<String, Serializable>> contentDiffValue = new HashMap<String, Map<String, Serializable>>();

        // TODO: remove contentDiff?

        List<LayoutRowDefinition> layoutRowDefinitions = new ArrayList<LayoutRowDefinition>();
        List<WidgetDefinition> widgetDefinitions = new ArrayList<WidgetDefinition>();

        List<DiffFieldDefinition> fieldDefinitions = diffBlockDefinition.getFields();
        for (DiffFieldDefinition fieldDefinition : fieldDefinitions) {

            String schemaName = fieldDefinition.getSchema();
            String fieldName = fieldDefinition.getName();
            boolean displayContentDiffLinks = fieldDefinition.isDisplayContentDiffLinks();
            List<DiffFieldItemDefinition> fieldItemDefs = fieldDefinition.getItems();

            SchemaDiff schemaDiff = docDiff.getSchemaDiff(schemaName);
            if (schemaDiff != null) {
                PropertyDiff fieldDiff = schemaDiff.getFieldDiff(fieldName);
                if (fieldDiff != null) {

                    String propertyName = getPropertyName(schemaName, fieldName);
                    List<WidgetReference> widgetReferences = new ArrayList<WidgetReference>();

                    // Set property widget definition
                    WidgetDefinition propertyWidgetDefinition = getWidgetDefinition(
                            propertyName, fieldDiff.getPropertyType(), null,
                            fieldItemDefs, false);
                    widgetDefinitions.add(propertyWidgetDefinition);
                    // Set property widget ref
                    WidgetReferenceImpl propertyWidgetRef = new WidgetReferenceImpl(
                            DIFF_WIDGET_CATEGORY, propertyName);
                    widgetReferences.add(propertyWidgetRef);

                    // Check if must display the content diff links widget
                    if (!displayContentDiffLinks) {
                        for (DiffFieldItemDefinition fieldItemDef : fieldItemDefs) {
                            if (fieldItemDef.isDisplayContentDiffLinks()) {
                                displayContentDiffLinks = true;
                                break;
                            }
                        }
                    }
                    // Set content diff links widget definition and ref if
                    // needed
                    if (displayContentDiffLinks) {
                        WidgetDefinition contentDiffLinksWidgetDefinition = getWidgetDefinition(
                                propertyName, fieldDiff.getPropertyType(),
                                null, fieldItemDefs, true);
                        widgetDefinitions.add(contentDiffLinksWidgetDefinition);
                        WidgetReferenceImpl contentDiffLinksWidgetRef = new WidgetReferenceImpl(
                                DIFF_WIDGET_CATEGORY, propertyName
                                        + CONTENT_DIFF_LINKS_WIDGET_NAME_SUFFIX);
                        widgetReferences.add(contentDiffLinksWidgetRef);
                    }

                    // Set layout row definition
                    LayoutRowDefinition layoutRowDefinition = new LayoutRowDefinitionImpl(
                            propertyName, null, widgetReferences, false, true);
                    layoutRowDefinitions.add(layoutRowDefinition);

                    // Set diff display field value
                    boolean isDisplayAllItems = isDisplayAllItems(propertyWidgetDefinition);
                    boolean isDisplayItemIndexes = isDisplayItemIndexes(propertyWidgetDefinition);

                    Serializable leftProperty = (Serializable) leftDoc.getProperty(
                            schemaName, fieldName);
                    Serializable rightProperty = (Serializable) rightDoc.getProperty(
                            schemaName, fieldName);

                    // Left diff display
                    setFieldDiffDisplay(leftProperty, fieldDiff,
                            isDisplayAllItems, isDisplayItemIndexes, leftValue,
                            schemaName, fieldName, leftDoc,
                            PropertyDiffDisplay.RED_BACKGROUND_STYLE_CLASS);

                    // Right diff display
                    setFieldDiffDisplay(rightProperty, fieldDiff,
                            isDisplayAllItems, isDisplayItemIndexes,
                            rightValue, schemaName, fieldName, rightDoc,
                            PropertyDiffDisplay.GREEN_BACKGROUND_STYLE_CLASS);

                    // TODO: manage better contentDiff if needed
                    // Content diff display
                    if (displayContentDiffLinks) {
                        Serializable contentDiffDisplay = getFieldXPaths(
                                propertyName, fieldDiff, leftProperty,
                                rightProperty, isDisplayAllItems,
                                isDisplayItemIndexes, fieldItemDefs);
                        Map<String, Serializable> contentDiffSchemaMap = contentDiffValue.get(schemaName);
                        if (contentDiffSchemaMap == null) {
                            contentDiffSchemaMap = new HashMap<String, Serializable>();
                            contentDiffValue.put(schemaName,
                                    contentDiffSchemaMap);
                        }
                        contentDiffSchemaMap.put(fieldName, contentDiffDisplay);
                    }
                }
            }
        }

        // Build layout definition
        LayoutDefinition layoutDefinition = new LayoutDefinitionImpl(
                diffBlockDefinition.getName(), null, null,
                layoutRowDefinitions, widgetDefinitions);

        // Build diff display block
        DiffDisplayBlock diffDisplayBlock = new DiffDisplayBlockImpl(
                diffBlockDefinition.getLabel(), leftValue, rightValue,
                contentDiffValue, layoutDefinition);

        return diffDisplayBlock;
    }

    protected final boolean isDisplayAllItems(WidgetDefinition wDef) {

        // Check 'displayAllItems' widget property
        return getBooleanProperty(wDef, BuiltinModes.ANY,
                DIFF_WIDGET_PROPERTY_DISPLAY_ALL_ITEMS);
    }

    protected final boolean isDisplayItemIndexes(WidgetDefinition wDef) {

        // Check 'displayItemIndexes' widget property
        return getBooleanProperty(wDef, BuiltinModes.ANY,
                DIFF_WIDGET_PROPERTY_DISPLAY_ITEM_INDEXES);
    }

    protected final boolean getBooleanProperty(WidgetDefinition wDef,
            String mode, String property) {

        Map<String, Map<String, Serializable>> props = wDef.getProperties();
        if (props != null) {
            Map<String, Serializable> modeProps = props.get(mode);
            if (modeProps != null) {
                Serializable propertyValue = modeProps.get(property);
                if (propertyValue instanceof String) {
                    return Boolean.parseBoolean((String) propertyValue);
                }
            }
        }
        return false;
    }

    /**
     * Sets the field diff display.
     *
     * @param property the property
     * @param fieldDiff the field diff
     * @param isDisplayAllItems the is display all items
     * @param isDisplayItemIndexes the is display item indexes
     * @param value the value
     * @param schemaName the schema name
     * @param fieldName the field name
     * @param doc the doc
     * @param styleClass the style class
     * @throws ClientException the client exception
     */
    protected void setFieldDiffDisplay(Serializable property,
            PropertyDiff fieldDiff, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes,
            Map<String, Map<String, PropertyDiffDisplay>> value,
            String schemaName, String fieldName, DocumentModel doc,
            String styleClass) throws ClientException {

        PropertyDiffDisplay fieldDiffDisplay = getFieldDiffDisplay(property,
                fieldDiff, isDisplayAllItems, isDisplayItemIndexes, false,
                styleClass);
        Map<String, PropertyDiffDisplay> schemaMap = value.get(schemaName);
        if (schemaMap == null) {
            schemaMap = new HashMap<String, PropertyDiffDisplay>();
            value.put(schemaName, schemaMap);
        }
        schemaMap.put(fieldName, fieldDiffDisplay);
        // TODO: better manage content (file) and note
        putFilenameDiffDisplay(schemaName, fieldName, schemaMap,
                fieldDiffDisplay.getValue());
        putMimetypeDiffDisplay(schemaName, fieldName, schemaMap, doc);
    }

    /**
     * @param schemaName
     * @param fieldName
     * @param schemaMap
     * @param fieldDiffDisplayValue
     */
    // TODO: should not be hardcoded
    protected final void putFilenameDiffDisplay(String schemaName,
            String fieldName, Map<String, PropertyDiffDisplay> schemaMap,
            Serializable fieldDiffDisplayValue) {

        if ("file".equals(schemaName) && "content".equals(fieldName)
                && !schemaMap.containsKey("filename")
                && fieldDiffDisplayValue instanceof Blob) {
            schemaMap.put("filename", new PropertyDiffDisplayImpl(
                    ((Blob) fieldDiffDisplayValue).getFilename()));
        }
    }

    // TODO: should not be hardcoded
    // => use HTML guesser?
    protected final void putMimetypeDiffDisplay(String schemaName,
            String fieldName, Map<String, PropertyDiffDisplay> schemaMap,
            DocumentModel doc) throws ClientException {

        if ("note".equals(schemaName) && "note".equals(fieldName)
                && !schemaMap.containsKey("mime_type")) {
            schemaMap.put("mime_type", new PropertyDiffDisplayImpl(
                    (Serializable) doc.getProperty("note", "mime_type")));
        }
    }

    protected final PropertyDiffDisplay getFieldDiffDisplay(
            Serializable property, PropertyDiff propertyDiff,
            boolean isDisplayAllItems, boolean isDisplayItemIndexes,
            boolean mustApplyStyleClass, String styleClass)
            throws ClientException {

        if (property == null) {
            String fieldDiffDisplayStyleClass = PropertyDiffDisplay.DEFAULT_STYLE_CLASS;
            if (mustApplyStyleClass && propertyDiff != null) {
                fieldDiffDisplayStyleClass = styleClass;
            }
            return new PropertyDiffDisplayImpl(null, fieldDiffDisplayStyleClass);
        }

        // List type
        if (isListType(property)) {
            List<Serializable> listProperty = getListProperty(property);
            return getListFieldDiffDisplay(listProperty,
                    (ListPropertyDiff) propertyDiff, isDisplayAllItems,
                    isDisplayItemIndexes, styleClass);
        }
        // Other types (scalar, complex, content)
        else {
            return getFinalFieldDiffDisplay(property, propertyDiff,
                    mustApplyStyleClass, styleClass);
        }
    }

    protected final PropertyDiffDisplay getFinalFieldDiffDisplay(
            Serializable fieldDiffDisplay, PropertyDiff propertyDiff,
            boolean mustApplyStyleClass, String styleClass)
            throws ClientException {

        String finalFieldDiffDisplayStyleClass = PropertyDiffDisplay.DEFAULT_STYLE_CLASS;
        if (mustApplyStyleClass && propertyDiff != null) {
            finalFieldDiffDisplayStyleClass = styleClass;
        }
        PropertyDiffDisplay finalFieldDiffDisplay;
        if (isComplexType(fieldDiffDisplay)) {
            ComplexPropertyDiff complexPropertyDiff = null;
            if (propertyDiff != null) {
                if (!propertyDiff.isComplexType()
                        || propertyDiff.isContentType()) {
                    throw new ClientException(
                            "'fieldDiffDisplay' is of complex type whereas 'propertyDiff' is not, this is inconsistent");
                }
                complexPropertyDiff = (ComplexPropertyDiff) propertyDiff;
            }
            Map<String, Serializable> complexFieldDiffDisplay = getComplexProperty(fieldDiffDisplay);
            for (String complexItemName : complexFieldDiffDisplay.keySet()) {
                PropertyDiff complexItemPropertyDiff = null;
                if (complexPropertyDiff != null) {
                    complexItemPropertyDiff = complexPropertyDiff.getDiff(complexItemName);
                }
                complexFieldDiffDisplay.put(complexItemName,
                // TODO: shouldn't we call getFieldDiffDisplay in case
                // of an embedded list?
                        getFinalFieldDiffDisplay(
                                complexFieldDiffDisplay.get(complexItemName),
                                complexItemPropertyDiff, true, styleClass));
            }
            finalFieldDiffDisplay = new PropertyDiffDisplayImpl(
                    (Serializable) complexFieldDiffDisplay);
        } else if (fieldDiffDisplay instanceof Calendar) {
            finalFieldDiffDisplay = new PropertyDiffDisplayImpl(
                    ((Calendar) fieldDiffDisplay).getTime(),
                    finalFieldDiffDisplayStyleClass);
        } else {
            finalFieldDiffDisplay = new PropertyDiffDisplayImpl(
                    fieldDiffDisplay, finalFieldDiffDisplayStyleClass);
        }
        return finalFieldDiffDisplay;
    }

    /**
     * Gets the list field diff display.
     *
     * @param listProperty the list property
     * @param listPropertyDiff the list property diff
     * @param isDisplayAllItems the is display all items
     * @param isDisplayItemIndexes the is display item indexes
     * @param styleClass the style class
     * @return the list field diff display
     * @throws ClientException the client exception
     */
    protected final PropertyDiffDisplay getListFieldDiffDisplay(
            List<Serializable> listProperty, ListPropertyDiff listPropertyDiff,
            boolean isDisplayAllItems, boolean isDisplayItemIndexes,
            String styleClass) throws ClientException {

        // Get list property indexes
        // By default: only items that are different (ie. held by the
        // propertyDiff)
        List<Integer> listPropertyIndexes = new ArrayList<Integer>();
        if (isDisplayAllItems) {
            // All items
            for (int index = 0; index < listProperty.size(); index++) {
                listPropertyIndexes.add(index);
            }
        } else {
            if (listPropertyDiff != null) {
                listPropertyIndexes = listPropertyDiff.getDiffIndexes();
            }
        }

        return getComplexListFieldDiffDisplay(listProperty,
                listPropertyIndexes, listPropertyDiff, isDisplayAllItems,
                isDisplayItemIndexes, styleClass);
    }

    protected final PropertyDiffDisplay getComplexListFieldDiffDisplay(
            List<Serializable> listProperty, List<Integer> listPropertyIndexes,
            ListPropertyDiff listPropertyDiff, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes, String styleClass)
            throws ClientException {

        if (listPropertyIndexes.isEmpty()) {
            return new PropertyDiffDisplayImpl(new ArrayList<Serializable>());
        }
        boolean isComplexListWidget = isDisplayItemIndexes
                || (listPropertyDiff != null && listPropertyDiff.size() > 0
                        && listPropertyDiff.getDiff(0).isComplexType() && !listPropertyDiff.getDiff(
                        0).isContentType());

        if (isComplexListWidget) {
            List<Map<String, Serializable>> listFieldDiffDisplay = new ArrayList<Map<String, Serializable>>();
            Set<String> complexPropertyItemNames = null;
            for (int index : listPropertyIndexes) {

                Map<String, Serializable> listItemDiffDisplay = new HashMap<String, Serializable>();
                // Put item index if wanted
                if (isDisplayItemIndexes) {
                    listItemDiffDisplay.put(
                            DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD, index + 1);
                }
                // Only put value if index is in list range
                if (index < listProperty.size()) {
                    Serializable listPropertyItem = listProperty.get(index);
                    PropertyDiff listItemPropertyDiff = null;
                    if (listPropertyDiff != null) {
                        listItemPropertyDiff = listPropertyDiff.getDiff(index);
                    }
                    if (isComplexType(listPropertyItem)) { // Complex
                                                           // list
                        ComplexPropertyDiff complexPropertyDiff = null;
                        if (listItemPropertyDiff != null
                                && listItemPropertyDiff.isComplexType()) {
                            complexPropertyDiff = (ComplexPropertyDiff) listItemPropertyDiff;
                        }
                        Map<String, Serializable> complexProperty = getComplexProperty(listPropertyItem);
                        complexPropertyItemNames = complexProperty.keySet();
                        for (String complexPropertyItemName : complexPropertyItemNames) {
                            Serializable complexPropertyItem = complexProperty.get(complexPropertyItemName);
                            // TODO: take into account subwidget properties
                            // 'displayAllItems' and 'displayItemIndexes'
                            // instead of inheriting them from the parent
                            // widget.
                            PropertyDiff complexItemPropertyDiff = null;
                            if (complexPropertyDiff != null) {
                                complexItemPropertyDiff = complexPropertyDiff.getDiff(complexPropertyItemName);
                            }
                            listItemDiffDisplay.put(
                                    complexPropertyItemName,
                                    getFieldDiffDisplay(complexPropertyItem,
                                            complexItemPropertyDiff,
                                            isDisplayAllItems,
                                            isDisplayItemIndexes, true,
                                            styleClass));
                        }
                    } else { // Scalar or content list
                        listItemDiffDisplay.put(
                                DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD,
                                getFinalFieldDiffDisplay(listPropertyItem,
                                        listItemPropertyDiff,
                                        isDisplayAllItems, styleClass));
                    }
                } else {// Index not in list range => put null value
                    if (complexPropertyItemNames != null) {
                        for (String complexPropertyItemName : complexPropertyItemNames) {
                            listItemDiffDisplay.put(complexPropertyItemName,
                                    new PropertyDiffDisplayImpl(null,
                                            styleClass));
                        }
                    } else {
                        listItemDiffDisplay.put(
                                DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD,
                                new PropertyDiffDisplayImpl(
                                        null,
                                        isDisplayAllItems ? styleClass
                                                : PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
                    }
                }
                listFieldDiffDisplay.add(listItemDiffDisplay);
            }
            return new PropertyDiffDisplayImpl(
                    (Serializable) listFieldDiffDisplay);
        } else {
            List<Serializable> listFieldDiffDisplay = new ArrayList<Serializable>();
            for (int index : listPropertyIndexes) {
                // Only put value if index is in list range
                if (index < listProperty.size()) {
                    PropertyDiff listItemPropertyDiff = null;
                    if (listPropertyDiff != null) {
                        listItemPropertyDiff = listPropertyDiff.getDiff(index);
                    }
                    listFieldDiffDisplay.add(getFinalFieldDiffDisplay(
                            listProperty.get(index), listItemPropertyDiff,
                            isDisplayAllItems, styleClass));
                } else {// Index not in list range => put null value
                    listFieldDiffDisplay.add(new PropertyDiffDisplayImpl(null,
                            isDisplayAllItems ? styleClass
                                    : PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
                }
            }
            return new PropertyDiffDisplayImpl(
                    (Serializable) listFieldDiffDisplay);
        }
    }

    protected final Serializable getFieldXPaths(String propertyName,
            PropertyDiff propertyDiff, Serializable leftProperty,
            Serializable rightProperty, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes,
            List<DiffFieldItemDefinition> complexFieldItemDefs)
            throws ClientException {

        Serializable fieldXPaths = null;
        if (propertyDiff == null) {
            throw new ClientException(
                    "The 'propertyDiff' parameter cannot be null.");
        }

        // Simple type
        if (propertyDiff.isSimpleType()) {
            SimplePropertyDiff simplePropertyDiff = (SimplePropertyDiff) propertyDiff;
            // Keep fieldXPaths null if one of the left or right properties is
            // empty
            if (!StringUtils.isEmpty(simplePropertyDiff.getLeftValue())
                    && !StringUtils.isEmpty(simplePropertyDiff.getRightValue())) {
                fieldXPaths = propertyName;
            }
        }
        // Content type
        // TODO: manage better content type (no use of hardcoded "data" and
        // so...)
        else if (propertyDiff.isContentType()) {
            ComplexPropertyDiff contentPropertyDiff = (ComplexPropertyDiff) propertyDiff;
            SimplePropertyDiff dataDiff = (SimplePropertyDiff) contentPropertyDiff.getDiff("data");
            // Keep fieldXPaths null if one of the left or right properties is
            // empty
            if (dataDiff != null
                    && !StringUtils.isEmpty(dataDiff.getLeftValue())
                    && !StringUtils.isEmpty(dataDiff.getRightValue())) {
                fieldXPaths = propertyName;
            }
        }
        // Complex type
        else if (propertyDiff.isComplexType()) {

            Map<String, Serializable> leftComplexProperty = getComplexPropertyIfNotNull(leftProperty);
            Map<String, Serializable> rightComplexProperty = getComplexPropertyIfNotNull(rightProperty);

            // TODO (maybe): take into account subwidget properties
            // 'displayAllItems' and 'displayItemIndexes'
            // instead of inheriting them from the parent
            // widget.
            Map<String, PropertyDiff> complexPropertyDiffMap = ((ComplexPropertyDiff) propertyDiff).getDiffMap();
            Map<String, Serializable> complexPropertyXPaths = new HashMap<String, Serializable>();
            if (CollectionUtils.isEmpty(complexFieldItemDefs)) {
                Iterator<String> complexFieldItemNamesIt = complexPropertyDiffMap.keySet().iterator();
                while (complexFieldItemNamesIt.hasNext()) {
                    String complexFieldItemName = complexFieldItemNamesIt.next();
                    setComplexPropertyXPaths(
                            complexPropertyXPaths,
                            complexFieldItemName,
                            getSubPropertyFullName(propertyName,
                                    complexFieldItemName),
                            complexPropertyDiffMap, leftComplexProperty,
                            rightComplexProperty, isDisplayAllItems,
                            isDisplayItemIndexes);
                }
            } else {
                for (DiffFieldItemDefinition complexFieldItemDef : complexFieldItemDefs) {
                    if (complexFieldItemDef.isDisplayContentDiffLinks()) {
                        String complexFieldItemName = complexFieldItemDef.getName();
                        if (complexPropertyDiffMap.containsKey(complexFieldItemName)) {
                            setComplexPropertyXPaths(
                                    complexPropertyXPaths,
                                    complexFieldItemName,
                                    getSubPropertyFullName(propertyName,
                                            complexFieldItemName),
                                    complexPropertyDiffMap,
                                    leftComplexProperty, rightComplexProperty,
                                    isDisplayAllItems, isDisplayItemIndexes);
                        }
                    }
                }
            }
            fieldXPaths = (Serializable) complexPropertyXPaths;
        }
        // List type
        else {
            List<Serializable> leftListProperty = getListPropertyIfNotNull(leftProperty);
            List<Serializable> rightListProperty = getListPropertyIfNotNull(rightProperty);

            ListPropertyDiff listPropertyDiff = (ListPropertyDiff) propertyDiff;

            // Get list property indexes
            // By default: only items that are different (ie. held by the
            // propertyDiff)
            List<Integer> listPropertyIndexes = new ArrayList<Integer>();
            if (isDisplayAllItems) {
                // All items
                int listPropertySize = Math.min(leftListProperty.size(),
                        rightListProperty.size());

                for (int index = 0; index < listPropertySize; index++) {
                    listPropertyIndexes.add(index);
                }
            } else {
                listPropertyIndexes = listPropertyDiff.getDiffIndexes();
            }
            fieldXPaths = (Serializable) getComplexListXPaths(propertyName,
                    listPropertyIndexes, listPropertyDiff, leftListProperty,
                    rightListProperty, isDisplayAllItems, isDisplayItemIndexes);
        }
        return fieldXPaths;
    }

    protected final Serializable getComplexListXPaths(String propertyName,
            List<Integer> listPropertyIndexes,
            ListPropertyDiff listPropertyDiff,
            List<Serializable> leftListProperty,
            List<Serializable> rightListProperty, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes) throws ClientException {

        if (listPropertyIndexes.isEmpty()) {
            return new ArrayList<Serializable>();
        }
        boolean isComplexListWidget = isDisplayItemIndexes
                || (listPropertyDiff.size() > 0
                        && listPropertyDiff.getDiff(0).isComplexType() && !listPropertyDiff.getDiff(
                        0).isContentType());

        if (isComplexListWidget) {
            List<Map<String, Serializable>> listFieldXPaths = new ArrayList<Map<String, Serializable>>();
            for (int index : listPropertyIndexes) {

                Map<String, Serializable> listItemXPaths = new HashMap<String, Serializable>();
                // Put item index if wanted
                if (isDisplayItemIndexes) {
                    listItemXPaths.put(DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD,
                            index + 1);
                }
                PropertyDiff listItemPropertyDiff = listPropertyDiff.getDiff(index);
                if (listItemPropertyDiff != null) {

                    Serializable leftListPropertyItem = null;
                    Serializable rightListPropertyItem = null;
                    if (index < leftListProperty.size()) {
                        leftListPropertyItem = leftListProperty.get(index);
                    }
                    if (index < rightListProperty.size()) {
                        rightListPropertyItem = rightListProperty.get(index);
                    }
                    Map<String, Serializable> leftComplexProperty = null;
                    Map<String, Serializable> rightComplexProperty = null;
                    if (isComplexType(leftListPropertyItem)) {
                        leftComplexProperty = getComplexProperty(leftListPropertyItem);
                    }
                    if (isComplexType(rightListPropertyItem)) {
                        rightComplexProperty = getComplexProperty(rightListPropertyItem);
                    }

                    // Complex list
                    if (listItemPropertyDiff.isComplexType()
                            && !listItemPropertyDiff.isContentType()) {
                        Map<String, PropertyDiff> complexPropertyDiffMap = ((ComplexPropertyDiff) listItemPropertyDiff).getDiffMap();
                        Iterator<String> complexPropertyItemNamesIt = complexPropertyDiffMap.keySet().iterator();
                        while (complexPropertyItemNamesIt.hasNext()) {
                            String complexPropertyItemName = complexPropertyItemNamesIt.next();
                            // TODO: take into account subwidget properties
                            // 'displayAllItems' and 'displayItemIndexes'
                            // instead of inheriting them from the parent
                            // widget.
                            setComplexPropertyXPaths(
                                    listItemXPaths,
                                    complexPropertyItemName,
                                    getSubPropertyFullName(
                                            propertyName,
                                            getSubPropertyFullName(
                                                    String.valueOf(index),
                                                    complexPropertyItemName)),
                                    complexPropertyDiffMap,
                                    leftComplexProperty, rightComplexProperty,
                                    isDisplayAllItems, isDisplayItemIndexes);
                        }
                    }
                    // Scalar or content list
                    else {
                        String listItemXPath = null;
                        // Keep listItemXPath null if one of the left or right
                        // properties is empty
                        if (leftListPropertyItem != null
                                && rightListPropertyItem != null) {
                            listItemXPath = getSubPropertyFullName(
                                    propertyName, String.valueOf(index));
                        }
                        listItemXPaths.put(
                                DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD,
                                listItemXPath);
                    }
                }
                listFieldXPaths.add(listItemXPaths);
            }
            return (Serializable) listFieldXPaths;
        } else {
            List<String> listFieldXPaths = new ArrayList<String>();
            for (int index : listPropertyIndexes) {
                String listItemXPath = null;
                // Keep listItemXPath null if one of the left or right
                // properties is empty
                if (index < leftListProperty.size()
                        && index < rightListProperty.size()) {
                    listItemXPath = getSubPropertyFullName(propertyName,
                            String.valueOf(index));
                }
                listFieldXPaths.add(listItemXPath);
            }
            return (Serializable) listFieldXPaths;
        }
    }

    /**
     * Sets the complex property xpaths.
     *
     * @param complexPropertyXPaths the complex property xpaths
     * @param complexFieldItemName the complex field item name
     * @param subPropertyFullName the sub property full name
     * @param complexPropertyDiffMap the complex property diff map
     * @param leftComplexProperty the left complex property
     * @param rightComplexProperty the right complex property
     * @param isDisplayAllItems the is display all items
     * @param isDisplayItemIndexes the is display item indexes
     * @throws ClientException the client exception
     */
    protected void setComplexPropertyXPaths(
            Map<String, Serializable> complexPropertyXPaths,
            String complexFieldItemName, String subPropertyFullName,
            Map<String, PropertyDiff> complexPropertyDiffMap,
            Map<String, Serializable> leftComplexProperty,
            Map<String, Serializable> rightComplexProperty,
            boolean isDisplayAllItems, boolean isDisplayItemIndexes)
            throws ClientException {

        Serializable leftComplexPropertyItemValue = null;
        Serializable rightComplexPropertyItemValue = null;
        if (leftComplexProperty != null) {
            leftComplexPropertyItemValue = leftComplexProperty.get(complexFieldItemName);
        }
        if (rightComplexProperty != null) {
            rightComplexPropertyItemValue = rightComplexProperty.get(complexFieldItemName);
        }
        complexPropertyXPaths.put(
                complexFieldItemName,
                getFieldXPaths(subPropertyFullName,
                        complexPropertyDiffMap.get(complexFieldItemName),
                        leftComplexPropertyItemValue,
                        rightComplexPropertyItemValue, isDisplayAllItems,
                        isDisplayItemIndexes, null));
    }

    protected boolean isListType(Serializable property) {

        return property instanceof List<?>
                || property instanceof Serializable[];
    }

    protected boolean isComplexType(Serializable property) {

        return property instanceof Map<?, ?>;
    }

    /**
     * Casts or convert a {@link Serializable} property to {@link List
     * <Serializable>}.
     *
     * @param property the property
     * @return the list property
     * @throws ClassCastException if the {@code property} is not a {@link List
     *             <Serializable>} nor an array.
     */
    @SuppressWarnings("unchecked")
    protected List<Serializable> getListProperty(Serializable property) {
        List<Serializable> listProperty;
        if (property instanceof List<?>) { // List
            listProperty = (List<Serializable>) property;
        } else { // Array
            listProperty = Arrays.asList((Serializable[]) property);
        }
        return listProperty;
    }

    /**
     * Gets the list property if the {@code property} is not null.
     *
     * @param property the property
     * @return the list property if the {@code property} is not null, null
     *         otherwise
     * @throws ClientException if the {@code property} is not a list.
     */
    protected List<Serializable> getListPropertyIfNotNull(Serializable property)
            throws ClientException {

        if (property != null) {
            if (!isListType(property)) {
                throw new ClientException(
                        "Tryed to get a list property from a Serializable property that is not a list, this is inconsistent.");
            }
            return getListProperty(property);
        }
        return null;
    }

    /**
     * Casts a {@link Serializable} property to {@link Map<String,
     * Serializable>}.
     *
     * @param property the property
     * @return the complex property
     * @throws ClassCastException if the {@code property} is not a {@link Map
     *             <String, Serializable>}.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Serializable> getComplexProperty(Serializable property) {
        return (Map<String, Serializable>) property;
    }

    /**
     * Gets the complex property if the {@code property} is not null.
     *
     * @param property the property
     * @return the complex property if the {@code property} is not null, null
     *         otherwise
     * @throws ClientException if the {@code property} is not a list.
     */
    protected Map<String, Serializable> getComplexPropertyIfNotNull(
            Serializable property) throws ClientException {

        if (property != null) {
            if (!isComplexType(property)) {
                throw new ClientException(
                        "Tryed to get a complex property from a Serializable property that is not a map, this is inconsistent.");
            }
            return getComplexProperty(property);
        }
        return null;
    }

    // TODO: better separate regular and contentDiffLinksWidget cases (call
    // submethods)
    protected final WidgetDefinition getWidgetDefinition(String propertyName,
            String propertyType, Field field,
            List<DiffFieldItemDefinition> complexFieldItemDefs,
            boolean isContentDiffLinksWidget) throws ClientException {

        boolean isGeneric = false;
        boolean isCloned = false;

        WidgetDefinition wDef = null;
        if (!isContentDiffLinksWidget) {
            // Look for a specific widget in the "diff" category named with the
            // property name
            wDef = getLayoutStore().getWidgetDefinition(DIFF_WIDGET_CATEGORY,
                    propertyName);
            if (wDef == null) {
                isGeneric = true;
                // Fallback on a generic widget in the "diff" category named
                // with the property type
                wDef = getLayoutStore().getWidgetDefinition(
                        DIFF_WIDGET_CATEGORY, propertyType);
                if (wDef == null) {
                    throw new ClientException(
                            String.format(
                                    "Could not find any specific widget named '%s', nor any generic widget named '%s'. Please make sure at least a generic widget is defined for this type.",
                                    propertyName, propertyType));
                }
            }
        } else {
            isGeneric = true;
            if (PropertyType.isSimpleType(propertyType)
                    || PropertyType.isContentType(propertyType)) {
                wDef = getLayoutStore().getWidgetDefinition(
                        DIFF_WIDGET_CATEGORY, CONTENT_DIFF_LINKS_WIDGET_NAME);
                if (wDef == null) {
                    throw new ClientException(
                            String.format(
                                    "Could not find any generic widget named '%s'. Please make sure a generic widget is defined with this name.",
                                    CONTENT_DIFF_LINKS_WIDGET_NAME));
                }
            } else {
                // Get the generic widget in the "diff" category named with
                // the property type
                wDef = getLayoutStore().getWidgetDefinition(
                        DIFF_WIDGET_CATEGORY, propertyType);
                if (wDef == null) {
                    throw new ClientException(
                            String.format(
                                    "Could not find any generic widget named '%s'. Please make sure a generic widget is defined for this type.",
                                    propertyType));
                }
            }
        }

        if (isGeneric) {
            // Clone widget definition
            wDef = wDef.clone();
            isCloned = true;
            // Set widget name
            String widgetName = propertyName;
            if (isContentDiffLinksWidget) {
                widgetName += CONTENT_DIFF_LINKS_WIDGET_NAME_SUFFIX;
            }
            wDef.setName(widgetName);

            // Set labels
            Map<String, String> labels = new HashMap<String, String>();
            labels.put(BuiltinModes.ANY, DIFF_WIDGET_LABEL_PREFIX
                    + getPropertyLabel(propertyName));
            wDef.setLabels(labels);

            // Set translated
            wDef.setTranslated(true);

            // TODO: set props ?
        }

        // TODO: better manage specific case of content type
        // filename/content (file and files) and note

        // Set field definitions if generic or specific and not already set in
        // widget definition
        if (isGeneric || !isFieldDefinitions(wDef)) {

            String fieldName = propertyName;
            if (field != null) {
                fieldName = field.getName().getLocalName();
            }

            FieldDefinition[] fieldDefinitions;
            if (isContentDiffLinksWidget) {
                fieldDefinitions = new FieldDefinition[1];
                fieldDefinitions[0] = new FieldDefinitionImpl(null, fieldName);
            } else {
                int fieldCount = 2;
                if (PropertyType.isContentType(propertyType)) {
                    fieldCount = 3;
                }
                fieldDefinitions = new FieldDefinition[fieldCount];
                fieldDefinitions[0] = new FieldDefinitionImpl(null,
                        getFieldDefinitionValueFieldName(fieldName));

                FieldDefinition styleClassFieldDef = new FieldDefinitionImpl(
                        null, getFieldDefinitionStyleClassFieldName(fieldName));
                if (PropertyType.isContentType(propertyType)) {
                    fieldName = "filename";
                    if (field == null) {
                        fieldName = getPropertyName(
                                getPropertySchema(propertyName), fieldName);
                    }
                    fieldDefinitions[1] = new FieldDefinitionImpl(null,
                            getFieldDefinitionValueFieldName(fieldName));
                    fieldDefinitions[2] = styleClassFieldDef;
                } else {
                    fieldDefinitions[1] = styleClassFieldDef;
                }
            }

            // Clone if needed
            if (!isCloned) {
                wDef = wDef.clone();
                isCloned = true;
            }
            wDef.setFieldDefinitions(fieldDefinitions);
        }

        // Set subwidgets if not already set
        if (!isSubWidgets(wDef)) {
            if (PropertyType.isListType(propertyType)
                    || (PropertyType.isComplexType(propertyType) && !PropertyType.isContentType(propertyType))) {

                Field declaringField = field;
                if (declaringField == null) {
                    declaringField = ComplexPropertyHelper.getField(
                            getPropertySchema(propertyName),
                            getPropertyField(propertyName));
                }
                // Clone if needed
                if (!isCloned) {
                    wDef = wDef.clone();
                    isCloned = true;
                }
                wDef.setSubWidgetDefinitions(getSubWidgetDefinitions(
                        propertyName, propertyType, declaringField,
                        complexFieldItemDefs, isDisplayItemIndexes(wDef),
                        isContentDiffLinksWidget));
            }
        }

        return wDef;
    }

    protected final boolean isSubWidgets(WidgetDefinition wDef) {

        WidgetDefinition[] subWidgetDefs = wDef.getSubWidgetDefinitions();
        return subWidgetDefs != null && subWidgetDefs.length > 0;
    }

    protected final boolean isFieldDefinitions(WidgetDefinition wDef) {

        FieldDefinition[] fieldDefs = wDef.getFieldDefinitions();
        return fieldDefs != null && fieldDefs.length > 0;
    }

    protected final String getFieldDefinitionValueFieldName(String fieldName) {

        return fieldName + "/" + DIFF_WIDGET_FIELD_DEFINITION_VALUE_SUFFIX;
    }

    protected final String getFieldDefinitionStyleClassFieldName(
            String fieldName) {

        return fieldName + "/"
                + DIFF_WIDGET_FIELD_DEFINITION_STYLE_CLASS_SUFFIX;
    }

    protected final WidgetDefinition[] getSubWidgetDefinitions(
            String propertyName, String propertyType, Field field,
            List<DiffFieldItemDefinition> complexFieldItemDefs,
            boolean isDisplayItemIndexes, boolean isContentDiffLinks)
            throws ClientException {

        WidgetDefinition[] subWidgetDefs = null;
        // Complex
        if (PropertyType.isComplexType(propertyType)
                && !PropertyType.isContentType(propertyType)) {
            subWidgetDefs = getComplexSubWidgetDefinitions(propertyName, field,
                    complexFieldItemDefs, false, isContentDiffLinks);
        }
        // Scalar or content list
        else if (PropertyType.isScalarListType(propertyType)
                || PropertyType.isContentListType(propertyType)) {
            Field listFieldItem = ComplexPropertyHelper.getListFieldItem(field);
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes, 1);
            subWidgetDefs[subWidgetDefs.length - 1] = getWidgetDefinition(
                    getSubPropertyFullName(propertyName,
                            listFieldItem.getName().getLocalName()),
                    ComplexPropertyHelper.getFieldType(listFieldItem),
                    new FieldImpl(new QName(
                            DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD),
                            field.getType(), listFieldItem.getType()), null,
                    isContentDiffLinks);
        }
        // Complex list
        else if (PropertyType.isComplexListType(propertyType)) {
            Field listFieldItem = ComplexPropertyHelper.getListFieldItem(field);
            subWidgetDefs = getComplexSubWidgetDefinitions(propertyName,
                    listFieldItem, complexFieldItemDefs, isDisplayItemIndexes,
                    isContentDiffLinks);
        }
        return subWidgetDefs;
    }

    protected final WidgetDefinition[] getComplexSubWidgetDefinitions(
            String propertyName, Field field,
            List<DiffFieldItemDefinition> complexFieldItemDefs,
            boolean isDisplayItemIndexes, boolean isContentDiffLinks)
            throws ClientException {

        WidgetDefinition[] subWidgetDefs;
        int subWidgetIndex = isDisplayItemIndexes ? 1 : 0;

        if (CollectionUtils.isEmpty(complexFieldItemDefs)) {
            List<Field> complexFieldItems = ComplexPropertyHelper.getComplexFieldItems(field);
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes,
                    complexFieldItems.size());

            for (Field complexFieldItem : complexFieldItems) {
                subWidgetDefs[subWidgetIndex] = getWidgetDefinition(
                        getSubPropertyFullName(propertyName,
                                complexFieldItem.getName().getLocalName()),
                        ComplexPropertyHelper.getFieldType(complexFieldItem),
                        complexFieldItem, null, isContentDiffLinks);
                subWidgetIndex++;
            }
        } else {
            int subWidgetCount = complexFieldItemDefs.size();
            // Only add a subwidget for the items marked to display the content
            // diff links
            if (isContentDiffLinks) {
                subWidgetCount = 0;
                for (DiffFieldItemDefinition complexFieldItemDef : complexFieldItemDefs) {
                    if (complexFieldItemDef.isDisplayContentDiffLinks()) {
                        subWidgetCount++;
                    }
                }
            }
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes,
                    subWidgetCount);

            for (DiffFieldItemDefinition complexFieldItemDef : complexFieldItemDefs) {
                if (!isContentDiffLinks
                        || complexFieldItemDef.isDisplayContentDiffLinks()) {
                    String complexFieldItemName = complexFieldItemDef.getName();
                    Field complexFieldItem = ComplexPropertyHelper.getComplexFieldItem(
                            field, complexFieldItemName);
                    if (complexFieldItem != null) {
                        subWidgetDefs[subWidgetIndex] = getWidgetDefinition(
                                getSubPropertyFullName(propertyName,
                                        complexFieldItemName),
                                ComplexPropertyHelper.getFieldType(complexFieldItem),
                                complexFieldItem, null, isContentDiffLinks);
                        subWidgetIndex++;
                    }
                }
            }
        }
        return subWidgetDefs;
    }

    protected final WidgetDefinition[] initSubWidgetDefinitions(
            boolean isDisplayItemIndexes, int subWidgetCount) {

        WidgetDefinition[] subWidgetDefs;
        if (isDisplayItemIndexes) {
            subWidgetDefs = new WidgetDefinition[subWidgetCount + 1];
            subWidgetDefs[0] = getIndexSubwidgetDefinition();
        } else {
            subWidgetDefs = new WidgetDefinition[subWidgetCount];
        }

        return subWidgetDefs;
    }

    protected final WidgetDefinition getIndexSubwidgetDefinition() {

        FieldDefinition[] fieldDefinitions = { new FieldDefinitionImpl(null,
                DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD) };

        return new WidgetDefinitionImpl(DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD,
                DIFF_LIST_WIDGET_INDEX_SUBWIDGET_TYPE,
                DIFF_LIST_WIDGET_INDEX_SUBWIDGET_LABEL, null, true, null,
                Arrays.asList(fieldDefinitions), null, null);
    }

    /**
     * Gets the property name.
     *
     * @param schema the schema
     * @param field the field
     * @return the property name
     */
    protected final String getPropertyName(String schema, String field) {

        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(schema)) {
            sb.append(schema);
            sb.append(":");
        }
        sb.append(field);
        return sb.toString();
    }

    protected final String getSubPropertyFullName(String basePropertyName,
            String subPropertyName) {

        if (StringUtils.isEmpty(subPropertyName)) {
            return basePropertyName;
        }
        StringBuilder sb = new StringBuilder(basePropertyName);
        sb.append("/");
        sb.append(subPropertyName);
        return sb.toString();
    }

    protected final String getPropertySchema(String propertyName) {

        int indexOfColon = propertyName.indexOf(':');
        if (indexOfColon > -1) {
            return propertyName.substring(0, indexOfColon);
        }
        return null;
    }

    protected final String getPropertyField(String propertyName) {

        int indexOfColon = propertyName.indexOf(':');
        if (indexOfColon > -1 && indexOfColon < propertyName.length() - 1) {
            return propertyName.substring(indexOfColon + 1);
        }
        return propertyName;
    }

    protected final String getPropertyLabel(String propertyName) {

        return propertyName.replaceAll(":", ".").replaceAll("/", ".");
    }

    /**
     * Gets the layout store service.
     *
     * @return the layout store service
     * @throws ClientException the client exception
     */
    protected final LayoutStore getLayoutStore() throws ClientException {

        LayoutStore layoutStore;
        try {
            layoutStore = Framework.getService(LayoutStore.class);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
        if (layoutStore == null) {
            throw new ClientException("LayoutStore service is null.");
        }
        return layoutStore;
    }
}