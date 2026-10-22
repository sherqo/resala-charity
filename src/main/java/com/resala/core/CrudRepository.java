package com.resala.core;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T> {

    long insert(T entity);

    boolean update(T entity);

    boolean deleteById(long id);

    Optional<T> findById(long id);

    List<T> findAll();
}
