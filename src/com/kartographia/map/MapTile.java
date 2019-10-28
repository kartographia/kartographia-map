package com.kartographia.map;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.WKTReader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.util.*;

//******************************************************************************
//**  MapTile
//******************************************************************************
/**
 *   Used to generate images that are rendered on a map. Can be used render
 *   points, lines, polygons, etc.
 *
 ******************************************************************************/

public class MapTile {

    private double ULx = 0;
    private double ULy = 0;
    private double resX = 1;
    private double resY = 1;

    protected javaxt.io.Image img;
    protected Graphics2D g2d;

    private String wkt;
    private double north;
    private double south;
    private double east;
    private double west;
    private Geometry geom;
    private int srid;


    private static DecimalFormat df = new DecimalFormat("#.##");
    static{ df.setMaximumFractionDigits(8); }



    private static PrecisionModel precisionModel = new PrecisionModel();
    private static GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 4326);

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MapTile(double minX, double minY, double maxX, double maxY,
                     int width, int height, int srid){



        this.srid = srid;
        if (srid==3857){


          //Convert coordinates to lat/lon
            north = getLat(maxY);
            south = getLat(minY);
            east = getLon(maxX);
            west = getLon(minX);


          //Validate Coordinates
            if (!valid(west, south, east, north)) throw new IllegalArgumentException();


          //Set wkt
            String NE = df.format(east) + " " + df.format(north);
            String SE = df.format(east) + " " + df.format(south);
            String SW = df.format(west) + " " + df.format(south);
            String NW = df.format(west) + " " + df.format(north);
            wkt = "POLYGON((" + NE + "," +  NW + "," + SW + "," + SE + "," + NE + "))";



            ULx = minX;
            ULy = maxY;


          //Compute pixelsPerMeter
            resX = width  / diff(minX,maxX);
            resY = height / diff(minY,maxY);

        }
        else if (srid==4326){


          //Validate Coordinates
            if (!valid(minX, minY, maxX, maxY)) throw new IllegalArgumentException();



          //Get wkt
            String NE = df.format(maxX) + " " + df.format(maxY);
            String SE = df.format(maxX) + " " + df.format(minY);
            String SW = df.format(minX) + " " + df.format(minY);
            String NW = df.format(minX) + " " + df.format(maxY);
            wkt = "POLYGON((" + NE + "," +  NW + "," + SW + "," + SE + "," + NE + "))";
            north = maxY;
            south = minY;
            east = maxX;
            west = minX;





          //Update min/max coordinates
            minX = x(minX);
            minY = y(minY);
            maxX = x(maxX);
            maxY = y(maxY);


          //Update Local Variables using updated values
            ULx = minX;
            ULy = maxY;


          //Compute pixelsPerDeg
            resX = ((double) width)  / (maxX-minX);
            resY = ((double) height) / (minY-maxY);//(maxY-minY);
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }


      //Create image
        img = new javaxt.io.Image(width, height);
        g2d = img.getBufferedImage().createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //applyQualityRenderingHints(g2d);
        g2d.setColor(Color.BLACK);
    }


  //**************************************************************************
  //** getSRID
  //**************************************************************************
    public int getSRID(){
        return srid;
    }


  //**************************************************************************
  //** getBounds
  //**************************************************************************
  /** Returns the tile boundary as a Well-Known Text (WKT) in lat/lon
   *  coordinates (EPSG:4326)
   */
    public String getBounds(){
        return wkt;
    }


  //**************************************************************************
  //** getGeometry
  //**************************************************************************
  /** Returns the tile boundary as a lat/lon geometry (EPSG:4326)
   */
    public Geometry getGeometry(){
        if (geom==null){
            try{
                geom = new WKTReader().read(wkt);
            }
            catch(Exception e){
                //should never happen
            }
        }
        return geom;
    }


  //**************************************************************************
  //** getWidth
  //**************************************************************************
    public int getWidth(){
        return img.getWidth();
    }


  //**************************************************************************
  //** getHeight
  //**************************************************************************
    public int getHeight(){
        return img.getHeight();
    }


    public double getNorth(){
        return north;
    }

    public double getSouth(){
        return south;
    }

    public double getEast(){
        return east;
    }

    public double getWest(){
        return west;
    }


  //**************************************************************************
  //** getImage
  //**************************************************************************
    public javaxt.io.Image getImage(){
        return img;
    }


  //**************************************************************************
  //** setBackgroundColor
  //**************************************************************************
    public void setBackgroundColor(int r, int g, int b){
        Color org = g2d.getColor();
        g2d.setColor(new Color(r,g,b));
        g2d.fillRect(0,0,img.getWidth(),img.getHeight());
        g2d.setColor(org);
    }

    
  //**************************************************************************
  //** addPixel
  //**************************************************************************
  /** Used to add a pixel to the image
   */
    public void addPixel(double lat, double lon, Color color){

      //Get center point
        double x;
        double y;
        if (srid == 3857){
            x = x(getX(lon));
            y = y(getY(lat));
        }
        else if (srid == 4326){
            x = x(lon);
            y = y(lat);
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }

        g2d.setColor(color);
        g2d.fillRect(cint(x), cint(y), 1, 1);
        g2d.setColor(Color.BLACK);
    }


  //**************************************************************************
  //** addPoint
  //**************************************************************************
  /** Used to add a point to the image
   */
    public void addPoint(double lat, double lon, Color color, double size){

      //Get center point
        double x;
        double y;
        if (srid == 3857){
            x = x(getX(lon));
            y = y(getY(lat));
        }
        else if (srid == 4326){
            x = x(lon);
            y = y(lat);
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }


      //Get upper left coordinate
        double r = size/2d;
        x = x-r;
        y = y-r;


      //Render circle
        g2d.setColor(color);
        g2d.fillOval(cint(x), cint(y), cint(size), cint(size));
        g2d.setColor(Color.BLACK);
    }


  //**************************************************************************
  //** addPolygon
  //**************************************************************************
  /** Used to add a polygon to the image
   */
    public void addPolygon(Polygon polygon, Color lineColor, Color fillColor){

        Coordinate[] coordinates = polygon.getCoordinates();
        int[] xPoints = new int[coordinates.length];
        int[] yPoints = new int[coordinates.length];

        for (int i=0; i<coordinates.length; i++){
            Coordinate coordinate = coordinates[i];
            //xPoints[i] = cint(x(coordinate.x));
            //yPoints[i] = cint(y(coordinate.y));

            double x;
            double y;
            if (srid == 3857){
                x = x(getX(coordinate.x));
                y = y(getY(coordinate.y));
            }
            else if (srid == 4326){
                x = x(coordinate.x);
                y = y(coordinate.y);
            }
            else{
                throw new IllegalArgumentException("Unsupported projection");
            }


            xPoints[i] = cint(x);
            yPoints[i] = cint(y);
        }


        if (fillColor!=null){
            g2d.setColor(fillColor);
            g2d.fillPolygon(xPoints, yPoints, coordinates.length);
        }
        if (lineColor!=null){
            g2d.setColor(lineColor);
            g2d.drawPolyline(xPoints, yPoints, coordinates.length);
        }
    }



  //**************************************************************************
  //** intersects
  //**************************************************************************
  /** Returns true if the tile intersects the given geometry.
   */
    public boolean intersects(String wkt) throws Exception {
        return new WKTReader().read(wkt).intersects(getGeometry());
    }


  //**************************************************************************
  //** validate
  //**************************************************************************
  /** Used to validate coordinates used to invoke this class
   */
    private boolean valid(double minX, double minY, double maxX, double maxY){
        if (minX > maxX || minY > maxY) return false;
        if (minX < -180 || maxX < -180 || maxX > 180 || minX > 180) return false;
        if (minY < -90 || maxY < -90 || maxY > 90 || minY > 90) return false;
        return true;
    }


  //**************************************************************************
  //** x
  //**************************************************************************
  /** Used to convert a geographic coordinate to a pixel coordinate
   *  @param pt Longitude value if the tile is in EPSG:4326. Otherwise,
   *  assumes value is in meters.
   */
    protected double x(double pt){
        if (srid == 3857){
            double x = pt;
            double d = diff(ULx,x);
            if (x<ULx) d = -d;
            return d * resX;
        }
        else if (srid == 4326){
            pt += 180;
            double x = (pt - ULx) * resX;
            //System.out.println("X = " + x);
            return x;
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }
    }


  //**************************************************************************
  //** y
  //**************************************************************************
  /** Used to convert a geographic coordinate to a pixel coordinate
   *  @param pt Latitude value if the tile is in EPSG:4326. Otherwise,
   *  assumes value is in meters.
   */
    protected double y(double pt){

        if (srid == 3857){
            double y = pt;
            double d = diff(ULy,y);
            if (y>ULy) d = -d;
            return  d * resY;
        }
        else if (srid == 4326){

            pt = -pt;
            if (pt<=0) pt = 90 + -pt;
            else pt = 90 - pt;

            pt = 180-pt;



            double y = (pt - ULy) * resY;

            if (cint(y)==0 || cint(y)==-0) y = 0;
            //else y = -y;


            return y;
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }
    }


    private static final double originShift = 2.0 * Math.PI * 6378137.0 / 2.0; //20037508.34

  //**************************************************************************
  //** getLat
  //**************************************************************************
  /** Converts y coordinate in EPSG:3857 to a latitude in EPSG:4326
   */
    public static double getLat(double y){
        //double lat = Math.log(Math.tan((90 + y) * Math.PI / 360.0)) / (Math.PI / 180.0);
        //return lat * originShift / 180.0;
        double lat = (y/originShift)*180.0;
        return 180.0 / Math.PI * (2 * Math.atan( Math.exp( lat * Math.PI / 180.0)) - Math.PI / 2.0);
    }



  //**************************************************************************
  //** getLon
  //**************************************************************************
  /** Converts x coordinate in EPSG:3857 to a longitude in EPSG:4326
   */
    public static double getLon(double x){
        //return x * originShift / 180.0;
        return (x/originShift)*180.0;
    }


  //**************************************************************************
  //** getX
  //**************************************************************************
  /** Converts longitude in EPSG:4326 to a x coordinate in EPSG:3857
   */
    public static double getX(double lon){
        //return (lon*180.0)/originShift;
        return lon * originShift / 180.0;
    }


  //**************************************************************************
  //** getY
  //**************************************************************************
  /** Converts latitude in EPSG:4326 to a y coordinate in EPSG:3857
   */
    public static double getY(double lat){
        //return Math.atan(Math.exp(lat * Math.PI / 20037508.34)) * 360 / Math.PI - 90;
        double y = Math.log( Math.tan((90 + lat) * Math.PI / 360.0 )) / (Math.PI / 180.0);
        return y * originShift / 180.0;
    }


  //**************************************************************************
  //** getIntersectingTiles
  //**************************************************************************
  /** Returns an array of x,y map tile coordinates that intersect a given
   *  geometry
   */
    public static ArrayList<int[]> getIntersectingTiles(Geometry geom, int z){
        Envelope box = geom.getEnvelopeInternal();

        int[] ul = MapTile.getTileCoordinate(box.getMaxY(), box.getMinX(), z);
        int[] lr = MapTile.getTileCoordinate(box.getMinY(), box.getMaxX(), z);

        int minX = ul[0];
        int minY = ul[1];
        int maxX = lr[0];
        int maxY = lr[1];

        ArrayList<int[]> tiles = new ArrayList<>();
        for (int y=minY; y<=maxY; y++){
            for (int x=minX; x<=maxX; x++){
                Geometry g = getTileGeometry(x,y,z);
                if (g.intersects(geom)){
                    tiles.add(new int[]{x,y});
                }
            }
        }

        return tiles;
    }


  //**************************************************************************
  //** getTileGeometry
  //**************************************************************************
  /** Returns a JTS Geometry for a given tile
   */
    public static Geometry getTileGeometry(int x, int y, int z){
        double north = tile2lat(y, z);
        double south = tile2lat(y + 1, z);
        double west = tile2lon(x, z);
        double east = tile2lon(x + 1, z);


        Coordinate ne = new Coordinate(east, north);
        Coordinate se = new Coordinate(east, south);
        Coordinate sw = new Coordinate(west, south);
        Coordinate nw = new Coordinate(west, north);


        Coordinate[] coordinates = new Coordinate[]{
            sw, se, ne, nw, sw
        };

        return geometryFactory.createPolygon(coordinates);
    }


    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    public static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }


  //**************************************************************************
  //** getTileCoordinate
  //**************************************************************************
  /** Returns the x,y coordinate of a map tile. Credit:
   *  https://github.com/chriswhong/map-tile-functions/blob/master/latLngToTileXY.js
   */
    public static int[] getTileCoordinate(double lat, double lng, int zoom){

        double latitude = clip(lat, -85.05112878, 85.05112878);
        double longitude = clip(lng, -180, 180);


        double x = (longitude + 180.0) / 360.0 * (1 << zoom);
        double y = (1.0 - Math.log(Math.tan(latitude * Math.PI / 180.0) + 1.0 / Math.cos(lat* Math.PI / 180)) / Math.PI) / 2.0 * (1 << zoom);

        int tilex  = trunc(x);
        int tiley  = trunc(y);

        return new int[]{tilex, tiley};
    }


    private static double clip(double n, double minValue, double maxValue){
        return Math.min(Math.max(n, minValue), maxValue);
    }


  //**************************************************************************
  //** trunc
  //**************************************************************************
  /** Emulates the Math.trunc() function in JavaScript. Returns the integer
   *  part of a floating-point number by removing the fractional digits. In
   *  other words, the function cuts off the dot and the digits to the right
   *  of it.
   */
    private static int trunc(double n){
        return new BigDecimal(n).toBigInteger().intValue();
    }


  //**************************************************************************
  //** cint
  //**************************************************************************
  /** Converts a double to an integer. Rounds the double to the nearest int.
   */
    private int cint(Double d){
        return (int)Math.round(d);
    }


  //**************************************************************************
  //** diff
  //**************************************************************************
  /** Returns the difference between to numbers
   */
    public static double diff(double a, double b){
        double x = a-b;
        if (x<0) x = -x;
        return x;
    }
}