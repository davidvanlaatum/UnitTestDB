package org.jenkinsci.plugins.unittestdb.db;

import com.google.common.base.Strings;
import hudson.Extension;
import java.io.Serializable;
import javax.persistence.*;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author David van Laatum
 */
@Entity
@Extension
@Table ( name = "createlocks" )
@NamedQueries ( {
  @NamedQuery ( name = "Createlocks.findAll", query
                = "SELECT c FROM Createlocks c" ),
  @NamedQuery ( name = "Createlocks.findByName", query
                = "SELECT c FROM Createlocks c WHERE c.name = :name" ) } )
public class Createlocks extends DBObject implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic ( optional = false )
  @Column ( name = "name" )
  private String name;

  public Createlocks () {
  }

  public Createlocks ( String name ) {
    this.name = name;
  }

  public String getName () {
    return name;
  }

  public void setName ( String name ) {
    this.name = name;
  }

  @Override
  public int hashCode () {
    int hash = 0;
    hash += ( name != null ? name.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof Createlocks ) ) {
      return false;
    }
    Createlocks other = (Createlocks) object;
    if ( ( this.name == null && other.name != null ) || ( this.name != null
                                                          && !this.name
                                                         .equals ( other.name ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "org.jenkinsci.plugins.unittestdb.DB.Createlocks[ name=" + name
                   + " ]";
  }

  public static Createlocks getLockObject ( EntityManager em, String name ) {
    requireNonNull ( em, "No EntityManager passed in" );
    if ( Strings.isNullOrEmpty ( name ) ) {
      throw new IllegalArgumentException ( "Name is null or empty" );
    }
    Query q = em.createNamedQuery ( "Createlocks.findByName" );
    q.setParameter ( "name", name );
    q.setMaxResults ( 1 );
    Createlocks rt = null;
    try {
      rt = (Createlocks) q.getSingleResult ();
    } catch ( NoResultException ex ) {
      throw new IllegalStateException ( "Failed to find lock for " + name, ex );
    }
    return rt;
  }

}
