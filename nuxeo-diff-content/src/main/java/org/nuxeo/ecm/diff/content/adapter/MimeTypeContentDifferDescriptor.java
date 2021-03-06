/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.diff.content.adapter;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Antoine Taillefer
 * @since 5.6
 */
@XObject("contentDiffer")
public class MimeTypeContentDifferDescriptor {

    @XNode("pattern")
    private String pattern;

    @XNode("@class")
    private Class<? extends MimeTypeContentDiffer> klass;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Class<? extends MimeTypeContentDiffer> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends MimeTypeContentDiffer> klass) {
        this.klass = klass;
    }

}
