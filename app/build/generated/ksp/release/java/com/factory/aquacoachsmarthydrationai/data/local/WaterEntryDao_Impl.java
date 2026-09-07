package com.factory.aquacoachsmarthydrationai.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WaterEntryDao_Impl implements WaterEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WaterEntry> __insertionAdapterOfWaterEntry;

  private final EntityDeletionOrUpdateAdapter<WaterEntry> __deletionAdapterOfWaterEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public WaterEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWaterEntry = new EntityInsertionAdapter<WaterEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `water_entries` (`id`,`amountMl`,`timestampEpochMillis`,`dayEpochDay`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WaterEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAmountMl());
        statement.bindLong(3, entity.getTimestampEpochMillis());
        statement.bindLong(4, entity.getDayEpochDay());
      }
    };
    this.__deletionAdapterOfWaterEntry = new EntityDeletionOrUpdateAdapter<WaterEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `water_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WaterEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM water_entries WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM water_entries";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final WaterEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWaterEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final WaterEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWaterEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long entryId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, entryId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WaterEntry>> observeEntriesForDay(final long dayEpochDay) {
    final String _sql = "SELECT * FROM water_entries WHERE dayEpochDay = ? ORDER BY timestampEpochMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayEpochDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"water_entries"}, new Callable<List<WaterEntry>>() {
      @Override
      @NonNull
      public List<WaterEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMl = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMl");
          final int _cursorIndexOfTimestampEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampEpochMillis");
          final int _cursorIndexOfDayEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dayEpochDay");
          final List<WaterEntry> _result = new ArrayList<WaterEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WaterEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpAmountMl;
            _tmpAmountMl = _cursor.getInt(_cursorIndexOfAmountMl);
            final long _tmpTimestampEpochMillis;
            _tmpTimestampEpochMillis = _cursor.getLong(_cursorIndexOfTimestampEpochMillis);
            final long _tmpDayEpochDay;
            _tmpDayEpochDay = _cursor.getLong(_cursorIndexOfDayEpochDay);
            _item = new WaterEntry(_tmpId,_tmpAmountMl,_tmpTimestampEpochMillis,_tmpDayEpochDay);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> observeTotalForDay(final long dayEpochDay) {
    final String _sql = "SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE dayEpochDay = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayEpochDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"water_entries"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<DailyTotal>> observeDailyTotals(final long startDayEpochDay,
      final long endDayEpochDay) {
    final String _sql = "SELECT dayEpochDay, SUM(amountMl) as totalMl FROM water_entries WHERE dayEpochDay BETWEEN ? AND ? GROUP BY dayEpochDay ORDER BY dayEpochDay ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDayEpochDay);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDayEpochDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"water_entries"}, new Callable<List<DailyTotal>>() {
      @Override
      @NonNull
      public List<DailyTotal> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDayEpochDay = 0;
          final int _cursorIndexOfTotalMl = 1;
          final List<DailyTotal> _result = new ArrayList<DailyTotal>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyTotal _item;
            final long _tmpDayEpochDay;
            _tmpDayEpochDay = _cursor.getLong(_cursorIndexOfDayEpochDay);
            final int _tmpTotalMl;
            _tmpTotalMl = _cursor.getInt(_cursorIndexOfTotalMl);
            _item = new DailyTotal(_tmpDayEpochDay,_tmpTotalMl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WaterEntry>> observeAllEntries() {
    final String _sql = "SELECT * FROM water_entries ORDER BY timestampEpochMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"water_entries"}, new Callable<List<WaterEntry>>() {
      @Override
      @NonNull
      public List<WaterEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMl = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMl");
          final int _cursorIndexOfTimestampEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampEpochMillis");
          final int _cursorIndexOfDayEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dayEpochDay");
          final List<WaterEntry> _result = new ArrayList<WaterEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WaterEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpAmountMl;
            _tmpAmountMl = _cursor.getInt(_cursorIndexOfAmountMl);
            final long _tmpTimestampEpochMillis;
            _tmpTimestampEpochMillis = _cursor.getLong(_cursorIndexOfTimestampEpochMillis);
            final long _tmpDayEpochDay;
            _tmpDayEpochDay = _cursor.getLong(_cursorIndexOfDayEpochDay);
            _item = new WaterEntry(_tmpId,_tmpAmountMl,_tmpTimestampEpochMillis,_tmpDayEpochDay);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
