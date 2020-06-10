package com.kartographia.map;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.util.*;

//******************************************************************************
//**  HeatMap Class
//******************************************************************************
/**
 *   Used to generate heatmaps using an array of points. Credit:
 *   https://security-consulting.icu/blog/2012/01/java-heatmap-example/
 *
 ******************************************************************************/

public class HeatMap {

    private ArrayList<int[]> points; //array of x,y coordinates and a count
    private int maxOccurance = 1;
    private float intensity = 1f;
    private int[] colors;
    private int radius = 32;
    private float blur = 1f;
    private int width;
    private int height;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public HeatMap(int width, int height) {
        this.width = width;
        this.height = height;
        points = new ArrayList<>();
        setColors(Color.black, Color.cyan, Color.green, Color.yellow, Color.red);
    }


  //**************************************************************************
  //** setRadius
  //**************************************************************************
  /** Used to set the size of individual points in the heatmap
   */
    public void setRadius(int radius){
        if (radius<1) return;
        this.radius = radius;
    }


  //**************************************************************************
  //** setBlur
  //**************************************************************************
  /** Used to set the percent blur to apply to individual points. Accepts a
   *  value between 0.0-1.0 (Default is 1).
   */
    public void setBlur(float blur){
        if (blur>1 || blur <= 0) return;
        this.blur = blur;
    }


  //**************************************************************************
  //** getColors
  //**************************************************************************
    public int[] getColors(){
        return colors;
    }


  //**************************************************************************
  //** setColors
  //**************************************************************************
  /** Used to set colors for the heatmap, from cold to hot
   */
    public void setColors(Color... c){
        if (c == null || c.length <= 0) return;


        int numSteps = c.length-1;
        int stepSize = (int) Math.round(500.0/(double)numSteps);
        int numColors = stepSize*numSteps;

        colors = new int[numColors];
        int x = 0;

        for (int i=0; i<numSteps; i++){
            Color currColor = c[i];
            Color nextColor = c[i+1];
            for (int j=0; j<stepSize; j++){
                float ratio = (float)j/(float)stepSize;
                Color color = blend(currColor, nextColor, ratio);
                if (i==0){
                    int opacity = (int) Math.floor(ratio*255.0);
                    color = new Color(color.getRed(),color.getGreen(),color.getBlue(),opacity);
                }
                colors[x] = color.getRGB();
                x++;
            }
        }
    }


  //**************************************************************************
  //** setColors
  //**************************************************************************
  /** Used to set colors for the heatmap, from cold to hot
   *  @param hex Hex colors (e.g. "#fff", "#ff6e00", "#ffa300", "#ffd200", "#fff")
   */
    public void setColors(String... hex){
        if (hex == null || hex.length <= 0) return;
        ArrayList<Color> arr = new ArrayList<>();
        for (String str : hex){
            if (!str.startsWith("#")) continue;
            if (str.length()==4) str += str.substring(1);
            arr.add(Color.decode(str));
        }
        setColors(arr.toArray(new Color[arr.size()]));
    }


  //**************************************************************************
  //** setColors
  //**************************************************************************
  /** Used to set colors for the heatmap, from cold to hot
   *  @param colors An array of rgb values
   */
    public void setColors(int[] colors){
        this.colors = colors;
    }


  //**************************************************************************
  //** setColors
  //**************************************************************************
  /** Used to set colors for the heatmap, from cold to hot
   *  @param image An image of a color ramp
   *  @param useRows If true, scans the image from top to bottom to generate
   *  a list of colors. Otherwise, scans the image from left to right.
   */
    public void setColors(BufferedImage image, boolean useRows){
        if (useRows){
            int h = image.getHeight();
            colors = new int[h];
            for (int i=0; i<h; i++){
                colors[i] = image.getRGB(0, i);
            }
        }
        else {
            int w = image.getWidth();
            colors = new int[w];
            for (int i=0; i<w; i++){
                colors[i] = image.getRGB(0, i);
            }
        }
    }


