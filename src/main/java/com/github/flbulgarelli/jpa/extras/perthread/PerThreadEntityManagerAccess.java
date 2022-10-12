package com.github.flbulgarelli.jpa.extras.perthread;

import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author gprieto
 * @author flbulgarelli
 * @author raniagus
 */
public class PerThreadEntityManagerAccess {

  private volatile EntityManagerFactory emf;

  private final String persistenceUnitName;

  private final ThreadLocal<EntityManager> threadLocal;

  private final PerThreadEntityManagerProperties properties;

  public PerThreadEntityManagerAccess(String persistenceUnitName) {
    this.persistenceUnitName = persistenceUnitName;
    this.threadLocal = new ThreadLocal<>();
    this.properties = new PerThreadEntityManagerProperties();
  }

  private void ensureNotInitialized() {
    synchronized (this) {
      if (emf != null) {
        throw new IllegalStateException("Can not set properties after initialization");
      }
    }
  }

  /**
   * Exposes the properties that will be used to create the entity manager factory.
   *
   * @param propertiesConsumer a consumer that will be called with the properties object
   * @throws IllegalStateException if the entity manager factory has already been created
   */
  public void configure(Consumer<PerThreadEntityManagerProperties> propertiesConsumer) {
    ensureNotInitialized();
    propertiesConsumer.accept(properties);
  }

  private EntityManagerFactory getEmf() {
    if (emf == null) {
      synchronized (this) {
        if (emf == null) {
          emf = Persistence.createEntityManagerFactory(
              persistenceUnitName, properties.get());
        }
      }
    }
    return emf;
  }

  /**
   * Shutdowns this access, preventing new entity managers to be produced
   */
  public void shutdown() {
    getEmf().close();
  }

  /**
   * @return whether {@link #shutdown()} has been called yet
   */
  public boolean isActive() {
    return getEmf().isOpen();
  }

  private void ensureActive() {
    if (!getEmf().isOpen()) {
      throw new IllegalStateException("Can not get an entity manager before initialize or after shutdown");
    }
  }

  /**
   * Returns the entity manager attached to the current thread, if any and is open.
   * Otherwise, it creates a new one and attaches it.
   *
   * @throws IllegalStateException if {@link #shutdown()} has been already called
   */
  public EntityManager get() {
    ensureActive();
    EntityManager manager = threadLocal.get();
    if (manager == null || !manager.isOpen()) {
      manager = emf.createEntityManager();
      threadLocal.set(manager);
    }
    return manager;
  }

  /**
   * Tells whether an entity manager is attached
   * to the current thread
   *
   * @throws IllegalStateException if {@link #shutdown()} has been already called
   */
  public boolean isAttached() {
    ensureActive();
    return threadLocal.get() != null;
  }

  /**
   * Closes and dereferences the currently attached entity
   * manager, if any
   *
   * @throws IllegalStateException if {@link #shutdown()} has been already called
   */
  public void dispose() {
    ensureActive();
    EntityManager em = threadLocal.get();
    if (em != null) {
      em.close();
      threadLocal.set(null);
    }
  }
}
