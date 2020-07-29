package com.kartographia.map;
import javaxt.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//******************************************************************************
//**  TileCache
//******************************************************************************
/**
 *   Used to add and retrieve image tiles from a cache directory. Ensures
 *   that no two threads create the same image file.
 *
 ******************************************************************************/

public class TileCache {

    private Directory tileCache;
    private ConcurrentHashMap<String, Tile> tiles = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> requests = new ConcurrentHashMap<>();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public TileCache(Directory cacheDir){
        if (!cacheDir.exists()) cacheDir.create();
        if (!cacheDir.exists()) throw new IllegalArgumentException("Invalid cacheDir");
        tileCache = cacheDir;



      //Set up timer task to periodically clean-up the "tiles" hashmap so it
      //doesn't grow too big
        int maxSize = 1000;
        long maxAge = 2*60*1000;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            public void run(){
                if (requests.size()<maxSize) return;
                long currTime = System.currentTimeMillis();
                int x = 0;
                synchronized(requests){
                    Iterator<String> it = requests.keySet().iterator();
                    while (it.hasNext()){
                        String key = it.next();
                        long lastRequest = requests.get(key);
                        if (currTime-lastRequest>maxAge){
                            synchronized(tiles){
                                tiles.remove(key);
                            }
                            it.remove();
                            x++;
                        }
                    }
                }
                System.out.println("Removed " + x + " tiles from memory");
            }
        }, maxAge, maxAge);
    }


  //**************************************************************************
  //** getOrCreateTile
  //**************************************************************************
    public Tile getOrCreateTile(String key, ImageCreator imageCreator){
        return getOrCreateTile(key, imageCreator, true);
    }


  //**************************************************************************
  //** getOrCreateTile
  //**************************************************************************
  /** Returns a tile from the tile cache. If a tile does not exist, the
   *  ImageCreator is used to create a new tile. By default, if the
   *  ImageCreator produces an empty or null image, a 0 byte file is created
   *  on disk. This is ideal for web mapping applications and provides a hint
   *  that the tile request was received but the corresponding tile has no
   *  data.
   *  @param key Unique key that is used to find a tile in the cache. The
   *  key is used to construct a file path. Typical keys follow the pattern
   *  "{layer}/{z}/{x}/{y}". The getRelativePath() method can be used to
   *  construct more elaborate paths using tile x,y,z coordinates.
   *  @param imageCreator ImageCreator implementation that is used to create
   *  image tiles.
   *  @param saveEmptyTiles If true, creates empty (0 byte) files for empty
   *  images (not recommended).
   */
    public Tile getOrCreateTile(String key, ImageCreator imageCreator, boolean saveEmptyTiles){

        synchronized(requests){
            requests.put(key, System.currentTimeMillis());
        }

        Tile tile;
        synchronized(tiles){
            tile = tiles.get(key);
            if (tile==null){
                tile = new Tile(key, tileCache, saveEmptyTiles);
                tiles.put(key, tile);
            }
        }

        synchronized(tile.file){
            if (tile.file.isEmpty()){

                boolean createTile = false;
                synchronized(tile.status){
                    if (tile.status.isEmpty()){
                        tile.status.add(0);
                        tile.status.notify();
                        createTile = true;
                    }
                    else{
                        while (!tile.status.isEmpty()){
                            try{
                                tile.status.wait();
                            }
                            catch(Exception e){
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }


                if (createTile){
                    Image img = imageCreator.create();
                    tile.update(img);
                }
            }
        }
        return tile;
    }


  //**************************************************************************
  //** removeTile
  //**************************************************************************
  /** Used to delete a tile from the cache
   */
    public void removeTile(String key){
        synchronized(tiles){
            Tile tile = tiles.get(key);
            if (tile!=null){

                synchronized(tile.file){
                    while (tile.file.isEmpty()){
                        try{
                            tile.file.wait();
                        }
                        catch(Exception e){
                            throw new RuntimeException(e);
                        }
                    }
                }

                tile.getFile().delete();
                tiles.remove(key);
            }
            else{
                tile = new Tile(key, tileCache, false);
                tile.getFile().delete();
            }
        }
    }


  //**************************************************************************
  //** ImageCreator
  //**************************************************************************
  /** Instances of this class are used to create image tiles
   */
    public static interface ImageCreator {
        public Image create();
    }


  //**************************************************************************
  //** Tile
  //**************************************************************************
    public static class Tile {

        private String key;
        private List<File> file = new LinkedList<>();
        private List status = new LinkedList<>();
        private Directory tileCache;
        private boolean saveEmptyTiles;

        public Tile(String key, Directory tileCache, boolean saveEmptyTiles){
            this.key = key;
            this.tileCache = tileCache;
            this.saveEmptyTiles = saveEmptyTiles;
            File f = getFile();
            if (f.exists()){
                synchronized(file){
                    file.add(f);
                    file.notify();
                }
            }
        }

        private void update(javaxt.io.Image img){
            File f = getFile();
            if (f.exists()) f.delete();
            Directory dir = f.getDirectory();
            dir.create();

            if (isEmpty(img)) img = null;


            File tmp = new File(dir+"_temp", f.getName());
            if (img==null){
                if (saveEmptyTiles){
                    tmp.create();
                }
            }
            else{
                img.saveAs(tmp.toFile());
            }

            if (tmp.exists()){
                tmp.rename(tmp.getName()+".tmp");
                tmp.moveTo(dir);
                tmp.rename(f.getName());
            }

            file.add(f);
            file.notifyAll();

            synchronized(status){
                status.clear();
            }
        }

        private boolean isEmpty(Image img){
            if (img!=null){
                for (int i=0; i<img.getWidth(); i++){
                    for (int j=0; j<img.getHeight(); j++){
                        java.awt.Color color = img.getColor(i, j);
                        int r = color.getRed();
                        int g = color.getGreen();
                        int b = color.getBlue();
                        int a = color.getAlpha();
                        if (a>0) return false;
                    }
                }
            }
            return true;
        }

        public File getFile(){
            if (file.isEmpty()){
                return new File(tileCache + key + ".png");
            }
            else{
                return file.get(0);
            }
        }
    }


  //**************************************************************************
  //** getRelativePath
  //**************************************************************************
  /** Returns a string that can be used to construct a directory to store
   *  image tiles. This method is designed to reduce the total number of
   *  folders that would be required to store image tiles. You can use this
   *  path as a key in the getOrCreateTile() method.
   */
    public static String getRelativePath(int x, int y, int z){
        String path = "/" + z + "/";
        if (z>=8){
            int[] t = MapTile.getTileCoordinate(x, y, z, 8);
            path += t[0] + "/" + t[1] + "/";
        }
        path += x + "/" + y;
        return path;
    }
}