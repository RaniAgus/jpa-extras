package com.github.flbulgarelli.jpa.extras;


import javax.persistence.EntityManager;

public interface WithEntityManager {

  EntityManager entityManager();
}
