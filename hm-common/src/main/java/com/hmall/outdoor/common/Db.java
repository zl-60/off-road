package com.hmall.outdoor.common;

import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class Db {

  public final JdbcTemplate jdbc;
  private final TransactionTemplate tx;

  public Db(JdbcTemplate jdbc, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
  }

  public <T> T tx(Supplier<T> work) {
    return tx.execute(s -> work.get());
  }

  public int update(String sql, Object... args) {
    return jdbc.update(sql, args);
  }

  public List<Map<String, Object>> rows(String sql, Object... args) {
    return jdbc.query(
      sql,
      (rs, n) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
          String key = rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT);
          StringBuilder b = new StringBuilder();
          boolean cap = false;
          for (char c : key.toCharArray()) {
            if (c == '_') {
              cap = true;
            } else {
              b.append(cap ? Character.toUpperCase(c) : c);
              cap = false;
            }
          }
          Object v = rs.getObject(i);
          if (v instanceof java.sql.Timestamp) v = ((java.sql.Timestamp) v)
            .toLocalDateTime()
            .toString();
          row.put(b.toString(), v);
        }
        return row;
      },
      args
    );
  }

  public Map<String, Object> one(String sql, Object... args) {
    List<Map<String, Object>> r = rows(sql, args);
    if (r.isEmpty()) throw new Problem(404, "记录不存在");
    return r.get(0);
  }

  public boolean exists(String sql, Object... args) {
    return !rows(sql, args).isEmpty();
  }
}
