/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2018 iText Group NV
    Authors: iText Software.

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
package com.itextpdf.svg.renderers.impl;

import com.itextpdf.kernel.geom.Point;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.styledxmlparser.css.util.CssUtils;
import com.itextpdf.svg.SvgConstants;
import com.itextpdf.svg.exceptions.SvgLogMessageConstant;
import com.itextpdf.svg.exceptions.SvgProcessingException;
import com.itextpdf.svg.renderers.ISvgNodeRenderer;
import com.itextpdf.svg.renderers.SvgDrawContext;
import com.itextpdf.svg.renderers.path.SvgPathShapeFactory;
import com.itextpdf.svg.renderers.path.IPathShape;
import com.itextpdf.svg.renderers.path.impl.ClosePath;
import com.itextpdf.svg.renderers.path.impl.MoveTo;
import com.itextpdf.svg.renderers.path.impl.CurveTo;
import com.itextpdf.svg.renderers.path.impl.SmoothSCurveTo;
import com.itextpdf.svg.renderers.path.impl.HorizontalLineTo;
import com.itextpdf.svg.renderers.path.impl.VerticalLineTo;
import com.itextpdf.svg.utils.SvgCssUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * {@link ISvgNodeRenderer} implementation for the &lt;path&gt; tag.
 */
public class PathSvgNodeRenderer extends AbstractSvgNodeRenderer {

    private static final String SEPARATOR = "";
    private static final String SPACE_CHAR = " ";

    /**
     * The regular expression to find invalid operators in the <a href="https://www.w3.org/TR/SVG/paths.html#PathData">PathData attribute of the &ltpath&gt element</a>
     * <p>
     * Any two consecutive letters are an invalid operator.
     */
    private final String INVALID_OPERATOR_REGEX = "(\\p{L}{2,})";


    /**
     * The regular expression to split the <a href="https://www.w3.org/TR/SVG/paths.html#PathData">PathData attribute of the &ltpath&gt element</a>
     * <p>
     * Since {@link PathSvgNodeRenderer#containsInvalidAttributes(String)} is called before the use of this expression in {@link PathSvgNodeRenderer#parsePropertiesAndStyles()} the attribute to be split is valid.
     * The regex splits at each letter.
     */
    private final String SPLIT_REGEX = "(?=[\\p{L}])";


    /**
     * The {@link Point} representing the current point in the path to be used for relative pathing operations.
     * The original value is {@code null}, and must be set via a {@link MoveTo} operation before it may be referenced.
     */
    private Point currentPoint = null;

    /**
     * The {@link ClosePath} shape keeping track of the initial point set by a {@link MoveTo} operation.
     * The original value is {@code null}, and must be set via a {@link MoveTo} operation before it may drawn.
     */
    private ClosePath zOperator = null;

    @Override
    public void doDraw(SvgDrawContext context) {
        PdfCanvas canvas = context.getCurrentCanvas();
        canvas.writeLiteral("% path\n");
        for (IPathShape item : getShapes()) {
            item.draw(canvas);
        }
    }

    @Override
    public ISvgNodeRenderer createDeepCopy() {
        PathSvgNodeRenderer copy = new PathSvgNodeRenderer();
        deepCopyAttributesAndStyles(copy);
        return copy;
    }

    /**
     * Gets the coordinates that shall be passed to {@link IPathShape#setCoordinates(String[])} for the current shape.
     *
     * @param shape          The current shape.
     * @param previousShape  The previous shape which can affect the coordinates of the current shape.
     * @param pathProperties The operator and all arguments as a {@link String[]}
     * @return a {@link String[]} of coordinates that shall be passed to {@link IPathShape#setCoordinates(String[])}
     */
    private String[] getShapeCoordinates(IPathShape shape, IPathShape previousShape, String[] pathProperties) {
        if (shape instanceof ClosePath) {
            return null;
        }
        String[] shapeCoordinates = null;
        String[] operatorArgs = Arrays.copyOfRange(pathProperties, 1, pathProperties.length);
        if (shape instanceof SmoothSCurveTo) {
            String[] startingControlPoint = new String[2];
            if (previousShape != null) {
                Map<String, String> coordinates = previousShape.getCoordinates();

                //if the previous command was a C or S use its last control point
                if (((previousShape instanceof CurveTo) || (previousShape instanceof SmoothSCurveTo))) {
                    float reflectedX = (float) (2 * CssUtils.parseFloat(coordinates.get(SvgConstants.Attributes.X)) - CssUtils.parseFloat(coordinates.get(SvgConstants.Attributes.X2)));
                    float reflectedy = (float) (2 * CssUtils.parseFloat(coordinates.get(SvgConstants.Attributes.Y)) - CssUtils.parseFloat(coordinates.get(SvgConstants.Attributes.Y2)));

                    startingControlPoint[0] = SvgCssUtils.convertFloatToString(reflectedX);
                    startingControlPoint[1] = SvgCssUtils.convertFloatToString(reflectedy);
                } else {
                    startingControlPoint[0] = coordinates.get(SvgConstants.Attributes.X);
                    startingControlPoint[1] = coordinates.get(SvgConstants.Attributes.Y);
                }
            } else {
                // TODO RND-951
                startingControlPoint[0] = pathProperties[1];
                startingControlPoint[1] = pathProperties[2];
            }
            shapeCoordinates = concatenate(startingControlPoint, operatorArgs);
        } else if (shape instanceof VerticalLineTo) {
            String currentX = String.valueOf(currentPoint.x);
            String currentY = String.valueOf(currentPoint.y);
            String[] yValues = concatenate(new String[]{currentY}, operatorArgs);
            shapeCoordinates = concatenate(new String[]{currentX}, yValues);

        } else if (shape instanceof HorizontalLineTo) {
            String currentX = String.valueOf(currentPoint.x);
            String currentY = String.valueOf(currentPoint.y);
            String[] xValues = concatenate(new String[]{currentX}, operatorArgs);
            shapeCoordinates = concatenate(new String[]{currentY}, xValues);
        }
        if (shapeCoordinates == null) {
            shapeCoordinates = operatorArgs;
        }
        return shapeCoordinates;
    }


