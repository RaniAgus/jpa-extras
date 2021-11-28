package com.github.flbulgarelli.jpa.extras.perthread;

import com.github.flbulgarelli.jpa.extras.WithEntityManager;

import javax.persistence.EntityManager;

public interface WithPerThreadEntityManager extends WithEntityManager {

  default EntityManager entityManager() {
    return perThreadEntityManagerAccess().get();
  }

  PerThreadEntityManagerAccess perThreadEntityManagerAccess();
}
