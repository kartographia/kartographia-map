package com.kartographia.map;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.PathIterator;
import java.awt.geom.QuadCurve2D;
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
  //** HeatMap
  //**************************************************************************
    public HeatMap(List<Point> points, int radius){
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;


        HashMap<Integer, int[]> counts = new HashMap<>();
        for (Point point : points){
            int x = point.x;
            int y = point.y;

            minX = Math.min(x, minX);
            maxX = Math.max(x, maxX);
            minY = Math.min(y, minY);
            maxY = Math.max(y, maxY);

            int hash = getkey(point);
            int[] pt = counts.get(hash);
            if (pt==null){
                pt = new int[]{point.x, point.y, 1};
                counts.put(hash, pt);
            }
            else {
                pt[2] = pt[2]+1;
            }
            if (pt[2]>maxOccurance) maxOccurance = pt[2];
        }

        width = (int)Math.round(Math.abs(maxX-minX));
        height = (int)Math.round(Math.abs(maxY-minY));
        this.radius = radius;
        this.points = new ArrayList<>();
        Iterator<Integer> it = counts.keySet().iterator();
        while (it.hasNext()) this.points.add(counts.get(it.next()));
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
  //** getRadius
  //**************************************************************************
    public int getRadius(){
        return radius;
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
  //** getBufferedImage
  //**************************************************************************
  /** Used to generate a heatmap and return an image
   */
    public BufferedImage getBufferedImage() {

      //Create white image
        BufferedImage heatMap = new BufferedImage(width, height, 6);
        Graphics2D g2 = heatMap.createGraphics();
        if (this.colors!=null){
            g2.setColor(Color.white);
            g2.fillRect(0, 0, heatMap.getWidth(), heatMap.getHeight());
        }


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


            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2.drawImage(circle, (x - radius), (y - radius), null);
        }
        g2.dispose();


      //Apply colors
        if (this.colors!=null){

          //Negate the image
            heatMap = negateImage(heatMap);


          //Remap black/white with colors
            remap(heatMap);
        }

        return heatMap;
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
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                int r = Math.abs(((rgb >>> 16) & 0xff) - 255); // red inverted
                int g = Math.abs(((rgb >>> 8) & 0xff) - 255); // green inverted
                int b = Math.abs((rgb & 0xff) - 255); // blue inverted
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return img;
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


  //**************************************************************************
  //** getContours
  //**************************************************************************
  /** Returns polygons used to represent density threshold values. Example:
   <pre>
        HeatMap heatmap = new HeatMap(arr, radius);
        HeatMap.Contour[] contours = heatmap.getContours();
        BufferedImage bi = heatmap.getBufferedImage();
        Graphics2D g2 = (Graphics2D) bi.getGraphics();
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));
        for (HeatMap.Contour contour : contours){
            ArrayList&lt;ArrayList<double[]>> polygons = contour.getPolygons();
            for (ArrayList&lt;double[]> coordinates : polygons){
                for (int i=1; i&lt;coordinates.size(); i++){
                    double[] prevCoord = coordinates.get(i-1);
                    double[] currCoord = coordinates.get(i);
                    int x1 = (int) Math.round(prevCoord[0]);
                    int y1 = (int) Math.round(prevCoord[1]);
                    int x2 = (int) Math.round(currCoord[0]);
                    int y2 = (int) Math.round(currCoord[1]);
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
        }
   </pre>
   * @param percentiles Optional. A range of percentile values used to create
   * contours (e.g. 80, 20, 0) where the 80th percentile represents the top 20%
   */
    public Contour[] getContours(Double... percentiles){

      //Temporarily update size, coordinates, colors, etc and get heatmap
        float blur = radius*1f;
        int offset = (int) Math.ceil(radius+blur);
        int buffer = 2*offset;
        width += buffer;
        height += buffer;
        ArrayList<int[]> org = points;
        points = new ArrayList<>();
        for (int[] point : org){
            points.add(new int[]{point[0]+offset, point[1]+offset, point[2]});
        }
        int[] colors = this.colors;
        this.colors = null;
        setColors("#fff", "#ff0000");
        BufferedImage bi = getBufferedImage();




      //Apply guassian blur
        javaxt.io.Image img = new javaxt.io.Image(bi);
        img.blur(blur);
        bi = img.getBufferedImage();
        //img.saveAs("/temp/gypsy/contours.png");


      //Generate list of alpha values for each point
        ArrayList<Integer> alphas = new ArrayList<>();
        int minA = Integer.MAX_VALUE;
        int maxA = Integer.MIN_VALUE;
        for (int[] point : points) {
            int pixel = bi.getRGB(point[0], point[1]);
            int alpha = (pixel >> 24) & 0xff;

            alphas.add(alpha);
            minA = Math.min(alpha, minA);
            maxA = Math.max(alpha, maxA);
        }
        Collections.sort(alphas);



      //Compute breakpoints for the contours using the alpha values
        int[] steps;
        if (percentiles.length==0){
            steps = new int[]{
                getAlpha(80, alphas), //get top 20% of alpha values
                0,
                minA
            };
            steps[1] = ((steps[0]-minA)/2)+minA;
        }
        else{
            steps = new int[percentiles.length];
            for (int i=0; i<steps.length; i++){
                double percentile = percentiles[i];
                if (percentile>0){
                    steps[i] = getAlpha(percentile, alphas);
                }
                else{
                    steps[i] = minA;
                }
            }
        }
        //System.out.println("steps: " + java.util.Arrays.toString(steps));



      //Generate contours for each step
        Contour[] contours = new Contour[steps.length];
        for (int i=0; i<steps.length; i++){
            int alpha = steps[i];
            ArrayList<ArrayList<double[]>> polygons = getContours(bi, alpha);
            for (ArrayList<double[]> coordinates : polygons){
                for (int j=0; j<coordinates.size(); j++){
                    double[] coord = coordinates.get(j);
                    coord[0] = coord[0]-offset;
                    coord[1] = coord[1]-offset;
                }
            }
            contours[i] = new Contour(polygons);
        }



      //Reset size, coordinates, colors, etc
        width = width-offset;
        height = height-offset;
        points = org;
        setColors(colors);


        return contours;
    }


  //**************************************************************************
  //** Contour Class
  //**************************************************************************
    public class Contour {
        private ArrayList<ArrayList<double[]>> polygons;
        private Contour(ArrayList<ArrayList<double[]>> polygons){
            this.polygons = polygons;
        }
        public ArrayList<ArrayList<double[]>> getPolygons(){
            return polygons;
        }
    }


  //**************************************************************************
  //** getContours
  //**************************************************************************
    private ArrayList<ArrayList<double[]>> getContours(BufferedImage bi, int step){
        long s = System.currentTimeMillis();
        ArrayList<ArrayList<double[]>> polygons = new ArrayList<>();

        int width = bi.getWidth();
        int height = bi.getHeight();


      //Creating indexed color array which has a boundary filled with -1 in every direction
        int [][] arr = new int[height+2][width+2];
        for(int j=0; j<(height+2); j++){ arr[j][0] = -1; arr[j][width+1 ] = -1; }
        for(int i=0; i<(width+2) ; i++){ arr[0][i] = -1; arr[height+1][i] = -1; }



        for (int y=0; y<height; y++){
            for (int x=0; x<width; x++){

                int pixel = bi.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;

                int ci= alpha>=step ? 1 : 0;
                arr[y+1][x+1] = ci;
            }
        }


        //System.out.println("Created " + width + "x" + height + " array in " + (System.currentTimeMillis()-s) + "ms");
        //s = System.currentTimeMillis();


      //Vectorize image
        HashMap<String,Float> tracingOptions = new HashMap<>();
        tracingOptions.put("ltres", 2f); // Linear error treshold
        tracingOptions.put("qtres", 2f); // Quadratic spline error treshold

        ArrayList<ArrayList<ArrayList<Double[]>>> layers =
        ImageTracer.imagedataToTracedata(new ImageTracer.IndexedImage(arr, palette), tracingOptions).layers;


        //System.out.println("Vectorized image in " + (System.currentTimeMillis()-s) + "ms");
        //s = System.currentTimeMillis();

        for (ArrayList<ArrayList<Double[]>> layer : layers){
            if (layer.isEmpty()) continue;

            for (ArrayList<Double[]> lines : layer){
                ArrayList<double[]> coordinates = new ArrayList<>();
                for (Double[] lineInfo : lines){

                    int lineType = lineInfo[0].intValue();
                    if (lineType==1){ //straight line

                        double x1 = lineInfo[1];
                        double y1 = lineInfo[2];
                        double x2 = lineInfo[3];
                        double y2 = lineInfo[4];
                        coordinates.add(new double[]{x1, y1});
                        coordinates.add(new double[]{x2, y2});

                    }
                    else if (lineType==2){ //quadratic spline

                        double x1 = lineInfo[1];
                        double y1 = lineInfo[2];
                        double cx = lineInfo[3]; //spline control point
                        double cy = lineInfo[4]; //spline control point
                        double x2 = lineInfo[5];
                        double y2 = lineInfo[6];
                        QuadCurve2D quadCurve = new QuadCurve2D.Double(x1, y1, cx, cy, x2, y2);
                        PathIterator it = quadCurve.getPathIterator(null, 0.5);
                        while (!it.isDone()){
                            double[] coords = new double[2];
                            it.currentSegment(coords);
                            coordinates.add(coords);
                            it.next();
                        }
                    }
                }


                if (coordinates.size()>2){
                    double[] firstPoint = coordinates.get(0);
                    double[] lastPoint = coordinates.get(coordinates.size()-1);
                    int x1 = (int) Math.round(firstPoint[0]);
                    int y1 = (int) Math.round(firstPoint[1]);
                    int x2 = (int) Math.round(lastPoint[0]);
                    int y2 = (int) Math.round(lastPoint[1]);
                    if (x1==x2 && y1==y2){
                        if (lines.size()==4 && (x1==0 && y1==0) || (x1==1 && y1==0) || (x1==0 && y1==1)){
                            //image outline?
                        }
                        else{
                            polygons.add(coordinates);
                        }
                    }
                }
            }
        }

        //System.out.println("Extracted coords in " + (System.currentTimeMillis()-s) + "ms");
        return polygons;
    }


  //**************************************************************************
  //** ImageTracer Class
  //**************************************************************************
  /** Raster image tracer and vectorizer by András Jankovics. This class is a
   *  stripped down version of the 1.1.2 release. The original source code can
   *  be found here: https://github.com/jankovicsandras/imagetracerjava
   */
    private static class ImageTracer {
        private ImageTracer(){}


	// Container for the color-indexed image before and tracedata after vectorizing
	public static class IndexedImage{
            public int width, height;
            public int [][] array; // array[x][y] of palette colors
            public byte [][] palette;// array[palettelength][4] RGBA color palette
            public ArrayList<ArrayList<ArrayList<Double[]>>> layers;// tracedata

            public IndexedImage(int [][] marray, byte [][] mpalette){
                array = marray;
                palette = mpalette;
                width = marray[0].length-2;
                height = marray.length-2;// Color quantization adds +2 to the original width and height
            }
	}


	// https://developer.mozilla.org/en-US/docs/Web/API/ImageData
	private static class ImageData{
            public int width, height;
            public byte[] data; // raw byte data: R G B A R G B A ...
            public ImageData(int mwidth, int mheight, byte[] mdata){
                width = mwidth;
                height = mheight;
                data = mdata;
            }
	}


	// The bitshift method in loadImageData creates signed bytes where -1 -> 255 unsigned ; -128 -> 128 unsigned ;
	// 127 -> 127 unsigned ; 0 -> 0 unsigned ; These will be converted to -128 (representing 0 unsigned) ...
	// 127 (representing 255 unsigned) and tosvgcolorstr will add +128 to create RGB values 0..255
	public static byte bytetrans (byte b){
            if(b<0){ return (byte)(b+128); }else{ return (byte)(b-128); }
	}


	public static IndexedImage imagedataToTracedata (IndexedImage ii, HashMap<String,Float> options){
            options = checkoptions(options);

            // 2. Layer separation and edge detection
            //long s = System.currentTimeMillis();
            int[][][] rawlayers = layering(ii);
            //System.out.println(" - layering took " + (System.currentTimeMillis()-s) + "ms");


            // 3. Batch pathscan
            ArrayList<ArrayList<ArrayList<Integer[]>>> bps = batchpathscan(rawlayers,(int)(Math.floor(options.get("pathomit"))));



            // 4. Batch interpollation
            ArrayList<ArrayList<ArrayList<Double[]>>> bis = batchinternodes(bps);



            // 5. Batch tracing
            ii.layers = batchtracelayers(bis,options.get("ltres"),options.get("qtres"));


            return ii;
	}// End of imagedataToTracedata()


	// creating options object, setting defaults for missing values
	private static HashMap<String,Float> checkoptions (HashMap<String,Float> options){
		if(options==null){ options = new HashMap<String,Float>(); }
		// Tracing
		if(!options.containsKey("ltres")){ options.put("ltres",1f); }
		if(!options.containsKey("qtres")){ options.put("qtres",1f); }
		if(!options.containsKey("pathomit")){ options.put("pathomit",8f); }
		// Color quantization
		if(!options.containsKey("colorsampling")){ options.put("colorsampling",1f); }
		if(!options.containsKey("numberofcolors")){ options.put("numberofcolors",16f); }
		if(!options.containsKey("mincolorratio")){ options.put("mincolorratio",0.02f); }
		if(!options.containsKey("colorquantcycles")){ options.put("colorquantcycles",3f); }
		// SVG rendering
		if(!options.containsKey("scale")){ options.put("scale",1f); }
		if(!options.containsKey("simplifytolerance")){ options.put("simplifytolerance",0f); }
		if(!options.containsKey("roundcoords")){ options.put("roundcoords",1f); }
		if(!options.containsKey("lcpr")){ options.put("lcpr",0f); }
		if(!options.containsKey("qcpr")){ options.put("qcpr",0f); }
		if(!options.containsKey("desc")){ options.put("desc",1f); }
		if(!options.containsKey("viewbox")){ options.put("viewbox",0f); }
		// Blur
		if(!options.containsKey("blurradius")){ options.put("blurradius",0f); }
		if(!options.containsKey("blurdelta")){ options.put("blurdelta",20f); }

		return options;
	}// End of checkoptions()





	// 2. Layer separation and edge detection
	// Edge node types ( ▓:light or 1; ░:dark or 0 )
	// 12  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓
	// 48  ░░  ░░  ░░  ░░  ░▓  ░▓  ░▓  ░▓  ▓░  ▓░  ▓░  ▓░  ▓▓  ▓▓  ▓▓  ▓▓
	//     0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15
	//
	private static int[][][] layering (IndexedImage ii){
		// Creating layers for each indexed color in arr
		int val=0, aw = ii.array[0].length, ah = ii.array.length, n1,n2,n3,n4,n5,n6,n7,n8;
		int[][][] layers = new int[ii.palette.length][ah][aw];

		// Looping through all pixels and calculating edge node type
		for(int j=1; j<(ah-1); j++){
			for(int i=1; i<(aw-1); i++){

				// This pixel's indexed color
				val = ii.array[j][i];

				// Are neighbor pixel colors the same?
				n1 = ii.array[j-1][i-1]==val ? 1 : 0;
				n2 = ii.array[j-1][i  ]==val ? 1 : 0;
				n3 = ii.array[j-1][i+1]==val ? 1 : 0;
				n4 = ii.array[j  ][i-1]==val ? 1 : 0;
				n5 = ii.array[j  ][i+1]==val ? 1 : 0;
				n6 = ii.array[j+1][i-1]==val ? 1 : 0;
				n7 = ii.array[j+1][i  ]==val ? 1 : 0;
				n8 = ii.array[j+1][i+1]==val ? 1 : 0;

				// this pixel"s type and looking back on previous pixels
				layers[val][j+1][i+1] = 1 + (n5 * 2) + (n8 * 4) + (n7 * 8) ;
				if(n4==0){ layers[val][j+1][i  ] = 0 + 2 + (n7 * 4) + (n6 * 8) ; }
				if(n2==0){ layers[val][j  ][i+1] = 0 + (n3*2) + (n5 * 4) + 8 ; }
				if(n1==0){ layers[val][j  ][i  ] = 0 + (n2*2) + 4 + (n4 * 8) ; }

			}// End of i loop
		}// End of j loop

		return layers;
	}// End of layering()


	// Lookup tables for pathscan
	private static byte [] pathscan_dir_lookup = {0,0,3,0, 1,0,3,0, 0,3,3,1, 0,3,0,0};
	private static boolean [] pathscan_holepath_lookup = {false,false,false,false, false,false,false,true, false,false,false,true, false,true,true,false };
	// pathscan_combined_lookup[ arr[py][px] ][ dir ] = [nextarrpypx, nextdir, deltapx, deltapy];
	private static byte [][][] pathscan_combined_lookup = {
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}},// arr[py][px]==0 is invalid
			{{ 0, 1, 0,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 2,-1, 0}},
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 1, 0,-1}, { 0, 0, 1, 0}},
			{{ 0, 0, 1, 0}, {-1,-1,-1,-1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}},

			{{-1,-1,-1,-1}, { 0, 0, 1, 0}, { 0, 3, 0, 1}, {-1,-1,-1,-1}},
			{{13, 3, 0, 1}, {13, 2,-1, 0}, { 7, 1, 0,-1}, { 7, 0, 1, 0}},
			{{-1,-1,-1,-1}, { 0, 1, 0,-1}, {-1,-1,-1,-1}, { 0, 3, 0, 1}},
			{{ 0, 3, 0, 1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}, {-1,-1,-1,-1}},

			{{ 0, 3, 0, 1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}, {-1,-1,-1,-1}},
			{{-1,-1,-1,-1}, { 0, 1, 0,-1}, {-1,-1,-1,-1}, { 0, 3, 0, 1}},
			{{11, 1, 0,-1}, {14, 0, 1, 0}, {14, 3, 0, 1}, {11, 2,-1, 0}},
			{{-1,-1,-1,-1}, { 0, 0, 1, 0}, { 0, 3, 0, 1}, {-1,-1,-1,-1}},

			{{ 0, 0, 1, 0}, {-1,-1,-1,-1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}},
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 1, 0,-1}, { 0, 0, 1, 0}},
			{{ 0, 1, 0,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 2,-1, 0}},
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}}// arr[py][px]==15 is invalid
	};


	// 3. Walking through an edge node array, discarding edge node types 0 and 15 and creating paths from the rest.
	// Walk directions (dir): 0 > ; 1 ^ ; 2 < ; 3 v
	// Edge node types ( ▓:light or 1; ░:dark or 0 )
	// ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓
	// ░░  ░░  ░░  ░░  ░▓  ░▓  ░▓  ░▓  ▓░  ▓░  ▓░  ▓░  ▓▓  ▓▓  ▓▓  ▓▓
	// 0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15
	//
	private static ArrayList<ArrayList<Integer[]>> pathscan (int [][] arr,float pathomit){
		ArrayList<ArrayList<Integer[]>> paths = new ArrayList<ArrayList<Integer[]>>();
		ArrayList<Integer[]> thispath;
		int px=0,py=0,w=arr[0].length,h=arr.length,dir=0;
		boolean pathfinished=true, holepath = false;
		byte[] lookuprow;

		for(int j=0;j<h;j++){
			for(int i=0;i<w;i++){
				if((arr[j][i]!=0)&&(arr[j][i]!=15)){

					// Init
					px = i; py = j;
					paths.add(new ArrayList<Integer[]>());
					thispath = paths.get(paths.size()-1);
					pathfinished = false;

					// fill paths will be drawn, but hole paths are also required to remove unnecessary edge nodes
					dir = pathscan_dir_lookup[ arr[py][px] ]; holepath = pathscan_holepath_lookup[ arr[py][px] ];

					// Path points loop
					while(!pathfinished){

						// New path point
						thispath.add(new Integer[3]);
						thispath.get(thispath.size()-1)[0] = px-1;
						thispath.get(thispath.size()-1)[1] = py-1;
						thispath.get(thispath.size()-1)[2] = arr[py][px];

						// Next: look up the replacement, direction and coordinate changes = clear this cell, turn if required, walk forward
						lookuprow = pathscan_combined_lookup[ arr[py][px] ][ dir ];
						arr[py][px] = lookuprow[0]; dir = lookuprow[1]; px += lookuprow[2]; py += lookuprow[3];

						// Close path
						if(((px-1)==thispath.get(0)[0])&&((py-1)==thispath.get(0)[1])){
							pathfinished = true;
							// Discarding 'hole' type paths and paths shorter than pathomit
							if( (holepath) || (thispath.size()<pathomit) ){
								paths.remove(thispath);
							}
						}

					}// End of Path points loop

				}// End of Follow path

			}// End of i loop
		}// End of j loop

		return paths;
	}// End of pathscan()


	// 3. Batch pathscan
	private static ArrayList<ArrayList<ArrayList<Integer[]>>> batchpathscan (int [][][] layers, float pathomit){
		ArrayList<ArrayList<ArrayList<Integer[]>>> bpaths = new ArrayList<ArrayList<ArrayList<Integer[]>>>();
		for (int[][] layer : layers) {
			bpaths.add(pathscan(layer,pathomit));
		}
		return bpaths;
	}


	// 4. interpolating between path points for nodes with 8 directions ( East, SouthEast, S, SW, W, NW, N, NE )
	private static ArrayList<ArrayList<Double[]>> internodes (ArrayList<ArrayList<Integer[]>> paths){
		ArrayList<ArrayList<Double[]>> ins = new ArrayList<ArrayList<Double[]>>();
		ArrayList<Double[]> thisinp;
		Double[] thispoint, nextpoint = new Double[2];
		Integer[] pp1, pp2, pp3;
		int palen=0,nextidx=0,nextidx2=0;

		// paths loop
		for(int pacnt=0; pacnt<paths.size(); pacnt++){
			ins.add(new ArrayList<Double[]>());
			thisinp = ins.get(ins.size()-1);
			palen = paths.get(pacnt).size();
			// pathpoints loop
			for(int pcnt=0;pcnt<palen;pcnt++){

				// interpolate between two path points
				nextidx = (pcnt+1)%palen; nextidx2 = (pcnt+2)%palen;
				thisinp.add(new Double[3]);
				thispoint = thisinp.get(thisinp.size()-1);
				pp1 = paths.get(pacnt).get(pcnt);
				pp2 = paths.get(pacnt).get(nextidx);
				pp3 = paths.get(pacnt).get(nextidx2);
				thispoint[0] = (pp1[0]+pp2[0]) / 2.0;
				thispoint[1] = (pp1[1]+pp2[1]) / 2.0;
				nextpoint[0] = (pp2[0]+pp3[0]) / 2.0;
				nextpoint[1] = (pp2[1]+pp3[1]) / 2.0;

				// line segment direction to the next point
				if(thispoint[0] < nextpoint[0]){
					if     (thispoint[1] < nextpoint[1]){ thispoint[2] = 1.0; }// SouthEast
					else if(thispoint[1] > nextpoint[1]){ thispoint[2] = 7.0; }// NE
					else                                { thispoint[2] = 0.0; } // E
				}else if(thispoint[0] > nextpoint[0]){
					if     (thispoint[1] < nextpoint[1]){ thispoint[2] = 3.0; }// SW
					else if(thispoint[1] > nextpoint[1]){ thispoint[2] = 5.0; }// NW
					else                                { thispoint[2] = 4.0; }// W
				}else{
					if     (thispoint[1] < nextpoint[1]){ thispoint[2] = 2.0; }// S
					else if(thispoint[1] > nextpoint[1]){ thispoint[2] = 6.0; }// N
					else                                { thispoint[2] = 8.0; }// center, this should not happen
				}

			}// End of pathpoints loop
		}// End of paths loop
		return ins;
	}// End of internodes()


	// 4. Batch interpollation
	private static ArrayList<ArrayList<ArrayList<Double[]>>> batchinternodes (ArrayList<ArrayList<ArrayList<Integer[]>>> bpaths){
		ArrayList<ArrayList<ArrayList<Double[]>>> binternodes = new ArrayList<ArrayList<ArrayList<Double[]>>>();
		for(int k=0; k<bpaths.size(); k++) {
			binternodes.add(internodes(bpaths.get(k)));
		}
		return binternodes;
	}


	// 5. tracepath() : recursively trying to fit straight and quadratic spline segments on the 8 direction internode path

	// 5.1. Find sequences of points with only 2 segment types
	// 5.2. Fit a straight line on the sequence
	// 5.3. If the straight line fails (an error>ltreshold), find the point with the biggest error
	// 5.4. Fit a quadratic spline through errorpoint (project this to get controlpoint), then measure errors on every point in the sequence
	// 5.5. If the spline fails (an error>qtreshold), find the point with the biggest error, set splitpoint = (fitting point + errorpoint)/2
	// 5.6. Split sequence and recursively apply 5.2. - 5.7. to startpoint-splitpoint and splitpoint-endpoint sequences
	// 5.7. TODO? If splitpoint-endpoint is a spline, try to add new points from the next sequence

	// This returns an SVG Path segment as a double[7] where
	// segment[0] ==1.0 linear  ==2.0 quadratic interpolation
	// segment[1] , segment[2] : x1 , y1
	// segment[3] , segment[4] : x2 , y2 ; middle point of Q curve, endpoint of L line
	// segment[5] , segment[6] : x3 , y3 for Q curve, should be 0.0 , 0.0 for L line
	//
	// path type is discarded, no check for path.size < 3 , which should not happen

	private static ArrayList<Double[]> tracepath (ArrayList<Double[]> path, float ltreshold, float qtreshold){
		int pcnt=0, seqend=0; double segtype1, segtype2;
		ArrayList<Double[]> smp = new ArrayList<Double[]>();
		//Double [] thissegment;
		int pathlength = path.size();

		while(pcnt<pathlength){
			// 5.1. Find sequences of points with only 2 segment types
			segtype1 = path.get(pcnt)[2]; segtype2 = -1; seqend=pcnt+1;
			while(
					((path.get(seqend)[2]==segtype1) || (path.get(seqend)[2]==segtype2) || (segtype2==-1))
					&& (seqend<(pathlength-1))){
				if((path.get(seqend)[2]!=segtype1) && (segtype2==-1)){ segtype2 = path.get(seqend)[2];}
				seqend++;
			}
			if(seqend==(pathlength-1)){ seqend = 0; }

			// 5.2. - 5.6. Split sequence and recursively apply 5.2. - 5.6. to startpoint-splitpoint and splitpoint-endpoint sequences
			smp.addAll(fitseq(path,ltreshold,qtreshold,pcnt,seqend));
			// 5.7. TODO? If splitpoint-endpoint is a spline, try to add new points from the next sequence

			// forward pcnt;
			if(seqend>0){ pcnt = seqend; }else{ pcnt = pathlength; }

		}// End of pcnt loop

		return smp;

	}// End of tracepath()


	// 5.2. - 5.6. recursively fitting a straight or quadratic line segment on this sequence of path nodes,
	// called from tracepath()
	private static ArrayList<Double[]> fitseq (ArrayList<Double[]> path, float ltreshold, float qtreshold, int seqstart, int seqend){
		ArrayList<Double[]> segment = new ArrayList<Double[]>();
		Double [] thissegment;
		int pathlength = path.size();

		// return if invalid seqend
		if((seqend>pathlength)||(seqend<0)){return segment;}

		int errorpoint=seqstart;
		boolean curvepass=true;
		double px, py, dist2, errorval=0;
		double tl = (seqend-seqstart); if(tl<0){ tl += pathlength; }
		double vx = (path.get(seqend)[0]-path.get(seqstart)[0]) / tl,
				vy = (path.get(seqend)[1]-path.get(seqstart)[1]) / tl;

		// 5.2. Fit a straight line on the sequence
		int pcnt = (seqstart+1)%pathlength;
		double pl;
		while(pcnt != seqend){
			pl = pcnt-seqstart; if(pl<0){ pl += pathlength; }
			px = path.get(seqstart)[0] + (vx * pl); py = path.get(seqstart)[1] + (vy * pl);
			dist2 = ((path.get(pcnt)[0]-px)*(path.get(pcnt)[0]-px)) + ((path.get(pcnt)[1]-py)*(path.get(pcnt)[1]-py));
			if(dist2>ltreshold){curvepass=false;}
			if(dist2>errorval){ errorpoint=pcnt; errorval=dist2; }
			pcnt = (pcnt+1)%pathlength;
		}

		// return straight line if fits
		if(curvepass){
			segment.add(new Double[7]);
			thissegment = segment.get(segment.size()-1);
			thissegment[0] = 1.0;
			thissegment[1] = path.get(seqstart)[0];
			thissegment[2] = path.get(seqstart)[1];
			thissegment[3] = path.get(seqend)[0];
			thissegment[4] = path.get(seqend)[1];
			thissegment[5] = 0.0;
			thissegment[6] = 0.0;
			return segment;
		}

		// 5.3. If the straight line fails (an error>ltreshold), find the point with the biggest error
		int fitpoint = errorpoint; curvepass = true; errorval = 0;

		// 5.4. Fit a quadratic spline through this point, measure errors on every point in the sequence
		// helpers and projecting to get control point
		double t=(fitpoint-seqstart)/tl, t1=(1.0-t)*(1.0-t), t2=2.0*(1.0-t)*t, t3=t*t;
		double cpx = (((t1*path.get(seqstart)[0]) + (t3*path.get(seqend)[0])) - path.get(fitpoint)[0])/-t2 ,
				cpy = (((t1*path.get(seqstart)[1]) + (t3*path.get(seqend)[1])) - path.get(fitpoint)[1])/-t2 ;

		// Check every point
		pcnt = seqstart+1;
		while(pcnt != seqend){

			t=(pcnt-seqstart)/tl; t1=(1.0-t)*(1.0-t); t2=2.0*(1.0-t)*t; t3=t*t;
			px = (t1 * path.get(seqstart)[0]) + (t2 * cpx) + (t3 * path.get(seqend)[0]);
			py = (t1 * path.get(seqstart)[1]) + (t2 * cpy) + (t3 * path.get(seqend)[1]);

			dist2 = ((path.get(pcnt)[0]-px)*(path.get(pcnt)[0]-px)) + ((path.get(pcnt)[1]-py)*(path.get(pcnt)[1]-py));

			if(dist2>qtreshold){curvepass=false;}
			if(dist2>errorval){ errorpoint=pcnt; errorval=dist2; }
			pcnt = (pcnt+1)%pathlength;
		}

		// return spline if fits
		if(curvepass){
			segment.add(new Double[7]);
			thissegment = segment.get(segment.size()-1);
			thissegment[0] = 2.0;
			thissegment[1] = path.get(seqstart)[0];
			thissegment[2] = path.get(seqstart)[1];
			thissegment[3] = cpx;
			thissegment[4] = cpy;
			thissegment[5] = path.get(seqend)[0];
			thissegment[6] = path.get(seqend)[1];
			return segment;
		}

		// 5.5. If the spline fails (an error>qtreshold), find the point with the biggest error,
		// set splitpoint = (fitting point + errorpoint)/2
		int splitpoint = (fitpoint + errorpoint)/2;

		// 5.6. Split sequence and recursively apply 5.2. - 5.6. to startpoint-splitpoint and splitpoint-endpoint sequences
		segment = fitseq(path,ltreshold,qtreshold,seqstart,splitpoint);
		segment.addAll(fitseq(path,ltreshold,qtreshold,splitpoint,seqend));
		return segment;

	}// End of fitseq()


	// 5. Batch tracing paths
	private static ArrayList<ArrayList<Double[]>> batchtracepaths (ArrayList<ArrayList<Double[]>> internodepaths, float ltres,float qtres){
		ArrayList<ArrayList<Double[]>> btracedpaths = new ArrayList<ArrayList<Double[]>>();
		for(int k=0; k<internodepaths.size(); k++){
			btracedpaths.add(tracepath(internodepaths.get(k),ltres,qtres) );
		}
		return btracedpaths;
	}


	// 5. Batch tracing layers
	private static ArrayList<ArrayList<ArrayList<Double[]>>> batchtracelayers (ArrayList<ArrayList<ArrayList<Double[]>>> binternodes, float ltres, float qtres){
		ArrayList<ArrayList<ArrayList<Double[]>>> btbis = new ArrayList<ArrayList<ArrayList<Double[]>>>();
		for(int k=0; k<binternodes.size(); k++){
			btbis.add( batchtracepaths( binternodes.get(k),ltres,qtres) );
		}
		return btbis;
	}




    }// End of ImageTracer class



    private static final int bgColor = 0x00000000;
    private static final byte a = ImageTracer.bytetrans((byte)(bgColor >>> 24));
    private static final byte r = ImageTracer.bytetrans((byte)(bgColor >>> 16));
    private static final byte g = ImageTracer.bytetrans((byte)(bgColor >>> 8));
    private static final byte b = ImageTracer.bytetrans((byte)(bgColor));
    private static final byte a1 = ImageTracer.bytetrans((byte)(0xFF000000 >>> 24));
    private static byte [][] palette = new byte[2][4];
    static {
        for (int i=0; i<2; i++){
            palette[i][0] = i==0 ? a : a1;
            palette[i][1] = r;
            palette[i][2] = g;
            palette[i][3] = b;
        }
    }


  //**************************************************************************
  //** getAlpha
  //**************************************************************************
    private int getAlpha(double percentile, ArrayList<Integer> alphas) {
        if (percentile==0) return alphas.get(0);
        int index = (int) Math.ceil(percentile / 100.0 * alphas.size());
        return alphas.get(index-1);
    }
}