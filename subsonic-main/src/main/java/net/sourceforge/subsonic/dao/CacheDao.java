/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.dao;

import com.db4o.Db4oEmbedded;
import com.db4o.EmbeddedObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.defragment.Defragment;
import com.db4o.internal.ObjectContainerBase;
import com.db4o.internal.query.Db4oQueryExecutionListener;
import com.db4o.internal.query.NQOptimizationInfo;
import com.db4o.query.Predicate;
import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.CacheElement;

import java.io.File;
import java.io.IOException;

/**
 * Provides database services for caching.
 *
 * @author Sindre Mehus
 */
public class CacheDao {

    private static final Logger LOG = Logger.getLogger(CacheDao.class);
    private final EmbeddedObjectContainer db;

    public CacheDao() {
        File dbFile = new File("/tmp/db40.db");
//        if (dbFile.exists()) {
//            try {
//                Defragment.defrag(dbFile.getPath());
//            }catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
        config.common().objectClass(CacheElement.class).objectField("id").indexed(true);
        config.common().objectClass(CacheElement.class).cascadeOnUpdate(true);
        config.common().objectClass(CacheElement.class).cascadeOnDelete(true);
        config.common().objectClass(CacheElement.class).cascadeOnActivate(true);

//        config.common().messageLevel(3);
        System.out.println("Optimized: " + config.common().optimizeNativeQueries());
        db = Db4oEmbedded.openFile(config, dbFile.getPath());

//        ((ObjectContainerBase) db).getNativeQueryHandler().addListener(new Db4oQueryExecutionListener() {
//            public void notifyQueryExecuted(NQOptimizationInfo info) {
//                System.out.println(info);
//            }
//        });
    }

    /**
     * Creates a new cache element.
     *
     * @param element The cache element to create (or update).
     */
    public void createCacheElement(CacheElement element) {
        deleteCacheElement(element);
        db.store(element);

        if (changeCount++ == BATCH_SIZE) {
            db.commit();
            changeCount = 0;
        }
    }
    int changeCount = 0;
    final int BATCH_SIZE = 100;

    public CacheElement getCacheElement(int type, String key) {
        long t0 = System.nanoTime();
        ObjectSet<CacheElement> result = db.query(new CacheElementPredicate(type, key));
        if (result.size() > 1) {
            LOG.error("Programming error. Got " + result.size() + " cache elements of type " + type + " and key " + key);
        }
        long t1 = System.nanoTime();
        if (!result.isEmpty()) {
            System.out.println(result.get(0).getValue().getClass().getSimpleName() + ": " + ((t1 - t0) / 1000) + " microsec");
        }

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Deletes the cache element with the given type and key.
     */
    private void deleteCacheElement(CacheElement element) {
        // Retrieve it from the database first.
        element = getCacheElement(element.getType(), element.getKey());
        if (element != null) {
            db.delete(element);
        }
    }

    private static class CacheElementPredicate extends Predicate<CacheElement> {
        private static final long serialVersionUID = 54911003002373726L;

        private final int id;

        public CacheElementPredicate(int type, String key) {
            id = CacheElement.createId(type, key);
        }

        @Override
        public boolean match(CacheElement candidate) {
            return candidate.getId() == id;
        }
    }
}