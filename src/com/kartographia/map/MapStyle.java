package com.kartographia.map;
import java.awt.Color;
import java.awt.Font;

//******************************************************************************
//**  MapStyle
//******************************************************************************
/**
 *   Used to encapsulate style information for rendering points, lines,
 *   polygons, and text on a map.
 *
 ******************************************************************************/

public class MapStyle {

    private Color color;
    private Color borderColor;
    private Float borderWidth;
    private Font font;
    private String align = "center";
    private String valign = "middle";
    private Integer textWrap;


    public MapStyle clone(){
        MapStyle style = new MapStyle();
        style.color = color;
        style.borderColor = borderColor;
        style.borderWidth = borderWidth;
        style.font = font;
        style.align = align;
        style.valign = valign;
        style.textWrap = textWrap;
        return style;
    }

    public Color getColor(){
        return color;
    }

    public void setColor(Color color){
        this.color = color;
    }

    public void setBorderColor(Color color){
        borderColor = color;
    }

    public Color getBorderColor(){
        return borderColor;
    }

    public void setBorderWidth(Float width){
        if (width!=null && width<0) return;
        borderWidth = width;
    }

    public Float getBorderWidth(){
        return borderWidth;
    }

    public void setFont(String fontName, int fontSize){
        font = new Font(fontName, Font.TRUETYPE_FONT, fontSize);
    }

    public Font getFont(){
        return font;
    }

    public void setTextAlign(String align){
        if (align==null) return;
        align = align.trim().toLowerCase();
        if (align.equals("left") || align.equals("center") || align.equals("right")){
            this.align = align;
        }
    }

    public String getTextAlign(){
        return align;
    }

    public void setTextVAlign(String valign){
        if (valign==null) return;
        valign = valign.trim().toLowerCase();
        if (valign.equals("top") || valign.equals("middle") || valign.equals("bottom")){
            this.valign = valign;
        }
    }

    public String getTextVAlign(){
        return valign;
    }

    public void setTextWrap(Integer pixels){
        if (pixels!=null && pixels<1) return;
        textWrap = pixels;
    }

    public Integer getTextWrap(){
        return textWrap;
    }

    public static Color getColor(String hex){
        if (hex!=null){
            if (hex.startsWith("#")){
                if (hex.length()==4) hex += hex.substring(1);
                return Color.decode(hex);
            }
        }
        return Color.BLACK;
    }

    public static String[] getAvailableFonts(){
        return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }
}