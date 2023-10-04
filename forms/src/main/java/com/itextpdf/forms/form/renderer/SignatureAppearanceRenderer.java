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
package com.itextpdf.forms.form.renderer;

import com.itextpdf.commons.utils.MessageFormatUtil;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.AbstractPdfFormField;
import com.itextpdf.forms.fields.PdfFormCreator;
import com.itextpdf.forms.fields.PdfSignatureFormField;
import com.itextpdf.forms.fields.SignatureFormFieldBuilder;
import com.itextpdf.forms.form.element.SignatureFieldAppearance;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.logs.IoLogMessageConstant;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.Background;
import com.itextpdf.layout.properties.BackgroundImage;
import com.itextpdf.layout.properties.BackgroundPosition;
import com.itextpdf.layout.properties.BackgroundRepeat;
import com.itextpdf.layout.properties.BackgroundSize;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.RenderingMode;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.layout.renderer.DrawContext;
import com.itextpdf.layout.renderer.IRenderer;
import com.itextpdf.layout.renderer.ImageRenderer;
import com.itextpdf.layout.renderer.ParagraphRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractTextFieldRenderer} implementation for SigFields.
 */
public class SignatureAppearanceRenderer extends AbstractTextFieldRenderer {
    /**
     * Extra space at the top.
     */
    private static final float TOP_SECTION = 0.3f;

    private static final float EPS = 1e-5f;

    private boolean isFontSizeApproximated = false;

