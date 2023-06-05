/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2023 Apryse Group NV
    Authors: Apryse Software.

    This program is offered under a commercial and under the AGPL license.
    For commercial licensing, contact us at https://itextpdf.com/sales.  For AGPL licensing, see below.

    AGPL licensing:
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itextpdf.layout.element;

import com.itextpdf.layout.renderer.ColumnContainerRenderer;
import com.itextpdf.layout.renderer.IRenderer;

import java.util.Map;

/**
 * represents a container of the column objects.
 */
public class ColumnContainer extends Div {

    /**
     * Creates new {@link ColumnContainer} instance.
     */
    public ColumnContainer() {
        super();
    }

    /**
     * Copies all properties of {@link ColumnContainer} to its child elements.
     */
    public void copyAllPropertiesToChildren() {
        for (final IElement child : this.getChildren()) {
            for (final Map.Entry<Integer, Object> entry : this.properties.entrySet()) {
                child.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    protected IRenderer makeNewRenderer() {
        return new ColumnContainerRenderer(this);
    }

}