    /**
     * Processes an individual pathing operator and all of its arguments, converting into one or more
     * {@link IPathShape} objects.
     *
     * @param pathProperties The property operator and all arguments as a {@link String[]}
     * @param previousShape  The previous shape which can affect the positioning of the current shape. If no previous
     *                       shape exists {@code null} is passed.
     * @return a {@link List} of each {@link IPathShape} that should be drawn to represent the operator.
     */
    private List<IPathShape> processPathOperator(String[] pathProperties, IPathShape previousShape) {
        List<IPathShape> shapes = new ArrayList<>();
        if (pathProperties.length == 0 || pathProperties[0].equals(SEPARATOR)) {
            return shapes;
        }
        //Implements (absolute) command value only
        //TODO implement relative values e. C(absolute), c(relative)
        IPathShape pathShape = SvgPathShapeFactory.createPathShape(pathProperties[0]);
        String[] shapeCoordinates = getShapeCoordinates(pathShape, previousShape, pathProperties);
        if (pathShape instanceof ClosePath) {
            pathShape = zOperator;
            if (pathShape == null) {
                throw new SvgProcessingException(SvgLogMessageConstant.INVALID_CLOSEPATH_OPERATOR_USE);
            }
        } else if (pathShape instanceof MoveTo) {
            zOperator = new ClosePath();
            zOperator.setCoordinates(shapeCoordinates);
        }

        Point endingPoint = null;
        if (pathShape != null) {
            if (shapeCoordinates != null) {
                pathShape.setCoordinates(shapeCoordinates);
            }
            endingPoint = pathShape.getEndingPoint();
            shapes.add(pathShape);
        }
        currentPoint = endingPoint;
        return shapes;
    }


    /**
     * Processes the {@link SvgConstants.Attributes.D} {@link PathSvgNodeRenderer#attributesAndStyles} and converts them
     * into one or more {@link IPathShape} objects to be drawn on the canvas.
     * <p>
     * Each individual operator is passed to {@link PathSvgNodeRenderer#processPathOperator(String[], IPathShape)} to be processed individually.
     *
     * @return a {@link Collection} of each {@link IPathShape} that should be drawn to represent the path.
     */
    private Collection<IPathShape> getShapes() {
        Collection<String> parsedResults = parsePropertiesAndStyles();
        List<IPathShape> shapes = new ArrayList<>();

        for (String parsedResult : parsedResults) {
            String[] pathProperties = parsedResult.split(SPACE_CHAR);
            IPathShape previousShape = shapes.size() == 0 ? null : shapes.get(shapes.size() - 1);
            List<IPathShape> operatorShapes = processPathOperator(pathProperties, previousShape);
            shapes.addAll(operatorShapes);
        }
        return shapes;
    }

    private static String[] concatenate(String[] first, String[] second) {
        String[] arr = new String[first.length + second.length];
        System.arraycopy(first, 0, arr, 0, first.length);
        System.arraycopy(second, 0, arr, first.length, second.length);
        return arr;
    }

    private boolean containsInvalidAttributes(String attributes) {
        return attributes.split(INVALID_OPERATOR_REGEX).length > 1;
    }

    private Collection<String> parsePropertiesAndStyles() {
        StringBuilder result = new StringBuilder();
        String attributes = attributesAndStyles.get(SvgConstants.Attributes.D);
        if (containsInvalidAttributes(attributes)) {
            throw new SvgProcessingException(SvgLogMessageConstant.INVALID_PATH_D_ATTRIBUTE_OPERATORS).setMessageParams(attributes);
        }
        String[] coordinates = attributes.split(SPLIT_REGEX);//gets an array attributesAr of {M 100 100, L 300 100, L200, 300, z}

        for (String inst : coordinates) {
            if (!inst.equals(SEPARATOR)) {
                String instTrim = inst.trim();
                String instruction = instTrim.charAt(0) + SPACE_CHAR;
                String temp = instruction + instTrim.replace(instTrim.charAt(0) + SEPARATOR, SEPARATOR).replace(",", SPACE_CHAR).trim();
                result.append(SPACE_CHAR);
                result.append(temp);
            }
        }

        String[] resultArray = result.toString().split(SPLIT_REGEX);
        List<String> resultList = new ArrayList<>(Arrays.asList(resultArray));

        return resultList;
    }


}