    /**
     * Creates a new {@link SignatureAppearanceRenderer} instance.
     *
     * @param modelElement the model element
     */
    public SignatureAppearanceRenderer(SignatureFieldAppearance modelElement) {
        super(modelElement);
        applyBackgroundImage(modelElement);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean isLayoutBasedOnFlatRenderer() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected IRenderer createFlatRenderer() {
        Div div = new Div();

        String description = ((SignatureFieldAppearance) modelElement).getDescription(true);
        SignatureFieldAppearance.RenderingMode renderingMode =
                ((SignatureFieldAppearance) modelElement).getRenderingMode();
        switch (renderingMode) {
            case NAME_AND_DESCRIPTION:
                div.add(new Paragraph(((SignatureFieldAppearance) modelElement).getSignedBy())
                                .setMargin(0).setMultipliedLeading(0.9f))
                        .add(new Paragraph(description).setMargin(0).setMultipliedLeading(0.9f));
                break;
            case GRAPHIC_AND_DESCRIPTION: {
                ImageData signatureGraphic = ((SignatureFieldAppearance) modelElement).getSignatureGraphic();
                if (signatureGraphic == null) {
                    throw new IllegalStateException("A signature image must be present when rendering mode is " +
                            "graphic and description. Use setSignatureGraphic()");
                }
                div.add(new Image(signatureGraphic))
                        .add(new Paragraph(description).setMargin(0).setMultipliedLeading(0.9f));
                break;
            }
            case GRAPHIC:
                ImageData signatureGraphic = ((SignatureFieldAppearance) modelElement).getSignatureGraphic();
                if (signatureGraphic == null) {
                    throw new IllegalStateException("A signature image must be present when rendering mode is " +
                            "graphic. Use setSignatureGraphic()");
                }
                div.add(new Image(signatureGraphic));
                break;
            default:
                div.add(new Paragraph(description).setMargin(0).setMultipliedLeading(0.9f));
                break;
        }
        return div.createRendererSubTree();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LayoutResult layout(LayoutContext layoutContext) {
        approximateFontSizeToFitLayoutArea(layoutContext);
        return super.layout(layoutContext);
    }

    /**
     * {@inheritDoc}
     *
     * @param layoutContext {@inheritDoc}
     */
    @Override
    protected void adjustFieldLayout(LayoutContext layoutContext) {
        Rectangle bBox = getOccupiedArea().getBBox().clone();
        applyPaddings(bBox, false);
        applyBorderBox(bBox, false);
        if (bBox.getY() < 0) {
            bBox.setHeight(bBox.getY() + bBox.getHeight());
            bBox.setY(0);
        }

        Rectangle descriptionRect = null;
        Rectangle signatureRect = null;

        SignatureFieldAppearance.RenderingMode renderingMode =
                ((SignatureFieldAppearance) modelElement).getRenderingMode();
        switch (renderingMode) {
            case NAME_AND_DESCRIPTION:
            case GRAPHIC_AND_DESCRIPTION: {
                // Split the signature field into two and add the name of the signer or an image to the one side,
                // the description to the other side.
                UnitValue[] paddings = getPaddings();
                if (bBox.getHeight() > bBox.getWidth()) {
                    float topPadding = paddings[0].getValue();
                    float bottomPadding = paddings[2].getValue();
                    signatureRect = new Rectangle(
                            bBox.getX(),
                            bBox.getY() + bBox.getHeight() / 2 + bottomPadding / 2,
                            bBox.getWidth(),
                            bBox.getHeight() / 2 - bottomPadding / 2);
                    descriptionRect = new Rectangle(
                            bBox.getX(),
                            bBox.getY(),
                            bBox.getWidth(),
                            bBox.getHeight() / 2 - topPadding / 2);
                } else {
                    // origin is the bottom-left
                    float rightPadding = paddings[1].getValue();
                    float leftPadding = paddings[3].getValue();
                    signatureRect = new Rectangle(
                            bBox.getX(),
                            bBox.getY(),
                            bBox.getWidth() / 2 - rightPadding / 2,
                            bBox.getHeight());
                    descriptionRect = new Rectangle(
                            bBox.getX() + bBox.getWidth() / 2 + leftPadding / 2,
                            bBox.getY(),
                            bBox.getWidth() / 2 - leftPadding / 2,
                            bBox.getHeight());
                }
                break;
            }
            case GRAPHIC:
                // The signature field will consist of an image only; no description will be shown.
                signatureRect = bBox;
                break;
            default:
                // Default one, it just shows whatever description was defined for the signature.
                if (retrieveHeight() == null) {
                    // Adjust calculated occupied area height to keep the same font size.
                    float calculatedHeight = getOccupiedArea().getBBox().getHeight();
                    getOccupiedArea().getBBox().moveDown(calculatedHeight * TOP_SECTION)
                            .setHeight(calculatedHeight * (1 + TOP_SECTION));
                    bBox.moveDown(calculatedHeight * TOP_SECTION);
                }
                descriptionRect = bBox.setHeight(getOccupiedArea().getBBox().getHeight() * (1 - TOP_SECTION)
                - calculateAdditionalHeight());
                break;
        }

        adjustChildrenLayout(renderingMode, signatureRect, descriptionRect, layoutContext.getArea().getPageNumber());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public IRenderer getNextRenderer() {
        return new SignatureAppearanceRenderer((SignatureFieldAppearance) modelElement);
    }

    /**
     * Gets the default value of the form field.
     *
     * @return the default value of the form field.
     */
    @Override
    public String getDefaultValue() {
        // FormProperty.FORM_FIELD_VALUE is not supported for SigField element.
        return "";
    }

    /**
     * {@inheritDoc}
     *
     * @param drawContext {@inheritDoc}
     */
    @Override
    protected void applyAcroField(DrawContext drawContext) {
        String name = getModelId();
        UnitValue fontSize = (UnitValue) this.getPropertyAsUnitValue(Property.FONT_SIZE);
        if (!fontSize.isPointValue()) {
            Logger logger = LoggerFactory.getLogger(SignatureAppearanceRenderer.class);
            logger.error(MessageFormatUtil.format(IoLogMessageConstant.PROPERTY_IN_PERCENTS_NOT_SUPPORTED,
                    Property.FONT_SIZE));
        }
        PdfDocument doc = drawContext.getDocument();
        Rectangle area = getOccupiedArea().getBBox().clone();
        applyMargins(area, false);
        deleteMargins();
        PdfPage page = doc.getPage(occupiedArea.getPageNumber());

        Background background = this.<Background>getProperty(Property.BACKGROUND);
        // Background is light gray by default, but can be set to null by user.
        final Color backgroundColor = background == null ? null : background.getColor();

        final float fontSizeValue = fontSize.getValue();
        if (font == null) {
            font = doc.getDefaultFont();
        }

        // Some properties are set to the HtmlDocumentRenderer, which is root renderer for this SigFieldRenderer, but
        // in forms logic root renderer is CanvasRenderer, and these properties will have default values. So
        // we get them from renderer and set these properties to model element, which will be passed to forms logic.
        modelElement.setProperty(Property.FONT_PROVIDER, this.<FontProvider>getProperty(Property.FONT_PROVIDER));
        modelElement.setProperty(Property.RENDERING_MODE, this.<RenderingMode>getProperty(Property.RENDERING_MODE));
        final PdfSignatureFormField sigField = new SignatureFormFieldBuilder(doc, name).setWidgetRectangle(area)
                .createSignature();
        sigField.disableFieldRegeneration();
        sigField.setFont(font).setFontSize(fontSizeValue);
        sigField.getFirstFormAnnotation().setBackgroundColor(backgroundColor);
        applyDefaultFieldProperties(sigField);
        sigField.getFirstFormAnnotation().setFormFieldElement((SignatureFieldAppearance) modelElement);
        sigField.enableFieldRegeneration();
        PdfAcroForm forms = PdfFormCreator.getAcroForm(doc, true);
        forms.addField(sigField, page);

        writeAcroFormFieldLangAttribute(doc);
    }

    private void adjustChildrenLayout(SignatureFieldAppearance.RenderingMode renderingMode,
                                      Rectangle signatureRect, Rectangle descriptionRect, int pageNum) {
        switch (renderingMode) {
            case NAME_AND_DESCRIPTION: {
                ParagraphRenderer name = (ParagraphRenderer) flatRenderer.getChildRenderers().get(0);
                relayoutParagraph(name, signatureRect, pageNum);

                ParagraphRenderer description = (ParagraphRenderer) flatRenderer.getChildRenderers().get(1);
                relayoutParagraph(description, descriptionRect, pageNum);
                break;
            }
            case GRAPHIC_AND_DESCRIPTION: {
                relayoutImage(signatureRect, pageNum);

                ParagraphRenderer description = (ParagraphRenderer) flatRenderer.getChildRenderers().get(1);
                relayoutParagraph(description, descriptionRect, pageNum);
                break;
            }
            case GRAPHIC: {
                relayoutImage(signatureRect, pageNum);
                break;
            }
            default: {
                ParagraphRenderer description = (ParagraphRenderer) flatRenderer.getChildRenderers().get(0);
                relayoutParagraph(description, descriptionRect, pageNum);
                break;
            }
        }
        // Apply vertical alignment for children including floats.
        VerticalAlignment verticalAlignment = this.<VerticalAlignment>getProperty(Property.VERTICAL_ALIGNMENT);
        float multiplier = 0;
        if (VerticalAlignment.MIDDLE == verticalAlignment) {
            multiplier = 0.5f;
        } else if (VerticalAlignment.BOTTOM == verticalAlignment) {
            multiplier = 1;
        }
        float lowestChildBottom = getLowestChildBottom(flatRenderer, getInnerAreaBBox().getTop());
        float deltaY = lowestChildBottom - getInnerAreaBBox().getY();
        if (deltaY > 0) {
            flatRenderer.move(0, -deltaY * multiplier);
        }
    }

    private void relayoutImage(Rectangle signatureRect, int pageNum) {
        ImageRenderer image = (ImageRenderer) flatRenderer.getChildRenderers().get(0);
        Rectangle imageBBox = image.getOccupiedArea().getBBox();
        float imgWidth = imageBBox.getWidth();
        if (imgWidth < EPS) {
            imgWidth = signatureRect.getWidth();
        }
        float imgHeight = imageBBox.getHeight();
        if (imgHeight < EPS) {
            imgHeight = signatureRect.getHeight();
        }
        float multiplierH = signatureRect.getWidth() / imgWidth;
        float multiplierW = signatureRect.getHeight() / imgHeight;
        float multiplier = Math.min(multiplierH, multiplierW);
        imgWidth *= multiplier;
        imgHeight *= multiplier;
        float x = signatureRect.getLeft() + (signatureRect.getWidth() - imgWidth) / 2;
        float y = signatureRect.getBottom() + (signatureRect.getHeight() - imgHeight) / 2;
        // We need to re-layout image since signature was divided into 2 parts and bBox was changed.
        LayoutContext layoutContext = new LayoutContext(
                new LayoutArea(pageNum, new Rectangle(x, y, imgWidth, imgHeight)));
        image.getModelElement().setProperty(Property.WIDTH, UnitValue.createPointValue(imgWidth));
        image.getModelElement().setProperty(Property.HEIGHT, UnitValue.createPointValue(imgHeight));
        image.layout(layoutContext);
    }

    private void relayoutParagraph(IRenderer renderer, Rectangle rect, int pageNum) {
        UnitValue fontSizeAsUV = this.hasOwnProperty(Property.FONT_SIZE) ?
                (UnitValue) this.<UnitValue>getOwnProperty(Property.FONT_SIZE) :
                (UnitValue) modelElement.<UnitValue>getOwnProperty(Property.FONT_SIZE);
        if (fontSizeAsUV == null || fontSizeAsUV.getValue() < EPS || isFontSizeApproximated) {
            // Calculate font size.
            IRenderer helper = ((Paragraph) renderer.getModelElement()).createRendererSubTree()
                    .setParent(renderer.getParent());
            this.deleteProperty(Property.FONT_SIZE);
            LayoutContext layoutContext = new LayoutContext(new LayoutArea(pageNum, rect));
            float lFontSize = 0.1f, rFontSize = 100;
            int numberOfIterations = 15;
            // 15 iterations with lFontSize = 0.1 and rFontSize = 100 should result in ~0.003 precision.
            float fontSize = calculateFittingFontSize(helper, lFontSize, rFontSize, layoutContext, numberOfIterations);
            renderer.getModelElement().setProperty(Property.FONT_SIZE, UnitValue.createPointValue(fontSize));
        }
        // Relayout the element after font size was changed or signature was split into 2 parts.
        LayoutContext layoutContext = new LayoutContext(new LayoutArea(pageNum, rect));
        renderer.layout(layoutContext);
    }

    private void applyBackgroundImage(SignatureFieldAppearance modelElement) {
        if (modelElement.getImage() != null) {
            BackgroundRepeat repeat = new BackgroundRepeat(BackgroundRepeat.BackgroundRepeatValue.NO_REPEAT);
            BackgroundPosition position = new BackgroundPosition()
                    .setPositionX(BackgroundPosition.PositionX.CENTER)
                    .setPositionY(BackgroundPosition.PositionY.CENTER);
            BackgroundSize size = new BackgroundSize();
            if (Math.abs(modelElement.getImageScale()) < EPS) {
                size.setBackgroundSizeToValues(UnitValue.createPercentValue(100),
                        UnitValue.createPercentValue(100));
            } else {
                float imageScale = modelElement.getImageScale();
                if (imageScale < 0) {
                    size.setBackgroundSizeToContain();
                } else {
                    size.setBackgroundSizeToValues(
                            UnitValue.createPointValue(imageScale * modelElement.getImage().getWidth()),
                            UnitValue.createPointValue(imageScale * modelElement.getImage().getHeight()));
                }
            }
            modelElement.setBackgroundImage(new BackgroundImage.Builder()
                    .setImage(new PdfImageXObject(modelElement.getImage()))
                    .setBackgroundSize(size)
                    .setBackgroundRepeat(repeat)
                    .setBackgroundPosition(position)
                    .build());
        }
    }

    private float calculateAdditionalHeight() {
        Rectangle dummy = new Rectangle(0, 0);
        this.applyMargins(dummy, true);
        this.applyBorderBox(dummy, true);
        this.applyPaddings(dummy, true);
        return dummy.getHeight();
    }

    private void approximateFontSizeToFitLayoutArea(LayoutContext layoutContext) {
        if (this.hasOwnProperty(Property.FONT_SIZE) || modelElement.hasOwnProperty(Property.FONT_SIZE)) {
            return;
        }
        if (SignatureFieldAppearance.RenderingMode.GRAPHIC ==
                ((SignatureFieldAppearance) modelElement).getRenderingMode() ||
                SignatureFieldAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION ==
                        ((SignatureFieldAppearance) modelElement).getRenderingMode()) {
            // We can expect CLIP_ELEMENT log messages since the initial image size may be larger than the field height.
            // But image size will be adjusted during its relayout in #adjustFieldLayout.
            return;
        }
        float fontSize = approximateFontSize(layoutContext, 0.1f, AbstractPdfFormField.DEFAULT_FONT_SIZE);
        if (fontSize > 0) {
            isFontSizeApproximated = true;
            modelElement.setProperty(Property.FONT_SIZE, UnitValue.createPointValue(fontSize));
        }
    }
}