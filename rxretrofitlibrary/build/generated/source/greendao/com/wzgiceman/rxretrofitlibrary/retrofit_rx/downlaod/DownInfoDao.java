package com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod.bean.DaoSession;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "DOWN_INFO".
*/
public class DownInfoDao extends AbstractDao<DownInfo, Long> {

    public static final String TABLENAME = "DOWN_INFO";

    /**
     * Properties of entity DownInfo.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, long.class, "id", true, "_id");
        public final static Property FileName = new Property(1, String.class, "fileName", false, "FILE_NAME");
        public final static Property SavePath = new Property(2, String.class, "savePath", false, "SAVE_PATH");
        public final static Property CountLength = new Property(3, long.class, "countLength", false, "COUNT_LENGTH");
        public final static Property ReadLength = new Property(4, long.class, "readLength", false, "READ_LENGTH");
        public final static Property ConnectonTime = new Property(5, int.class, "connectonTime", false, "CONNECTON_TIME");
        public final static Property StateInte = new Property(6, int.class, "stateInte", false, "STATE_INTE");
        public final static Property Url = new Property(7, String.class, "url", false, "URL");
    }


    public DownInfoDao(DaoConfig config) {
        super(config);
    }
    
    public DownInfoDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"DOWN_INFO\" (" + //
                "\"_id\" INTEGER PRIMARY KEY NOT NULL ," + // 0: id
                "\"FILE_NAME\" TEXT," + // 1: fileName
                "\"SAVE_PATH\" TEXT," + // 2: savePath
                "\"COUNT_LENGTH\" INTEGER NOT NULL ," + // 3: countLength
                "\"READ_LENGTH\" INTEGER NOT NULL ," + // 4: readLength
                "\"CONNECTON_TIME\" INTEGER NOT NULL ," + // 5: connectonTime
                "\"STATE_INTE\" INTEGER NOT NULL ," + // 6: stateInte
                "\"URL\" TEXT);"); // 7: url
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"DOWN_INFO\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, DownInfo entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
 
        String fileName = entity.getFileName();
        if (fileName != null) {
            stmt.bindString(2, fileName);
        }
 
        String savePath = entity.getSavePath();
        if (savePath != null) {
            stmt.bindString(3, savePath);
        }
        stmt.bindLong(4, entity.getCountLength());
        stmt.bindLong(5, entity.getReadLength());
        stmt.bindLong(6, entity.getConnectonTime());
        stmt.bindLong(7, entity.getStateInte());
 
        String url = entity.getUrl();
        if (url != null) {
            stmt.bindString(8, url);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, DownInfo entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
 
        String fileName = entity.getFileName();
        if (fileName != null) {
            stmt.bindString(2, fileName);
        }
 
        String savePath = entity.getSavePath();
        if (savePath != null) {
            stmt.bindString(3, savePath);
        }
        stmt.bindLong(4, entity.getCountLength());
        stmt.bindLong(5, entity.getReadLength());
        stmt.bindLong(6, entity.getConnectonTime());
        stmt.bindLong(7, entity.getStateInte());
 
        String url = entity.getUrl();
        if (url != null) {
            stmt.bindString(8, url);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.getLong(offset + 0);
    }    

    @Override
    public DownInfo readEntity(Cursor cursor, int offset) {
        DownInfo entity = new DownInfo( //
            cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // fileName
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // savePath
            cursor.getLong(offset + 3), // countLength
            cursor.getLong(offset + 4), // readLength
            cursor.getInt(offset + 5), // connectonTime
            cursor.getInt(offset + 6), // stateInte
            cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7) // url
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, DownInfo entity, int offset) {
        entity.setId(cursor.getLong(offset + 0));
        entity.setFileName(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setSavePath(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setCountLength(cursor.getLong(offset + 3));
        entity.setReadLength(cursor.getLong(offset + 4));
        entity.setConnectonTime(cursor.getInt(offset + 5));
        entity.setStateInte(cursor.getInt(offset + 6));
        entity.setUrl(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(DownInfo entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(DownInfo entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(DownInfo entity) {
        throw new UnsupportedOperationException("Unsupported for entities with a non-null key");
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