  //**************************************************************************
  //** addPoints
  //**************************************************************************
    public void addPoints(final List<Point> points) {
        Map<Integer, List<Point>> map = new HashMap<>();


        final int pointSize = points.size();
        for (int i = 0; i < pointSize; i++) {
            final Point point = points.get(i);
            // add point to correct list.
            final int hash = getkey(point);
            if (map.containsKey(hash)) {
                final List<Point> thisList = map.get(hash);
                thisList.add(point);
            } else {
                final List<Point> newList = new LinkedList<>();
                newList.add(point);
                map.put(hash, newList);
            }
        }

        Iterator<List<Point>> iterator = map.values().iterator();
        while (iterator.hasNext()) {
            final List<Point> currentPoints = iterator.next();
            Point point = currentPoints.get(0);
            int count = currentPoints.size();
            if (count>maxOccurance) maxOccurance = count;
            this.points.add(new int[]{point.x, point.y, count});
        }
    }


  //**************************************************************************
  //** addPoints
  //**************************************************************************
    public void addPoints(ArrayList<int[]> points){
        for (int[] point : points){
            int count = point[2];
            if (count>maxOccurance) maxOccurance = count;
            this.points.add(point);
        }
    }


  //**************************************************************************
  //** setMaxOccurance
  //**************************************************************************
  /** Used to set/override the maxOccurance for the points. Normally, the
   *  value is computed automatically. But in some cases (e.g. tiling), you
   *  might want to control the value.
   */
    public void setMaxOccurance(int maxOccurance){
        if (maxOccurance <= 0) return;
        this.maxOccurance = maxOccurance;
    }


  //**************************************************************************
  //** createHeatMap
  //**************************************************************************
  /** Used to generate a heatmap and return an image
   */
    public BufferedImage createHeatMap() {

      //Create white image
        BufferedImage heatMap = new BufferedImage(width, height, 6);
        Graphics2D g2 = heatMap.createGraphics();
        g2.setColor(Color.white);
        g2.fillRect(0, 0, heatMap.getWidth(), heatMap.getHeight());
        g2.dispose();

      //Create circle image
        int w = radius*2;
        int h = w;

        Point2D center = new Point2D.Float(radius, radius);

        float dist[] = { 0.0f, 0.1f, 1.0f };
        int finalOpacity = (int) Math.round(255-(255*blur));
        Color colors[] = {
            new Color(0,0,0,255),
            new Color(0,0,0,255),
            new Color(0,0,0,finalOpacity)
        };

        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);

        BufferedImage circle = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = circle.createGraphics();
        g.setPaint(p);
        g.fillRect(0, 0, w, h);
        g.dispose();



      //Draw circles
        for (int[] point : points){
            int x = point[0];
            int y = point[1];
            int count = point[2];

            float opacity = count / (float) maxOccurance;
            opacity = opacity * intensity;
            if (opacity > 1) opacity = 1;

            addImage(heatMap, circle, opacity, (x - radius), (y - radius));
        }


        // negate the image
        heatMap = negateImage(heatMap);


        // remap black/white with colors
        remap(heatMap);



