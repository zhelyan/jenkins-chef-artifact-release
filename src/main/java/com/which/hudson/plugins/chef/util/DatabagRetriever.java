package com.which.hudson.plugins.chef.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.jclouds.chef.ChefApi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class DatabagRetriever {

    /**
     * Retrieves databags and associated data bag items.
     * It does that by spawning a thread per data bag to do the work.
     * @param api  (@link ChefApi) to use
     * @return
     */
    public static Multimap<String,String> getDatabags(ChefApi api) {
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(processors);

        List<String> databags = new LinkedList<String>(api.listDatabags());

//        ThreadPoolExecutor exec = new ThreadPoolExecutor(processors, // core size
//                databags.size(), // max size
//                10*60, // idle timeout
//                TimeUnit.SECONDS,
//                new ArrayBlockingQueue<Runnable>(databags.size()));
//



        List<Callable<Multimap<String,String>>> callables =  new ArrayList<Callable<Multimap<String,String>>>();
        for (String databag : databags) {
            callables.add(new DBIWorker(api, databag));
        }
        Multimap<String,String> data = TreeMultimap.create();
        try {
            List<Future<Multimap<String,String>>> results = exec.invokeAll(callables);
            for (Future<Multimap<String,String>> result : results) {
                data.putAll(result.get());
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        } finally {
            exec.shutdownNow();
        }
        return data;
    }

    private static class DBIWorker implements Callable<Multimap<String,String> > {
        private final String databag;
        private final ChefApi api;

        DBIWorker(ChefApi api, String databag) {
            this.databag = databag;
            this.api =  api;
        }

        public Multimap<String,String> call() {
            Multimap data = TreeMultimap.create();
            data.putAll(databag,api.listDatabagItems(databag));
            return data;
        }
    }

}