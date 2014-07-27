package org.jenkinsci.plugins.unittestdb.db;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.Extension;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import static java.util.Objects.requireNonNull;

/**
 *
 * @author David van Laatum
 */
@Entity
@Extension
@Table ( name = "failure_users" )
@XmlRootElement
@NamedQueries ( {
  @NamedQuery ( name = "FailureUser.findAll", query
                = "SELECT f FROM FailureUser f" ),
  @NamedQuery ( name = "FailureUser.findByFailureUserId", query
                = "SELECT f FROM FailureUser f WHERE f.failureUserId = :failureUserId" ),
  @NamedQuery ( name = "FailureUser.findByState", query
                = "SELECT f FROM FailureUser f WHERE f.state = :state" ) } )
public class FailureUser extends DBObject implements Serializable {

  private static final Logger LOG
          = Logger.getLogger ( FailureUser.class.getName () );

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue ( strategy = GenerationType.IDENTITY )
  @Basic ( optional = false )
  @Column ( name = "failure_user_id" )
  private Integer failureUserId;
  @Basic ( optional = false )
  @Column ( name = "state" )
  @Enumerated ( EnumType.STRING )
  private FailureUserState state;
  @JoinColumn ( name = "user_id", referencedColumnName = "user_id" )
  @ManyToOne ( optional = false )
  private User user;
  @JoinColumn ( name = "failure_id", referencedColumnName = "failure_id" )
  @ManyToOne ( optional = false )
  private Failure failure;

  public FailureUser () {
  }

  public FailureUser ( Integer failureUserId ) {
    this.failureUserId = failureUserId;
  }

  public FailureUser ( Integer failureUserId, FailureUserState state ) {
    this.failureUserId = failureUserId;
    this.state = state;
  }

  public Integer getFailureUserId () {
    return failureUserId;
  }

  public void setFailureUserId ( Integer failureUserId ) {
    this.failureUserId = failureUserId;
  }

  public FailureUserState getState () {
    return state;
  }

  public void setState ( FailureUserState state ) {
    this.state = state;
  }

  public User getUser () {
    return user;
  }

  public void setUser ( User user ) {
    this.user = user;
  }

  public Failure getFailure () {
    return failure;
  }

  public void setFailure ( Failure failure ) {
    this.failure = failure;
  }

  @Override
  public int hashCode () {
    int hash = 0;
    hash += ( failureUserId != null ? failureUserId.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof FailureUser ) ) {
      return false;
    }
    FailureUser other = (FailureUser) object;
    if ( ( this.failureUserId == null && other.failureUserId != null )
                 || ( this.failureUserId != null && !this.failureUserId.equals (
                     other.failureUserId ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "com.vanlaatum.unittestsdb.DB.FailureUser[ failureUserId="
                   + failureUserId + " ]";
  }

  public static FailureUser findByID ( Integer id, EntityManager em ) {
    requireNonNull ( em, "No EntityManager passed in" );
    requireNonNull ( id, "No id passed in" );
    Query q = em.createNamedQuery ( "FailureUser.findByFailureUserId" );
    q.setParameter ( "failureUserId", id );
    q.setMaxResults ( 1 );
    FailureUser rt = null;
    try {
      rt = (FailureUser) q.getSingleResult ();
    } catch ( NoResultException ex ) {
      LOG.log ( Level.FINE, "FailureUser with with id {0} not found", id );
    }
    return rt;
  }

}