        return heatMap;
    }


  //**************************************************************************
  //** getIntensity
  //**************************************************************************
    public float getIntensity(){
        return intensity;
    }


  //**************************************************************************
  //** setIntensity
  //**************************************************************************
  /** Used to tweak the opacity for each point in the heatmap. Smaller values
   *  make the points less intense and appear with cooler colors. Accepts a
   *  value between 0.0-1.0 (Default is 1).
   */
    public void setIntensity(float intensity){
        if (intensity>1 || intensity <= 0) return;
        this.intensity = intensity;
    }


  //**************************************************************************
  //** remap
  //**************************************************************************
  /** Remaps black and white picture with colors. The whiter a pixel is, the
   *  more it will get a color from the bottom of it. Black will stay black.
   *  @param heatMap black and white heat map
   */
    private void remap(BufferedImage heatMap) {

        final int width = heatMap.getWidth();
        final int height = heatMap.getHeight();
        final int numColors = colors.length-1;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {

                // get heatMapBW color values:
                final int rGB = heatMap.getRGB(i, j);

                // calculate multiplier to be applied to height of gradiant.
                float multiplier = rGB & 0xff; // blue
                multiplier *= ((rGB >>> 8)) & 0xff; // green
                multiplier *= (rGB >>> 16) & 0xff; // red
                multiplier /= 16581375; // 255f * 255f * 255f

                // apply multiplier
                final int idx = (int) Math.round(multiplier * numColors);


                // calculate new value based on whiteness of heatMap
                // (the whiter, the more a color from the top of colorGradiant
                // will be chosen.
                int rgb = colors[idx];
                heatMap.setRGB(i, j, rgb);
            }
        }
    }


  //**************************************************************************
  //** negateImage
  //**************************************************************************
  /** returns a negated version of an image
   */
    private BufferedImage negateImage(final BufferedImage img) {
        final int width = img.getWidth();
        final int height = img.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                final int rGB = img.getRGB(x, y);

                // Swaps values
                // i.e. 255, 255, 255 (white)
                // becomes 0, 0, 0 (black)
                final int r = Math.abs(((rGB >>> 16) & 0xff) - 255); // red
                                                                     // inverted
                final int g = Math.abs(((rGB >>> 8) & 0xff) - 255); // green
                                                                    // inverted
                final int b = Math.abs((rGB & 0xff) - 255); // blue inverted

                // transform back to pixel value and set it
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }


  //**************************************************************************
  //** addImage
  //**************************************************************************
  /** prints the contents of buff2 on buff1 with the given opaque value.
     *
     * @param buff1
     *            buffer
     * @param buff2
     *            buffer
     * @param opaque
     *            how opaque the second buffer should be drawn
     * @param x
     *            x position where the second buffer should be drawn
     * @param y
     *            y position where the second buffer should be drawn
     */
    private void addImage(final BufferedImage buff1, final BufferedImage buff2,
            final float opaque, final int x, final int y) {
        final Graphics2D g2d = buff1.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                opaque));
        g2d.drawImage(buff2, x, y, null);
        g2d.dispose();
    }


  //**************************************************************************
  //** getkey
  //**************************************************************************
  /** Returns a hash for a given point
   */
    private int getkey(final Point p) {
        return ((p.x << 19) | (p.y << 7));
    }


  //**************************************************************************
  //** blend
  //**************************************************************************
  /** Used to blend 2 colors together. Credit:
   *  https://stackoverflow.com/a/20332789
   */
    private Color blend( Color c1, Color c2, float ratio ) {
        if ( ratio > 1f ) ratio = 1f;
        else if ( ratio < 0f ) ratio = 0f;
        float iRatio = 1.0f - ratio;

        int i1 = c1.getRGB();
        int i2 = c2.getRGB();

        int a1 = (i1 >> 24 & 0xff);
        int r1 = ((i1 & 0xff0000) >> 16);
        int g1 = ((i1 & 0xff00) >> 8);
        int b1 = (i1 & 0xff);

        int a2 = (i2 >> 24 & 0xff);
        int r2 = ((i2 & 0xff0000) >> 16);
        int g2 = ((i2 & 0xff00) >> 8);
        int b2 = (i2 & 0xff);

        int a = (int)((a1 * iRatio) + (a2 * ratio));
        int r = (int)((r1 * iRatio) + (r2 * ratio));
        int g = (int)((g1 * iRatio) + (g2 * ratio));
        int b = (int)((b1 * iRatio) + (b2 * ratio));

        return new Color( a << 24 | r << 16 | g << 8 | b );
    }
}