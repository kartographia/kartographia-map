package com.kartographia.map;
import javaxt.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//******************************************************************************
//**  TileCache
//******************************************************************************
/**
 *   Used to add and retrieve image tiles from a cache directory. Ensures
 *   that no two threads create the same image file
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

        synchronized(requests){
            requests.put(key, System.currentTimeMillis());
        }

        Tile tile;
        synchronized(tiles){
            tile = tiles.get(key);
            if (tile==null){
                tile = new Tile(key, tileCache);
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
  //** ImageCreator
  //**************************************************************************
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

        public Tile(String key, Directory tileCache){
            this.key = key;
            this.tileCache = tileCache;
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

            File tmp = new File(dir+"_temp", f.getName());
            img.saveAs(tmp.toFile());
            tmp.rename(tmp.getName()+".tmp");
            tmp.moveTo(dir);
            tmp.rename(f.getName());


            file.add(f);
            file.notifyAll();

            synchronized(status){
                status.clear();
            }
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
}